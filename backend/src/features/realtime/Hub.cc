#include "features/realtime/Hub.h"

#include <vector>

#include <json/writer.h>

namespace features::realtime
{
bool Hub::add(int64_t userId, const drogon::WebSocketConnectionPtr &connection, bool visible)
{
    std::lock_guard lock(mutex_);
    connections_[userId].insert(connection);
    if (!visible)
        return false;
    return ++visibleCounts_[userId] == 1;
}

bool Hub::remove(int64_t userId, const drogon::WebSocketConnectionPtr &connection, bool visible)
{
    std::lock_guard lock(mutex_);
    const auto it = connections_.find(userId);
    if (it == connections_.end() || it->second.erase(connection) == 0)
        return false;
    if (it->second.empty())
        connections_.erase(it);
    if (!visible)
        return false;
    const auto count = visibleCounts_.find(userId);
    if (count == visibleCounts_.end() || --count->second > 0)
        return false;
    visibleCounts_.erase(count);
    return true;
}

bool Hub::isOnline(int64_t userId) const
{
    std::lock_guard lock(mutex_);
    return visibleCounts_.contains(userId);
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
