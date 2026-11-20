#include "features/roles/RoleRepository.h"

#include "core/database/Database.h"
#include "features/rooms/Authority.h"

namespace features::roles
{
namespace
{
const char *const kRoleSelect =
    "SELECT rr.id, rr.name, COALESCE(rr.color, '') AS color, rr.separate, "
    "array_to_string(rr.permissions, ',') AS permissions, rr.position, "
    "(SELECT count(*) FROM member_roles mr JOIN chat_members cm ON cm.chat_id = mr.room_id AND cm.user_id = mr.user_id "
    "AND cm.status = 'active' WHERE mr.role_id = rr.id) AS member_count "
    "FROM room_roles rr ";

Role roleFromRow(const drogon::orm::Row &row)
{
    return Role{row["id"].as<int64_t>(),
                row["name"].as<std::string>(),
                row["color"].as<std::string>(),
                row["separate"].as<bool>(),
                rooms::splitKeys(row["permissions"].as<std::string>()),
                row["position"].as<int>(),
                row["member_count"].as<int64_t>()};
}

std::string arrayLiteral(const std::vector<std::string> &keys)
{
    std::string text = "{";
    for (size_t i = 0; i < keys.size(); ++i)
    {
        if (i > 0)
            text += ',';
        text += keys[i];
    }
    return text + "}";
}
}

drogon::Task<std::vector<Role>> RoleRepository::list(int64_t roomId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kRoleSelect) + "WHERE rr.room_id = $1 ORDER BY rr.position, rr.id", roomId);
    std::vector<Role> roles;
    roles.reserve(result.size());
    for (const auto &row : result)
        roles.push_back(roleFromRow(row));
    co_return roles;
}

drogon::Task<std::optional<Role>> RoleRepository::find(int64_t roomId, int64_t roleId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kRoleSelect) + "WHERE rr.room_id = $1 AND rr.id = $2", roomId, roleId);
    if (result.empty())
        co_return std::nullopt;
    co_return roleFromRow(result[0]);
}

drogon::Task<int64_t> RoleRepository::count(int64_t roomId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT count(*) AS total FROM room_roles WHERE room_id = $1", roomId);
    co_return result[0]["total"].as<int64_t>();
}

drogon::Task<> RoleRepository::create(int64_t roleId, int64_t roomId, RoleFields fields)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO room_roles (id, room_id, name, color, separate, permissions, position) "
        "SELECT $1, $2, $3, NULLIF($4::text, ''), $5, $6::text[], "
        "COALESCE((SELECT max(position) + 1 FROM room_roles WHERE room_id = $2), 0)",
        roleId,
        roomId,
        fields.name,
        fields.color,
        fields.separate,
        arrayLiteral(fields.permissions));
}

drogon::Task<> RoleRepository::update(int64_t roleId, RoleFields fields)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_roles SET name = $2, color = NULLIF($3::text, ''), separate = $4, permissions = $5::text[] "
        "WHERE id = $1",
        roleId,
        fields.name,
        fields.color,
        fields.separate,
        arrayLiteral(fields.permissions));
}

drogon::Task<> RoleRepository::swap(int64_t roomId, int first, int second)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_roles SET position = CASE WHEN position = $2 THEN $3 ELSE $2 END "
        "WHERE room_id = $1 AND position IN ($2, $3)",
        roomId,
        first,
        second);
}

drogon::Task<> RoleRepository::remove(int64_t roomId, int64_t roleId, int position)
{
    auto transaction = co_await core::database::client()->newTransactionCoro();
    co_await transaction->execSqlCoro("DELETE FROM room_roles WHERE id = $1", roleId);
    co_await transaction->execSqlCoro(
        "DELETE FROM channel_overrides WHERE room_id = $1 AND target_type = 'role' AND target_id = $2", roomId, roleId);
    co_await transaction->execSqlCoro(
        "UPDATE room_roles SET position = position - 1 WHERE room_id = $1 AND position > $2", roomId, position);
}

drogon::Task<std::vector<int64_t>> RoleRepository::memberRoleIds(int64_t roomId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT role_id FROM member_roles WHERE room_id = $1 AND user_id = $2", roomId, userId);
    std::vector<int64_t> ids;
    ids.reserve(result.size());
    for (const auto &row : result)
        ids.push_back(row["role_id"].as<int64_t>());
    co_return ids;
}

drogon::Task<> RoleRepository::setMemberRoles(int64_t roomId, int64_t userId, std::vector<int64_t> roleIds)
{
    auto transaction = co_await core::database::client()->newTransactionCoro();
    co_await transaction->execSqlCoro("DELETE FROM member_roles WHERE room_id = $1 AND user_id = $2", roomId, userId);
    for (const auto roleId : roleIds)
        co_await transaction->execSqlCoro(
            "INSERT INTO member_roles (room_id, user_id, role_id) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING",
            roomId,
            userId,
            roleId);
}
}
