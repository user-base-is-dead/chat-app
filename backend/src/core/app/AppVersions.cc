#include "core/app/AppVersions.h"

#include <algorithm>
#include <mutex>
#include <unordered_map>
#include <vector>

#include <drogon/drogon.h>
#include <json/reader.h>

#include "core/app/ApkCheck.h"
#include "core/database/Database.h"

namespace core::app
{
namespace
{
constexpr double kRefreshSeconds = 10.0;

struct Rule
{
    int minVersion = 0;
    std::vector<int> blocked;
};

std::mutex mutex;
std::unordered_map<std::string, Rule> rules;
std::unordered_map<std::string, std::string> stamps;
std::function<void(const std::string &)> listener;
bool loaded = false;

int parseVersion(const std::string &value)
{
    if (value.empty() || value.size() > 9 || !std::all_of(value.begin(), value.end(), ::isdigit))
        return -1;
    return std::stoi(value);
}

std::vector<int> parseList(const std::string &value)
{
    std::vector<int> list;
    std::string current;
    for (const char c : value)
    {
        if (std::isdigit(static_cast<unsigned char>(c)))
            current += c;
        else if (!current.empty())
        {
            list.push_back(std::stoi(current));
            current.clear();
        }
    }
    if (!current.empty())
        list.push_back(std::stoi(current));
    return list;
}

int latestGoodRelease(const std::string &rows)
{
    size_t start = 0;
    while (start < rows.size())
    {
        auto end = rows.find('\n', start);
        if (end == std::string::npos)
            end = rows.size();
        const auto line = rows.substr(start, end - start);
        start = end + 1;
        const auto space = line.find(' ');
        if (space == std::string::npos)
            continue;
        const auto code = parseVersion(line.substr(0, space));
        if (code > 0 && releaseFileMatches(line.substr(space + 1), code))
            return code;
    }
    return 0;
}

void refresh()
{
    database::client()->execSqlAsync(
        "SELECT p.platform, p.min_version, p.blocked_versions::text AS blocked, p.flags::text AS flags, "
        "COALESCE((SELECT string_agg(r.version_code || ' ' || r.download_url, E'\n' ORDER BY r.version_code DESC) "
        "FROM (SELECT version_code, download_url FROM app_releases WHERE platform = p.platform "
        "ORDER BY version_code DESC LIMIT 20) r), '') AS releases "
        "FROM app_platforms p",
        [](const drogon::orm::Result &result) {
            std::unordered_map<std::string, Rule> next;
            std::unordered_map<std::string, std::string> nextStamps;
            for (const auto &row : result)
            {
                const auto platform = row["platform"].as<std::string>();
                const auto blocked = row["blocked"].as<std::string>();
                const auto latest = latestGoodRelease(row["releases"].as<std::string>());
                auto minVersion = row["min_version"].as<int>();
                if (platform == "android" && minVersion > latest)
                {
                    if (minVersion > 0)
                        LOG_WARN << "min_version " << minVersion << " is above the newest good android release " << latest;
                    minVersion = latest;
                }
                next[platform] = Rule{minVersion, parseList(blocked)};
                nextStamps[platform] = std::to_string(minVersion) + "|" + blocked + "|" +
                                       row["flags"].as<std::string>() + "|" + std::to_string(latest);
            }
            std::vector<std::string> changed;
            std::function<void(const std::string &)> notify;
            {
                std::lock_guard lock(mutex);
                if (loaded)
                    for (const auto &[platform, stamp] : nextStamps)
                    {
                        const auto it = stamps.find(platform);
                        if (it == stamps.end() || it->second != stamp)
                            changed.push_back(platform);
                    }
                rules = std::move(next);
                stamps = std::move(nextStamps);
                loaded = true;
                notify = listener;
            }
            if (notify)
                for (const auto &platform : changed)
                    notify(platform);
        },
        [](const drogon::orm::DrogonDbException &error) { LOG_WARN << "Version rules not loaded: " << error.base().what(); });
}
}

void onVersionChange(std::function<void(const std::string &platform)> next)
{
    std::lock_guard lock(mutex);
    listener = std::move(next);
}

void startVersionWatch()
{
    drogon::app().getLoop()->queueInLoop(refresh);
    drogon::app().getLoop()->runEvery(kRefreshSeconds, refresh);
}

bool allowed(const std::string &platform, const std::string &version)
{
    const auto code = parseVersion(version);
    if (code < 0)
        return false;
    std::lock_guard lock(mutex);
    if (!loaded)
        return true;
    const auto it = rules.find(platform);
    if (it == rules.end())
        return false;
    const auto &rule = it->second;
    return code >= rule.minVersion && std::find(rule.blocked.begin(), rule.blocked.end(), code) == rule.blocked.end();
}

drogon::Task<Json::Value> versionInfo(std::string platform)
{
    const auto db = database::client();
    const auto rule = co_await db->execSqlCoro(
        "SELECT min_version, blocked_versions::text AS blocked, flags::text AS flags FROM app_platforms WHERE platform = $1",
        platform);
    Json::Value body;
    if (rule.empty())
        co_return body;

    body["minVersion"] = rule[0]["min_version"].as<int>();
    body["blocked"] = Json::arrayValue;
    for (const auto code : parseList(rule[0]["blocked"].as<std::string>()))
        body["blocked"].append(code);

    Json::Value flags;
    Json::CharReaderBuilder builder;
    std::string errors;
    const auto text = rule[0]["flags"].as<std::string>();
    std::unique_ptr<Json::CharReader> reader(builder.newCharReader());
    body["flags"] = reader->parse(text.data(), text.data() + text.size(), &flags, &errors) ? flags : Json::Value(Json::objectValue);

    const auto history = co_await db->execSqlCoro(
        "SELECT version_code, version_name, notes, download_url, play_url, "
        "(extract(epoch FROM created_at) * 1000)::bigint AS created_at "
        "FROM app_releases WHERE platform = $1 ORDER BY version_code DESC LIMIT 20",
        platform);
    body["releases"] = Json::arrayValue;
    body["latest"] = Json::nullValue;
    for (const auto &row : history)
    {
        if (!releaseFileMatches(row["download_url"].as<std::string>(), row["version_code"].as<int>()))
            continue;
        if (body["latest"].isNull())
        {
            Json::Value release;
            release["versionCode"] = row["version_code"].as<int>();
            release["versionName"] = row["version_name"].as<std::string>();
            release["notes"] = row["notes"].as<std::string>();
            release["downloadUrl"] = row["download_url"].as<std::string>();
            release["playUrl"] = row["play_url"].as<std::string>();
            release["createdAt"] = Json::Int64(row["created_at"].as<int64_t>());
            body["latest"] = release;
        }
        Json::Value item;
        item["versionCode"] = row["version_code"].as<int>();
        item["versionName"] = row["version_name"].as<std::string>();
        item["notes"] = row["notes"].as<std::string>();
        item["createdAt"] = Json::Int64(row["created_at"].as<int64_t>());
        body["releases"].append(item);
    }
    if (platform == "android")
    {
        const auto good = body["latest"].isNull() ? 0 : body["latest"]["versionCode"].asInt();
        if (body["minVersion"].asInt() > good)
            body["minVersion"] = good;
    }

    co_return body;
}
}
