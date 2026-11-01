#include "features/realtime/RealtimeController.h"

#include <chrono>
#include <memory>

#include <drogon/drogon.h>
#include <json/reader.h>

#include "core/database/Database.h"
#include "core/security/Tokens.h"
#include "features/chats/ChatRepository.h"
#include "features/realtime/Hub.h"
#include "features/users/UserRepository.h"

namespace features::realtime
{
namespace
{
constexpr auto kAuthTimeout = 10.0;
constexpr auto kPingInterval = std::chrono::seconds(20);
const auto kUnauthorized = static_cast<drogon::CloseCode>(4001);

struct Session
{
    int64_t userId = 0;
    int64_t deviceId = 0;
};

drogon::Task<> announcePresence(int64_t userId, bool online)
{
    try
    {
        Json::Value data;
        data["userId"] = std::to_string(userId);
        data["online"] = online;
        data["lastSeenAt"] = Json::Value();

        if (!online)
        {
            const auto lastSeen = co_await users::UserRepository().touchLastSeen(userId);
            if (hub().isOnline(userId))
                co_return;
            data["lastSeenAt"] = static_cast<Json::Int64>(lastSeen);
        }

        const auto peers = co_await chats::ChatRepository().peerIds(userId);
        for (const auto peerId : peers)
            hub().send(peerId, "presence", data);
    }
    catch (const std::exception &error)
    {
        LOG_ERROR << "presence update failed: " << error.what();
    }
}

drogon::Task<> authorize(std::weak_ptr<drogon::WebSocketConnection> weak, int64_t userId, int64_t deviceId)
{
    bool known = false;
    try
    {
        const auto result = co_await core::database::client()->execSqlCoro(
            "SELECT 1 FROM devices WHERE id = $1 AND user_id = $2", deviceId, userId);
        known = !result.empty();
    }
    catch (const std::exception &error)
    {
        LOG_ERROR << "realtime auth failed: " << error.what();
    }

    const auto connection = weak.lock();
    if (!connection)
        co_return;
    if (!known)
    {
        connection->shutdown(kUnauthorized, "Invalid access token");
        co_return;
    }

    const auto session = connection->getContext<Session>();
    session->userId = userId;
    session->deviceId = deviceId;
    const bool cameOnline = hub().add(userId, connection);
    if (!connection->connected())
    {
        hub().remove(userId, connection);
        co_return;
    }
    connection->send(R"({"event":"ready","data":{}})");
    if (cameOnline)
        co_await announcePresence(userId, true);
}

drogon::Task<> relayTyping(int64_t userId, int64_t chatId, bool active)
{
    try
    {
        const auto member = co_await chats::ChatRepository().membership(chatId, userId);
        if (!member || member->status != "active")
            co_return;

        Json::Value data;
        data["chatId"] = std::to_string(chatId);
        data["userId"] = std::to_string(userId);
        data["active"] = active;
        hub().send(member->peerId, "typing", data);
    }
    catch (const std::exception &error)
    {
        LOG_ERROR << "typing relay failed: " << error.what();
    }
}

bool parse(const std::string &text, Json::Value &out)
{
    Json::CharReaderBuilder builder;
    std::string errors;
    std::unique_ptr<Json::CharReader> reader(builder.newCharReader());
    return reader->parse(text.data(), text.data() + text.size(), &out, &errors) && out.isObject();
}

int64_t toId(const Json::Value &value)
{
    if (!value.isString())
        return 0;
    try
    {
        return std::stoll(value.asString());
    }
    catch (...)
    {
        return 0;
    }
}
}

void RealtimeController::handleNewConnection(const drogon::HttpRequestPtr &,
                                             const drogon::WebSocketConnectionPtr &connection)
{
    connection->setContext(std::make_shared<Session>());
    connection->setPingMessage("", kPingInterval);

    std::weak_ptr<drogon::WebSocketConnection> weak = connection;
    drogon::app().getLoop()->runAfter(kAuthTimeout, [weak] {
        const auto connection = weak.lock();
        if (connection && connection->getContext<Session>()->userId == 0)
            connection->shutdown(kUnauthorized, "Authentication timed out");
    });
}

void RealtimeController::handleNewMessage(const drogon::WebSocketConnectionPtr &connection,
                                          std::string &&message,
                                          const drogon::WebSocketMessageType &type)
{
    if (type != drogon::WebSocketMessageType::Text)
        return;

    Json::Value json;
    if (!parse(message, json))
        return;

    const auto event = json["event"].asString();
    const auto &data = json["data"];
    const auto session = connection->getContext<Session>();

    if (session->userId == 0)
    {
        if (event != "auth")
            return;
        const auto claims = core::security::verifyAccessToken(data["token"].asString());
        if (!claims)
        {
            connection->shutdown(kUnauthorized, "Invalid access token");
            return;
        }
        std::weak_ptr<drogon::WebSocketConnection> weak = connection;
        const auto userId = claims->userId;
        const auto deviceId = claims->deviceId;
        drogon::async_run([weak, userId, deviceId] { return authorize(weak, userId, deviceId); });
        return;
    }

    if (event == "typing")
    {
        const auto chatId = toId(data["chatId"]);
        if (chatId == 0)
            return;
        const auto userId = session->userId;
        const bool active = data["active"].asBool();
        drogon::async_run([userId, chatId, active] { return relayTyping(userId, chatId, active); });
    }
}

void RealtimeController::handleConnectionClosed(const drogon::WebSocketConnectionPtr &connection)
{
    const auto session = connection->getContext<Session>();
    if (!session || session->userId == 0)
        return;
    if (hub().remove(session->userId, connection))
    {
        const auto userId = session->userId;
        drogon::async_run([userId] { return announcePresence(userId, false); });
    }
}
}
