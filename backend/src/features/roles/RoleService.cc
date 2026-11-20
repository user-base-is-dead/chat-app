#include "features/roles/RoleService.h"

#include <algorithm>
#include <set>

#include "core/http/ApiError.h"
#include "core/http/Params.h"
#include "core/id/Snowflake.h"
#include "core/text/Text.h"
#include "features/rooms/RoomService.h"

namespace features::roles
{
namespace
{
using core::http::ApiError;

constexpr int64_t kMaxRoles = 25;
constexpr size_t kMaxName = 32;
const std::set<std::string> kColors = {
    "red", "orange", "amber", "yellow", "lime", "green", "teal", "cyan", "blue", "indigo", "purple", "pink"};

ApiError notFound()
{
    return ApiError(drogon::k404NotFound, "room_not_found", "Not found");
}

ApiError notAllowed()
{
    return ApiError(drogon::k403Forbidden, "not_allowed", "You do not have permission to do this");
}

RoleFields readFields(const Json::Value &body, RoleFields fields)
{
    if (body.isMember("name"))
    {
        if (!body["name"].isString())
            throw ApiError(drogon::k400BadRequest, "invalid_name", "Role name must be 1-32 characters");
        fields.name = core::text::trim(body["name"].asString());
    }
    if (fields.name.empty() || core::text::characterCount(fields.name) > kMaxName)
        throw ApiError(drogon::k400BadRequest, "invalid_name", "Role name must be 1-32 characters");

    if (body.isMember("color"))
    {
        const auto &color = body["color"];
        if (color.isNull() || (color.isString() && color.asString().empty()))
            fields.color.clear();
        else if (color.isString() && kColors.contains(color.asString()))
            fields.color = color.asString();
        else
            throw ApiError(drogon::k400BadRequest, "invalid_color", "Unknown colour");
    }

    if (body.isMember("separate"))
    {
        if (!body["separate"].isBool())
            throw ApiError(drogon::k400BadRequest, "invalid_separate", "separate must be true or false");
        fields.separate = body["separate"].asBool();
    }

    if (body.isMember("permissions"))
    {
        if (!body["permissions"].isArray())
            throw ApiError(drogon::k400BadRequest, "invalid_permission", "Unknown permission");
        std::set<std::string> keys;
        for (const auto &item : body["permissions"])
        {
            if (!item.isString() || !rooms::isPermissionKey(item.asString()))
                throw ApiError(drogon::k400BadRequest, "invalid_permission", "Unknown permission");
            keys.insert(item.asString());
        }
        fields.permissions.clear();
        for (const auto &key : rooms::kPermissionKeys)
            if (keys.contains(key))
                fields.permissions.push_back(key);
    }
    return fields;
}

void requireGrantable(const rooms::Authority &authority,
                      const std::vector<std::string> &before,
                      const std::vector<std::string> &after)
{
    if (authority.owner)
        return;
    for (const auto &key : after)
        if (std::find(before.begin(), before.end(), key) == before.end() && !authority.has(key))
            throw notAllowed();
    for (const auto &key : before)
        if (std::find(after.begin(), after.end(), key) == after.end() && !authority.has(key))
            throw notAllowed();
}
}

Json::Value toJson(const Role &role)
{
    Json::Value json;
    json["id"] = std::to_string(role.id);
    json["name"] = role.name;
    json["color"] = role.color.empty() ? Json::Value() : Json::Value(role.color);
    json["separate"] = role.separate;
    json["permissions"] = Json::Value(Json::arrayValue);
    for (const auto &key : role.permissions)
        json["permissions"].append(key);
    json["position"] = role.position;
    json["memberCount"] = static_cast<Json::Int64>(role.memberCount);
    return json;
}

drogon::Task<Json::Value> RoleService::list(int64_t roomId, int64_t userId)
{
    co_await requireMember(roomId, userId);
    Json::Value body(Json::arrayValue);
    for (const auto &role : co_await roles_.list(roomId))
        body.append(toJson(role));
    co_return body;
}

drogon::Task<Role> RoleService::create(int64_t roomId, int64_t userId, Json::Value body)
{
    const auto authority = co_await requireManager(roomId, userId);
    if (co_await roles_.count(roomId) >= kMaxRoles)
        throw ApiError(drogon::k409Conflict, "too_many", "A group can have up to 25 roles");
    const auto fields = readFields(body, RoleFields{"", "", false, {}});
    requireGrantable(authority, {}, fields.permissions);
    const auto roleId = core::id::next();
    co_await roles_.create(roleId, roomId, fields);
    co_await rooms::notifyRoomChanged(roomId);
    co_return co_await requireRole(roomId, roleId);
}

drogon::Task<Role> RoleService::update(int64_t roomId, int64_t roleId, int64_t userId, Json::Value body)
{
    const auto authority = co_await requireManager(roomId, userId);
    const auto role = co_await requireRole(roomId, roleId);
    if (!authority.above(role.position))
        throw notAllowed();

    if (body["move"].isString())
    {
        const auto move = body["move"].asString();
        if (move != "up" && move != "down")
            throw ApiError(drogon::k400BadRequest, "invalid_move", "move must be up or down");
        const int target = move == "up" ? role.position - 1 : role.position + 1;
        const auto total = co_await roles_.count(roomId);
        if (target < 0 || target >= total)
            throw ApiError(drogon::k409Conflict, "cannot_move", "This role cannot move further");
        if (!authority.above(target))
            throw notAllowed();
        co_await roles_.swap(roomId, role.position, target);
    }
    else
    {
        const auto fields = readFields(body, RoleFields{role.name, role.color, role.separate, role.permissions});
        requireGrantable(authority, role.permissions, fields.permissions);
        co_await roles_.update(roleId, fields);
    }
    co_await rooms::notifyRoomChanged(roomId);
    co_return co_await requireRole(roomId, roleId);
}

drogon::Task<> RoleService::remove(int64_t roomId, int64_t roleId, int64_t userId)
{
    const auto authority = co_await requireManager(roomId, userId);
    const auto role = co_await requireRole(roomId, roleId);
    if (!authority.above(role.position))
        throw notAllowed();
    co_await roles_.remove(roomId, roleId, role.position);
    co_await rooms::notifyRoomChanged(roomId);
}

drogon::Task<std::vector<int64_t>> RoleService::setMemberRoles(int64_t roomId,
                                                               int64_t targetId,
                                                               int64_t userId,
                                                               Json::Value body)
{
    const auto authority = co_await requireManager(roomId, userId);
    const auto target = co_await rooms_.access(roomId, targetId);
    if (!target || target->status != "active")
        throw ApiError(drogon::k400BadRequest, "invalid_member", "Choose a current member");
    const auto targetAuthority = rooms::authorityOf(*target);
    if (!authority.owner && !authority.outranks(targetAuthority.owner, targetAuthority.rank))
        throw notAllowed();

    if (!body["roleIds"].isArray())
        throw ApiError(drogon::k400BadRequest, "invalid_role", "roleIds must be a list");
    std::set<int64_t> wanted;
    for (const auto &item : body["roleIds"])
    {
        if (!item.isString())
            throw ApiError(drogon::k400BadRequest, "invalid_role", "Unknown role");
        wanted.insert(core::http::parseId(item.asString(), "roleId"));
    }

    const auto roles = co_await roles_.list(roomId);
    const auto positionOf = [&roles](int64_t id) -> std::optional<int> {
        for (const auto &role : roles)
            if (role.id == id)
                return role.position;
        return std::nullopt;
    };
    for (const auto id : wanted)
        if (!positionOf(id))
            throw ApiError(drogon::k400BadRequest, "invalid_role", "Unknown role");

    const auto current = co_await roles_.memberRoleIds(roomId, targetId);
    const std::set<int64_t> had(current.begin(), current.end());
    std::vector<int64_t> result;
    for (const auto &role : roles)
    {
        const bool before = had.contains(role.id);
        const bool after = wanted.contains(role.id);
        const bool keep = (before == after || !authority.above(role.position)) ? before : after;
        if (keep)
            result.push_back(role.id);
    }
    co_await roles_.setMemberRoles(roomId, targetId, result);
    co_await rooms::notifyRoomChanged(roomId);
    co_return result;
}

drogon::Task<rooms::RoomAccess> RoleService::requireMember(int64_t roomId, int64_t userId)
{
    const auto room = co_await rooms_.access(roomId, userId);
    if (!room || room->kind != "group" || room->roomId != roomId || room->status != "active")
        throw notFound();
    co_return *room;
}

drogon::Task<rooms::Authority> RoleService::requireManager(int64_t roomId, int64_t userId)
{
    const auto authority = rooms::authorityOf(co_await requireMember(roomId, userId));
    if (!authority.has("manageRoles"))
        throw notAllowed();
    co_return authority;
}

drogon::Task<Role> RoleService::requireRole(int64_t roomId, int64_t roleId)
{
    const auto role = co_await roles_.find(roomId, roleId);
    if (!role)
        throw ApiError(drogon::k404NotFound, "role_not_found", "Role not found");
    co_return *role;
}
}
