#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

#include "features/messages/Message.h"

namespace features::channels
{
struct Category
{
    int64_t id;
    std::string name;
    int position;
    std::vector<std::string> permissions;
};

struct Override
{
    std::string targetType;
    int64_t targetId;
    std::vector<std::string> allow;
    std::vector<std::string> deny;
};

struct Channel
{
    int64_t id;
    std::string name;
    std::optional<int64_t> categoryId;
    int position;
    int64_t unreadCount;
    int64_t lastReadId;
    int64_t readUpTo;
    std::optional<messages::Message> lastMessage;
    std::vector<std::string> permissions;
    bool isPrivate;
    bool synced;
};

class ChannelRepository
{
  public:
    drogon::Task<std::vector<Category>> categories(int64_t roomId, int64_t userId);
    drogon::Task<std::vector<Channel>> channels(int64_t roomId, int64_t userId);
    drogon::Task<std::optional<Channel>> channel(int64_t roomId, int64_t channelId, int64_t userId);
    drogon::Task<std::optional<Category>> category(int64_t roomId, int64_t categoryId, int64_t userId = 0);
    drogon::Task<int64_t> channelCount(int64_t roomId);
    drogon::Task<int64_t> categoryCount(int64_t roomId);
    drogon::Task<> createChannel(int64_t id, int64_t roomId, int64_t categoryId, std::string name);
    drogon::Task<> renameChannel(int64_t channelId, std::string name);
    drogon::Task<> setCategory(int64_t roomId, int64_t channelId, int64_t categoryId);
    drogon::Task<> moveChannel(int64_t roomId, int64_t channelId, bool up);
    drogon::Task<> removeChannel(int64_t channelId);
    drogon::Task<> createCategory(int64_t id, int64_t roomId, std::string name);
    drogon::Task<> renameCategory(int64_t categoryId, std::string name);
    drogon::Task<> moveCategory(int64_t roomId, int64_t categoryId, bool up);
    drogon::Task<> removeCategory(int64_t categoryId);
    drogon::Task<std::vector<Override>> overrides(int64_t scopeId);
    drogon::Task<> setOverride(int64_t scopeId, int64_t roomId, Override item);
    drogon::Task<> copyOverrides(int64_t fromScope, int64_t toScope, int64_t roomId);
    drogon::Task<> clearOverrides(int64_t scopeId);
    drogon::Task<> setSynced(int64_t channelId, bool synced);
};
}
