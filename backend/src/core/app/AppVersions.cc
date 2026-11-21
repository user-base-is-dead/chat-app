#include "core/app/AppVersions.h"

#include <algorithm>
#include <mutex>
#include <unordered_map>
#include <vector>

#include <drogon/drogon.h>
#include <json/reader.h>

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

void refresh()
{
    database::client()->execSqlAsync(
        "SELECT p.platform, p.min_version, p.blocked_versions::text AS blocked, p.flags::text AS flags, "
        "COALESCE((SELECT max(r.version_code) FROM app_releases r WHERE r.platform = p.platform), 0) AS latest "
        "FROM app_platforms p",
        [](const drogon::orm::Result &result) {
            std::unordered_map<std::string, Rule> next;
            std::unordered_map<std::string, std::string> nextStamps;
            for (const auto &row : result)
            {
                const auto platform = row["platform"].as<std::string>();
                const auto blocked = row["blocked"].as<std::string>();
                next[platform] = Rule{row["min_version"].as<int>(), parseList(blocked)};
                nextStamps[platform] = row["min_version"].as<std::string>() + "|" + blocked + "|" +
                                       row["flags"].as<std::string>() + "|" + row["latest"].as<std::string>();
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
        "SELECT version_code, version_name, notes, (extract(epoch FROM created_at) * 1000)::bigint AS created_at "
        "FROM app_releases WHERE platform = $1 ORDER BY version_code DESC LIMIT 20",
        platform);
    body["releases"] = Json::arrayValue;
    for (const auto &row : history)
    {
        Json::Value item;
        item["versionCode"] = row["version_code"].as<int>();
        item["versionName"] = row["version_name"].as<std::string>();
        item["notes"] = row["notes"].as<std::string>();
        item["createdAt"] = Json::Int64(row["created_at"].as<int64_t>());
        body["releases"].append(item);
    }

    const auto latest = co_await db->execSqlCoro(
        "SELECT version_code, version_name, notes, download_url, play_url, "
        "(extract(epoch FROM created_at) * 1000)::bigint AS created_at FROM app_releases "
        "WHERE platform = $1 ORDER BY version_code DESC LIMIT 1",
        platform);
    if (latest.empty())
    {
        body["latest"] = Json::nullValue;
        co_return body;
    }
    Json::Value release;
    release["versionCode"] = latest[0]["version_code"].as<int>();
    release["versionName"] = latest[0]["version_name"].as<std::string>();
    release["notes"] = latest[0]["notes"].as<std::string>();
    release["downloadUrl"] = latest[0]["download_url"].as<std::string>();
    release["playUrl"] = latest[0]["play_url"].as<std::string>();
    release["createdAt"] = Json::Int64(latest[0]["created_at"].as<int64_t>());
    body["latest"] = release;
    co_return body;
}
}
