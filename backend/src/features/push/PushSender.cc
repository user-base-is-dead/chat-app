#include "features/push/PushSender.h"

#include <chrono>
#include <fstream>
#include <mutex>
#include <optional>
#include <sstream>

#include <drogon/HttpClient.h>
#include <drogon/drogon.h>
#include <drogon/utils/Utilities.h>
#include <json/reader.h>
#include <json/writer.h>
#include <jwt-cpp/jwt.h>

#include "core/config/Settings.h"
#include "features/devices/DeviceRepository.h"

namespace features::push
{
namespace
{
constexpr auto kScope = "https://www.googleapis.com/auth/firebase.messaging";
constexpr auto kTokenHost = "https://oauth2.googleapis.com";
constexpr auto kSendHost = "https://fcm.googleapis.com";
constexpr double kTimeout = 10.0;

struct ServiceKey
{
    std::string projectId;
    std::string clientEmail;
    std::string privateKey;
};

struct AccessToken
{
    std::string value;
    std::chrono::system_clock::time_point expiresAt;
};

std::mutex mutex;
std::optional<ServiceKey> key;
bool keyLoaded = false;
AccessToken access;

const std::optional<ServiceKey> &serviceKey()
{
    std::lock_guard lock(mutex);
    if (keyLoaded)
        return key;
    keyLoaded = true;
    const auto &path = core::config::settings().fcmKeyFile;
    if (path.empty())
        return key;
    std::ifstream file(path);
    Json::Value json;
    Json::CharReaderBuilder builder;
    std::string errors;
    if (!file || !Json::parseFromStream(builder, file, &json, &errors))
    {
        LOG_WARN << "Push disabled, cannot read FCM key file " << path;
        return key;
    }
    key = ServiceKey{json["project_id"].asString(), json["client_email"].asString(), json["private_key"].asString()};
    LOG_INFO << "Push enabled for project " << key->projectId;
    return key;
}

std::string compact(const Json::Value &value)
{
    Json::StreamWriterBuilder builder;
    builder["indentation"] = "";
    return Json::writeString(builder, value);
}

drogon::Task<std::string> accessToken(const ServiceKey &service)
{
    const auto now = std::chrono::system_clock::now();
    {
        std::lock_guard lock(mutex);
        if (!access.value.empty() && now < access.expiresAt)
            co_return access.value;
    }

    const auto assertion = jwt::create()
                               .set_type("JWT")
                               .set_issuer(service.clientEmail)
                               .set_audience(std::string(kTokenHost) + "/token")
                               .set_payload_claim("scope", jwt::claim(std::string(kScope)))
                               .set_issued_at(now)
                               .set_expires_at(now + std::chrono::hours(1))
                               .sign(jwt::algorithm::rs256("", service.privateKey, "", ""));

    auto request = drogon::HttpRequest::newHttpRequest();
    request->setMethod(drogon::Post);
    request->setPath("/token");
    request->setContentTypeCode(drogon::CT_APPLICATION_X_FORM);
    request->setBody("grant_type=" + drogon::utils::urlEncodeComponent("urn:ietf:params:oauth:grant-type:jwt-bearer") +
                     "&assertion=" + assertion);
    const auto response = co_await drogon::HttpClient::newHttpClient(kTokenHost)->sendRequestCoro(request, kTimeout);
    const auto body = response->getJsonObject();
    if (response->getStatusCode() != drogon::k200OK || !body)
        throw std::runtime_error("FCM auth failed: " + std::string(response->body()));

    std::lock_guard lock(mutex);
    access.value = (*body)["access_token"].asString();
    access.expiresAt = now + std::chrono::seconds((*body)["expires_in"].asInt() - 60);
    co_return access.value;
}

drogon::Task<> sendTo(const ServiceKey &service, std::string token, std::string chatId)
{
    Json::Value message;
    message["token"] = token;
    message["data"]["chatId"] = chatId;
    message["android"]["priority"] = "HIGH";
    message["android"]["collapse_key"] = chatId;
    Json::Value body;
    body["message"] = message;

    auto request = drogon::HttpRequest::newHttpRequest();
    request->setMethod(drogon::Post);
    request->setPath("/v1/projects/" + service.projectId + "/messages:send");
    request->setContentTypeCode(drogon::CT_APPLICATION_JSON);
    request->addHeader("Authorization", "Bearer " + co_await accessToken(service));
    request->setBody(compact(body));
    const auto response = co_await drogon::HttpClient::newHttpClient(kSendHost)->sendRequestCoro(request, kTimeout);
    const auto status = response->getStatusCode();
    if (status == drogon::k200OK)
        co_return;
    const std::string text(response->body());
    if (status == drogon::k404NotFound || text.find("UNREGISTERED") != std::string::npos)
    {
        co_await devices::DeviceRepository().clearPushToken(token);
        co_return;
    }
    if (status == drogon::k401Unauthorized)
    {
        std::lock_guard lock(mutex);
        access.value.clear();
    }
    LOG_WARN << "FCM send failed " << status << ": " << text;
}
}

bool enabled()
{
    return serviceKey().has_value();
}

void notifyNew(int64_t userId, std::string chatId)
{
    if (!enabled())
        return;
    drogon::async_run([userId, chatId = std::move(chatId)]() -> drogon::Task<> {
        try
        {
            const auto &service = *serviceKey();
            const auto tokens = co_await devices::DeviceRepository().pushTokens(userId);
            for (const auto &token : tokens)
                co_await sendTo(service, token, chatId);
        }
        catch (const std::exception &error)
        {
            LOG_WARN << "Push failed: " << error.what();
        }
    });
}
}
