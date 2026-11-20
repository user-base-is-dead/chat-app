#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

namespace features::roles
{
struct Role
{
    int64_t id;
    std::string name;
    std::string color;
    bool separate;
    std::vector<std::string> permissions;
    int position;
    int64_t memberCount;
};

struct RoleFields
{
    std::string name;
    std::string color;
    bool separate;
    std::vector<std::string> permissions;
};

class RoleRepository
{
  public:
    drogon::Task<std::vector<Role>> list(int64_t roomId);
    drogon::Task<std::optional<Role>> find(int64_t roomId, int64_t roleId);
    drogon::Task<int64_t> count(int64_t roomId);
    drogon::Task<> create(int64_t roleId, int64_t roomId, RoleFields fields);
    drogon::Task<> update(int64_t roleId, RoleFields fields);
    drogon::Task<> swap(int64_t roomId, int first, int second);
    drogon::Task<> remove(int64_t roomId, int64_t roleId, int position);
    drogon::Task<std::vector<int64_t>> memberRoleIds(int64_t roomId, int64_t userId);
    drogon::Task<> setMemberRoles(int64_t roomId, int64_t userId, std::vector<int64_t> roleIds);
};
}
