#include "features/users/UserRepository.h"

#include <drogon/orm/Exception.h>

#include "core/database/Database.h"

namespace features::users
{
namespace
{
const char *const kUserColumns = "id, username, display_name, bio";

User toUser(const drogon::orm::Row &row)
{
    return User{row["id"].as<int64_t>(),
                row["username"].as<std::string>(),
                row["display_name"].as<std::string>(),
                row["bio"].as<std::string>()};
}

UserCredentials toCredentials(const drogon::orm::Row &row)
{
    return UserCredentials{
        toUser(row), row["password_hash"].as<std::string>(), !row["disabled_at"].isNull(), !row["deleted_at"].isNull()};
}
}

Json::Value toJson(const User &user)
{
    Json::Value json;
    json["id"] = std::to_string(user.id);
    json["username"] = user.username;
    json["displayName"] = user.displayName;
    json["bio"] = user.bio;
    return json;
}

drogon::Task<std::optional<User>> UserRepository::create(int64_t id,
                                                         std::string username,
                                                         std::string displayName,
                                                         std::string passwordHash)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("INSERT INTO users (id, username, display_name, password_hash) "
                    "SELECT $1, $2, $3, $4 WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE lower(username) = lower($2)) "
                    "ON CONFLICT (lower(username)) DO NOTHING RETURNING ") +
            kUserColumns,
        id,
        username,
        displayName,
        passwordHash);
    if (result.empty())
        co_return std::nullopt;
    co_return toUser(result[0]);
}

drogon::Task<std::optional<UserCredentials>> UserRepository::findCredentials(std::string username)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kUserColumns +
            ", password_hash, disabled_at, deleted_at FROM users WHERE lower(username) = lower($1) AND deleted_at IS NULL",
        username);
    if (result.empty())
        co_return std::nullopt;
    co_return toCredentials(result[0]);
}

drogon::Task<std::optional<UserCredentials>> UserRepository::findCredentialsById(int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kUserColumns + ", password_hash, disabled_at, deleted_at FROM users WHERE id = $1", id);
    if (result.empty())
        co_return std::nullopt;
    co_return toCredentials(result[0]);
}

drogon::Task<std::optional<User>> UserRepository::findById(int64_t id)
{
    const auto result =
        co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kUserColumns + " FROM users WHERE id = $1 AND deleted_at IS NULL", id);
    if (result.empty())
        co_return std::nullopt;
    co_return toUser(result[0]);
}

drogon::Task<std::vector<User>> UserRepository::search(std::string query, int64_t excludeUserId, int limit)
{
    std::string escaped;
    for (const char c : query)
    {
        if (c == '%' || c == '_' || c == '\\')
            escaped += '\\';
        escaped += c;
    }

    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kUserColumns +
            " FROM users WHERE lower(username) LIKE '%' || lower($1) || '%' AND id <> $2 AND disabled_at IS NULL AND deleted_at IS NULL "
            "ORDER BY lower(username) = lower($4) DESC, lower(username) LIKE lower($1) || '%' DESC, "
            "length(username), lower(username) LIMIT $3",
        escaped,
        excludeUserId,
        static_cast<int64_t>(limit),
        query);

    std::vector<User> users;
    users.reserve(result.size());
    for (const auto &row : result)
        users.push_back(toUser(row));
    co_return users;
}

drogon::Task<int64_t> UserRepository::touchLastSeen(int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "UPDATE users SET last_seen_at = now() WHERE id = $1 "
        "RETURNING (extract(epoch FROM last_seen_at) * 1000)::bigint AS last_seen",
        id);
    co_return result.empty() ? 0 : result[0]["last_seen"].as<int64_t>();
}

drogon::Task<std::optional<bool>> UserRepository::showsLastSeen(int64_t id)
{
    const auto result =
        co_await core::database::client()->execSqlCoro("SELECT show_last_seen FROM users WHERE id = $1", id);
    if (result.empty())
        co_return std::nullopt;
    co_return result[0]["show_last_seen"].as<bool>();
}

drogon::Task<> UserRepository::setShowLastSeen(int64_t id, bool show)
{
    co_await core::database::client()->execSqlCoro("UPDATE users SET show_last_seen = $2 WHERE id = $1", id, show);
}

drogon::Task<std::optional<int64_t>> UserRepository::lastSeenAt(int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT (extract(epoch FROM last_seen_at) * 1000)::bigint AS last_seen FROM users WHERE id = $1", id);
    if (result.empty() || result[0]["last_seen"].isNull())
        co_return std::nullopt;
    co_return result[0]["last_seen"].as<int64_t>();
}

drogon::Task<std::optional<User>> UserRepository::updateProfile(int64_t id,
                                                               std::string username,
                                                               std::string displayName,
                                                               std::string bio)
{
    try
    {
        const auto result = co_await core::database::client()->execSqlCoro(
            std::string("UPDATE users SET username = $2, display_name = $3, bio = $4 WHERE id = $1 "
                        "AND NOT EXISTS (SELECT 1 FROM users WHERE lower(username) = lower($2) AND id <> $1) "
                        "AND NOT EXISTS (SELECT 1 FROM rooms WHERE lower(username) = lower($2)) "
                        "RETURNING ") +
                kUserColumns,
            id,
            username,
            displayName,
            bio);
        if (result.empty())
            co_return std::nullopt;
        co_return toUser(result[0]);
    }
    catch (const drogon::orm::SqlError &error)
    {
        if (error.sqlState() == "23505")
            co_return std::nullopt;
        throw;
    }
}

drogon::Task<> UserRepository::setDisabled(int64_t id, bool disabled)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE users SET disabled_at = CASE WHEN $2 THEN now() ELSE NULL END WHERE id = $1", id, disabled);
}

drogon::Task<> UserRepository::setPasswordHash(int64_t id, std::string passwordHash)
{
    co_await core::database::client()->execSqlCoro("UPDATE users SET password_hash = $2 WHERE id = $1", id, passwordHash);
}

drogon::Task<> UserRepository::deleteAccount(int64_t id)
{
    auto transaction = co_await core::database::client()->newTransactionCoro();
    co_await transaction->execSqlCoro(
        "DELETE FROM chats c WHERE c.id IN (SELECT chat_id FROM dm_pairs WHERE user_low = $1 OR user_high = $1) "
        "AND NOT EXISTS (SELECT 1 FROM messages m WHERE m.chat_id = c.id)",
        id);
    co_await transaction->execSqlCoro(
        "UPDATE chat_members cm SET role = 'owner' FROM ("
        "SELECT DISTINCT ON (o.chat_id) m.chat_id, m.user_id FROM chat_members o "
        "JOIN rooms r ON r.chat_id = o.chat_id "
        "JOIN chat_members m ON m.chat_id = o.chat_id AND m.user_id <> $1 AND m.status = 'active' "
        "WHERE o.user_id = $1 AND o.role = 'owner' "
        "ORDER BY o.chat_id, m.role = 'admin' DESC, (SELECT min(rr.position) FROM member_roles mr "
        "JOIN room_roles rr ON rr.id = mr.role_id WHERE mr.room_id = o.chat_id AND mr.user_id = m.user_id) NULLS LAST, "
        "m.joined_at, m.user_id) heir "
        "WHERE cm.chat_id = heir.chat_id AND cm.user_id = heir.user_id",
        id);
    co_await transaction->execSqlCoro(
        "DELETE FROM chats c WHERE c.id IN (SELECT r.chat_id FROM rooms r "
        "JOIN chat_members o ON o.chat_id = r.chat_id AND o.user_id = $1 AND o.role = 'owner' "
        "WHERE NOT EXISTS (SELECT 1 FROM chat_members m WHERE m.chat_id = r.chat_id AND m.user_id <> $1 AND m.status = 'active'))",
        id);
    co_await transaction->execSqlCoro(
        "DELETE FROM chat_members cm USING rooms r WHERE r.chat_id = cm.chat_id AND cm.user_id = $1", id);
    co_await transaction->execSqlCoro("DELETE FROM member_roles WHERE user_id = $1", id);
    co_await transaction->execSqlCoro("DELETE FROM devices WHERE user_id = $1", id);
    co_await transaction->execSqlCoro(
        "DELETE FROM media m WHERE m.owner_id = $1 AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.photo_id = m.id)", id);
    co_await transaction->execSqlCoro(
        "UPDATE users SET username = 'deleted_' || id, display_name = '', bio = '', password_hash = '', "
        "disabled_at = NULL, deleted_at = now() WHERE id = $1",
        id);
}
}
