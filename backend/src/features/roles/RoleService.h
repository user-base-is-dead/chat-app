#pragma once

#include <cstdint>
#include <vector>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/roles/RoleRepository.h"
#include "features/rooms/Authority.h"
#include "features/rooms/RoomRepository.h"

namespace features::roles
{
Json::Value toJson(const Role &role);

class RoleService
{
  public:
    drogon::Task<Json::Value> list(int64_t roomId, int64_t userId);
    drogon::Task<Role> create(int64_t roomId, int64_t userId, Json::Value body);
    drogon::Task<Role> update(int64_t roomId, int64_t roleId, int64_t userId, Json::Value body);
    drogon::Task<> remove(int64_t roomId, int64_t roleId, int64_t userId);
    drogon::Task<std::vector<int64_t>> setMemberRoles(int64_t roomId, int64_t targetId, int64_t userId, Json::Value body);

  private:
    drogon::Task<rooms::RoomAccess> requireMember(int64_t roomId, int64_t userId);
    drogon::Task<rooms::Authority> requireManager(int64_t roomId, int64_t userId);
    drogon::Task<Role> requireRole(int64_t roomId, int64_t roleId);

    RoleRepository roles_;
    rooms::RoomRepository rooms_;
};
}
