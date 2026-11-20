#pragma once

#include <cstdint>
#include <string>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/channels/ChannelRepository.h"
#include "features/roles/RoleRepository.h"
#include "features/rooms/Authority.h"
#include "features/rooms/RoomRepository.h"

namespace features::channels
{
std::string cleanChannelName(const std::string &text);
Json::Value toJson(const Category &category);
Json::Value toJson(const Channel &channel);

class ChannelService
{
  public:
    drogon::Task<Json::Value> list(int64_t roomId, int64_t userId);
    drogon::Task<Channel> create(int64_t roomId, int64_t userId, Json::Value body);
    drogon::Task<Channel> update(int64_t roomId, int64_t channelId, int64_t userId, Json::Value body);
    drogon::Task<> remove(int64_t roomId, int64_t channelId, int64_t userId);
    drogon::Task<Category> createCategory(int64_t roomId, int64_t userId, Json::Value body);
    drogon::Task<Category> updateCategory(int64_t roomId, int64_t categoryId, int64_t userId, Json::Value body);
    drogon::Task<> removeCategory(int64_t roomId, int64_t categoryId, int64_t userId);
    drogon::Task<Json::Value> channelPermissions(int64_t roomId, int64_t channelId, int64_t userId);
    drogon::Task<Json::Value> setChannelOverride(int64_t roomId,
                                                 int64_t channelId,
                                                 int64_t userId,
                                                 std::string targetType,
                                                 std::string targetId,
                                                 Json::Value body);
    drogon::Task<Json::Value> syncChannel(int64_t roomId, int64_t channelId, int64_t userId);
    drogon::Task<Json::Value> categoryPermissions(int64_t roomId, int64_t categoryId, int64_t userId);
    drogon::Task<Json::Value> setCategoryOverride(int64_t roomId,
                                                  int64_t categoryId,
                                                  int64_t userId,
                                                  std::string targetType,
                                                  std::string targetId,
                                                  Json::Value body);

  private:
    drogon::Task<rooms::RoomAccess> requireMember(int64_t roomId, int64_t userId);
    drogon::Task<> requireManager(int64_t roomId, int64_t userId);
    drogon::Task<Channel> requireChannel(int64_t roomId, int64_t channelId, int64_t userId);
    drogon::Task<Category> requireCategory(int64_t roomId, int64_t categoryId);
    drogon::Task<int64_t> categoryFrom(int64_t roomId, const Json::Value &value);
    drogon::Task<> notify(int64_t roomId);
    drogon::Task<Override> checkOverride(int64_t roomId,
                                         const rooms::Authority &authority,
                                         const std::vector<std::string> &scopePermissions,
                                         const std::vector<Override> &current,
                                         const std::string &targetType,
                                         const std::string &targetId,
                                         const Json::Value &body);
    Json::Value permissionsJson(const std::vector<Override> &overrides, const std::vector<std::string> &permissions);

    ChannelRepository channels_;
    roles::RoleRepository roles_;
    rooms::RoomRepository rooms_;
};
}
