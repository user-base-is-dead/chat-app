#include "features/realtime/Hub.h"

#include <vector>

#include <json/writer.h>

namespace features::realtime
{
bool Hub::add(int64_t userId, const drogon::WebSocketConnectionPtr &connection)
{
    std::lock_guard lock(mutex_);
    auto &userConnections = connections_[userId];
    const bool cameOnline = userConnections.empty();
    userConnections.insert(connection);
    return cameOnline;
}

bool Hub::remove(int64_t userId, const drogon::WebSocketConnectionPtr &connection)
{
    std::lock_guard lock(mutex_);
    const auto it = connections_.find(userId);
    if (it == connections_.end())
        return false;
    it->second.erase(connection);
    if (!it->second.empty())
        return false;
    connections_.erase(it);
    return true;
}

bool Hub::isOnline(int64_t userId) const
{
    std::lock_guard lock(mutex_);
    return connections_.contains(userId);
}

void Hub::send(int64_t userId, const std::string &event, const Json::Value &data) const
{
    std::vector<drogon::WebSocketConnectionPtr> targets;
    {
        std::lock_guard lock(mutex_);
        const auto it = connections_.find(userId);
        if (it == connections_.end())
            return;
        targets.assign(it->second.begin(), it->second.end());
    }

    Json::Value envelope;
    envelope["event"] = event;
    envelope["data"] = data;
    Json::StreamWriterBuilder builder;
    builder["indentation"] = "";
    const auto payload = Json::writeString(builder, envelope);

    for (const auto &connection : targets)
        connection->send(payload);
}

void Hub::disconnect(int64_t userId) const
{
    std::vector<drogon::WebSocketConnectionPtr> targets;
    {
        std::lock_guard lock(mutex_);
        const auto it = connections_.find(userId);
        if (it == connections_.end())
            return;
        targets.assign(it->second.begin(), it->second.end());
    }
    for (const auto &connection : targets)
        connection->shutdown(drogon::CloseCode::kNormalClosure, "Account closed");
}

Hub &hub()
{
    static Hub instance;
    return instance;
}
}
