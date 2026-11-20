#include "features/realtime/Presence.h"

#include <unordered_set>
#include <vector>

#include <drogon/drogon.h>

#include "features/chats/ChatRepository.h"
#include "features/realtime/Hub.h"
#include "features/rooms/RoomRepository.h"
#include "features/users/UserRepository.h"

namespace features::realtime
{
namespace
{
drogon::Task<std::vector<int64_t>> receivers(int64_t userId)
{
    auto ids = co_await chats::ChatRepository().peerIds(userId);
    const auto mates = co_await rooms::RoomRepository().groupMateIds(userId);
    std::unordered_set<int64_t> seen(ids.begin(), ids.end());
    for (const auto id : mates)
        if (seen.insert(id).second)
            ids.push_back(id);
    co_return ids;
}

drogon::Task<> sendToReceivers(int64_t userId, const Json::Value &data)
{
    for (const auto id : co_await receivers(userId))
        hub().send(id, "presence", data);
}
}

drogon::Task<> announcePresence(int64_t userId, bool online)
{
    try
    {
        users::UserRepository users;
        Json::Value data;
        data["userId"] = std::to_string(userId);
        data["online"] = online;
        data["lastSeenAt"] = Json::Value();
        data["hidden"] = false;

        if (!online)
        {
            const auto lastSeen = co_await users.touchLastSeen(userId);
            if (hub().isOnline(userId))
                co_return;
            data["lastSeenAt"] = static_cast<Json::Int64>(lastSeen);
        }

        if (!(co_await users.showsLastSeen(userId)).value_or(false))
            co_return;
        co_await sendToReceivers(userId, data);
    }
    catch (const std::exception &error)
    {
        LOG_ERROR << "presence update failed: " << error.what();
    }
}

drogon::Task<> announceVisibility(int64_t userId, bool show)
{
    try
    {
        const bool online = show && hub().isOnline(userId);
        Json::Value data;
        data["userId"] = std::to_string(userId);
        data["online"] = online;
        data["hidden"] = !show;
        data["lastSeenAt"] = Json::Value();
        if (show && !online)
        {
            const auto lastSeen = co_await users::UserRepository().lastSeenAt(userId);
            if (lastSeen)
                data["lastSeenAt"] = static_cast<Json::Int64>(*lastSeen);
        }
        co_await sendToReceivers(userId, data);
    }
    catch (const std::exception &error)
    {
        LOG_ERROR << "visibility update failed: " << error.what();
    }
}
}
