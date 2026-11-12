#include "features/rooms/RoomRepository.h"

#include <drogon/orm/Exception.h>

#include "core/database/Database.h"
#include "features/realtime/Hub.h"

namespace features::rooms
{
namespace
{
const char *const kSummarySelect =
    "SELECT c.id, c.type, r.title, r.about, COALESCE(r.username, '') AS username, r.is_public, r.invite_code, "
    "r.members_can_send, r.members_can_add, COALESCE(md.storage_key, '') AS photo, "
    "me.status, me.role, me.last_read_message_id, me.muted, "
    "(extract(epoch FROM me.pinned_at) * 1000)::bigint AS pinned_at, "
    "(SELECT count(*) FROM chat_members x WHERE x.chat_id = c.id AND x.status = 'active') AS member_count, "
    "(SELECT COALESCE(max(x.last_read_message_id), 0) FROM chat_members x "
    "WHERE x.chat_id = c.id AND x.user_id <> me.user_id AND x.status = 'active') AS read_up_to, "
    "(SELECT count(*) FROM messages um WHERE um.chat_id = c.id AND um.id > me.last_read_message_id "
    "AND um.sender_id <> me.user_id) AS unread_count, "
    "lm.id AS lm_id, lm.chat_id AS lm_chat_id, lm.sender_id AS lm_sender_id, lm.sender_name AS lm_sender_name, "
    "lm.ciphertext AS lm_ciphertext, lm.edited_at AS lm_edited_at, lm.deleted_at AS lm_deleted_at, "
    "lm.created_at AS lm_created_at "
    "FROM chat_members me "
    "JOIN chats c ON c.id = me.chat_id AND c.type <> 'dm' "
    "JOIN rooms r ON r.chat_id = c.id "
    "LEFT JOIN media md ON md.id = r.photo_id "
    "LEFT JOIN LATERAL ("
    "    SELECT id, chat_id, sender_id, "
    "    (SELECT display_name FROM users su WHERE su.id = m.sender_id) AS sender_name, "
    "    translate(encode(ciphertext, 'base64'), E'\\n', '') AS ciphertext, "
    "    (extract(epoch FROM edited_at) * 1000)::bigint AS edited_at, "
    "    (extract(epoch FROM deleted_at) * 1000)::bigint AS deleted_at, "
    "    (extract(epoch FROM created_at) * 1000)::bigint AS created_at "
    "    FROM messages m WHERE m.chat_id = c.id ORDER BY m.id DESC LIMIT 1"
    ") lm ON TRUE ";

const char *const kPreviewSelect =
    "SELECT c.id, c.type, r.title, r.about, COALESCE(r.username, '') AS username, r.is_public, "
    "COALESCE(md.storage_key, '') AS photo, "
    "(SELECT count(*) FROM chat_members x WHERE x.chat_id = c.id AND x.status = 'active') AS member_count, "
    "COALESCE(me.status, '') AS status "
    "FROM rooms r JOIN chats c ON c.id = r.chat_id "
    "LEFT JOIN media md ON md.id = r.photo_id "
    "LEFT JOIN chat_members me ON me.chat_id = c.id AND me.user_id = $1 ";

std::string photoUrl(const drogon::orm::Row &row)
{
    const auto key = row["photo"].as<std::string>();
    return key.empty() ? std::string() : "/v1/media/" + key;
}

RoomSummary summaryFromRow(const drogon::orm::Row &row)
{
    RoomSummary room{
        row["id"].as<int64_t>(),
        row["type"].as<std::string>(),
        row["title"].as<std::string>(),
        row["about"].as<std::string>(),
        row["username"].as<std::string>(),
        row["is_public"].as<bool>(),
        photoUrl(row),
        row["invite_code"].as<std::string>(),
        row["members_can_send"].as<bool>(),
        row["members_can_add"].as<bool>(),
        row["status"].as<std::string>(),
        row["role"].as<std::string>(),
        row["last_read_message_id"].as<int64_t>(),
        row["read_up_to"].as<int64_t>(),
        row["unread_count"].as<int64_t>(),
        row["member_count"].as<int64_t>(),
        row["pinned_at"].isNull() ? std::nullopt : std::optional<int64_t>(row["pinned_at"].as<int64_t>()),
        row["muted"].as<bool>(),
        std::nullopt,
    };
    if (!row["lm_id"].isNull())
        room.lastMessage = messages::messageFromRow(row, "lm_");
    return room;
}

RoomPreview previewFromRow(const drogon::orm::Row &row)
{
    return RoomPreview{
        row["id"].as<int64_t>(),
        row["type"].as<std::string>(),
        row["title"].as<std::string>(),
        row["about"].as<std::string>(),
        row["username"].as<std::string>(),
        row["is_public"].as<bool>(),
        photoUrl(row),
        row["member_count"].as<int64_t>(),
        row["status"].as<std::string>(),
    };
}

std::vector<int64_t> idsFrom(const drogon::orm::Result &result, const char *column)
{
    std::vector<int64_t> ids;
    ids.reserve(result.size());
    for (const auto &row : result)
        ids.push_back(row[column].as<int64_t>());
    return ids;
}
}

drogon::Task<std::optional<RoomAccess>> RoomRepository::access(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT c.type, COALESCE(me.status, '') AS status, COALESCE(me.role, '') AS role, "
        "r.is_public, r.members_can_send, r.members_can_add "
        "FROM rooms r JOIN chats c ON c.id = r.chat_id "
        "LEFT JOIN chat_members me ON me.chat_id = r.chat_id AND me.user_id = $2 "
        "WHERE r.chat_id = $1",
        chatId,
        userId);
    if (result.empty())
        co_return std::nullopt;
    const auto &row = result[0];
    co_return RoomAccess{row["type"].as<std::string>(),
                         row["status"].as<std::string>(),
                         row["role"].as<std::string>(),
                         row["is_public"].as<bool>(),
                         row["members_can_send"].as<bool>(),
                         row["members_can_add"].as<bool>()};
}

drogon::Task<std::vector<RoomSummary>> RoomRepository::listForUser(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kSummarySelect) +
            "WHERE me.user_id = $1 ORDER BY me.pinned_at DESC NULLS LAST, COALESCE(lm.id, c.id) DESC",
        userId);
    std::vector<RoomSummary> rooms;
    rooms.reserve(result.size());
    for (const auto &row : result)
        rooms.push_back(summaryFromRow(row));
    co_return rooms;
}

drogon::Task<std::optional<RoomSummary>> RoomRepository::findForUser(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kSummarySelect) + "WHERE me.user_id = $1 AND me.chat_id = $2", userId, chatId);
    if (result.empty())
        co_return std::nullopt;
    co_return summaryFromRow(result[0]);
}

drogon::Task<bool> RoomRepository::create(int64_t chatId,
                                          std::string kind,
                                          RoomFields fields,
                                          std::string inviteCode,
                                          int64_t ownerId)
{
    try
    {
        co_await core::database::client()->execSqlCoro(
            "WITH chat AS (INSERT INTO chats (id, type) VALUES ($1, $2) RETURNING id), "
            "room AS (INSERT INTO rooms (chat_id, title, about, username, is_public, photo_id, invite_code, "
            "members_can_send, members_can_add) "
            "SELECT id, $3, $4, NULLIF($5::text, ''), $6, NULLIF($7::bigint, 0), $8, $9, $10 FROM chat RETURNING chat_id) "
            "INSERT INTO chat_members (chat_id, user_id, status, role) SELECT chat_id, $11, 'active', 'owner' FROM room",
            chatId,
            kind,
            fields.title,
            fields.about,
            fields.username,
            fields.isPublic,
            fields.photoId,
            inviteCode,
            fields.membersCanSend,
            fields.membersCanAdd,
            ownerId);
        co_return true;
    }
    catch (const drogon::orm::SqlError &error)
    {
        if (error.sqlState() == "23505")
            co_return false;
        throw;
    }
}

drogon::Task<bool> RoomRepository::update(int64_t chatId, RoomFields fields)
{
    try
    {
        co_await core::database::client()->execSqlCoro(
            "UPDATE rooms SET title = $2, about = $3, username = NULLIF($4::text, ''), is_public = $5, "
            "photo_id = NULLIF($6::bigint, 0), members_can_send = $7, members_can_add = $8 WHERE chat_id = $1",
            chatId,
            fields.title,
            fields.about,
            fields.username,
            fields.isPublic,
            fields.photoId,
            fields.membersCanSend,
            fields.membersCanAdd);
        co_return true;
    }
    catch (const drogon::orm::SqlError &error)
    {
        if (error.sqlState() == "23505")
            co_return false;
        throw;
    }
}

drogon::Task<> RoomRepository::setInviteCode(int64_t chatId, std::string code)
{
    co_await core::database::client()->execSqlCoro("UPDATE rooms SET invite_code = $2 WHERE chat_id = $1", chatId, code);
}

drogon::Task<int64_t> RoomRepository::photoOf(int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT COALESCE(photo_id, 0) AS photo_id FROM rooms WHERE chat_id = $1", chatId);
    co_return result.empty() ? 0 : result[0]["photo_id"].as<int64_t>();
}

drogon::Task<bool> RoomRepository::usernameFree(std::string username, int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT NOT EXISTS (SELECT 1 FROM users WHERE lower(username) = lower($1)) "
        "AND NOT EXISTS (SELECT 1 FROM rooms WHERE lower(username) = lower($1) AND chat_id <> $2) AS free",
        username,
        chatId);
    co_return result[0]["free"].as<bool>();
}

drogon::Task<bool> RoomRepository::ownsPhoto(int64_t photoId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT 1 FROM media WHERE id = $1 AND owner_id = $2", photoId, userId);
    co_return !result.empty();
}

drogon::Task<std::vector<RoomPreview>> RoomRepository::search(std::string query,
                                                              std::string kind,
                                                              int64_t userId,
                                                              int limit)
{
    std::string escaped;
    for (const char c : query)
    {
        if (c == '%' || c == '_' || c == '\\')
            escaped += '\\';
        escaped += c;
    }
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kPreviewSelect) +
            "WHERE r.is_public AND c.type = $2 AND (lower(r.username) LIKE '%' || lower($3) || '%' "
            "OR lower(r.title) LIKE '%' || lower($3) || '%') "
            "ORDER BY lower(r.username) = lower($5) DESC, member_count DESC, lower(r.title) LIMIT $4",
        userId,
        kind,
        escaped,
        static_cast<int64_t>(limit),
        query);
    std::vector<RoomPreview> rooms;
    rooms.reserve(result.size());
    for (const auto &row : result)
        rooms.push_back(previewFromRow(row));
    co_return rooms;
}

drogon::Task<std::optional<RoomPreview>> RoomRepository::findByKey(std::string key, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kPreviewSelect) + "WHERE r.invite_code = $2 OR (r.is_public AND lower(r.username) = lower($2))",
        userId,
        key);
    if (result.empty())
        co_return std::nullopt;
    co_return previewFromRow(result[0]);
}

drogon::Task<std::vector<RoomMember>> RoomRepository::members(int64_t chatId, int64_t limit, int64_t offset)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT u.id, u.username, u.display_name, m.role, m.status, u.disabled_at IS NOT NULL AS disabled, "
        "u.deleted_at IS NOT NULL AS deleted FROM chat_members m JOIN users u ON u.id = m.user_id "
        "WHERE m.chat_id = $1 ORDER BY m.status = 'active' DESC, "
        "CASE m.role WHEN 'owner' THEN 0 WHEN 'admin' THEN 1 ELSE 2 END, m.joined_at, u.id LIMIT $2 OFFSET $3",
        chatId,
        limit,
        offset);
    std::vector<RoomMember> members;
    members.reserve(result.size());
    for (const auto &row : result)
        members.push_back(RoomMember{row["id"].as<int64_t>(),
                                     row["username"].as<std::string>(),
                                     row["display_name"].as<std::string>(),
                                     row["role"].as<std::string>(),
                                     row["status"].as<std::string>(),
                                     row["disabled"].as<bool>(),
                                     row["deleted"].as<bool>()});
    co_return members;
}

drogon::Task<std::vector<int64_t>> RoomRepository::activeMemberIds(int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT user_id FROM chat_members WHERE chat_id = $1 AND status = 'active'", chatId);
    co_return idsFrom(result, "user_id");
}

drogon::Task<std::vector<int64_t>> RoomRepository::allMemberIds(int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT user_id FROM chat_members WHERE chat_id = $1", chatId);
    co_return idsFrom(result, "user_id");
}

drogon::Task<std::vector<int64_t>> RoomRepository::roomsOf(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT m.chat_id FROM chat_members m JOIN rooms r ON r.chat_id = m.chat_id WHERE m.user_id = $1", userId);
    co_return idsFrom(result, "chat_id");
}

drogon::Task<std::optional<std::string>> RoomRepository::memberRole(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT role FROM chat_members WHERE chat_id = $1 AND user_id = $2 AND status = 'active'", chatId, userId);
    if (result.empty())
        co_return std::nullopt;
    co_return result[0]["role"].as<std::string>();
}

drogon::Task<bool> RoomRepository::addMember(int64_t chatId, int64_t userId, std::string status)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "INSERT INTO chat_members (chat_id, user_id, status, last_read_message_id) "
        "VALUES ($1, $2, $3, COALESCE((SELECT max(id) FROM messages WHERE chat_id = $1), 0)) ON CONFLICT DO NOTHING",
        chatId,
        userId,
        status);
    co_return result.affectedRows() > 0;
}

drogon::Task<bool> RoomRepository::acceptInvite(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET status = 'active', joined_at = now(), "
        "last_read_message_id = COALESCE((SELECT max(id) FROM messages WHERE chat_id = $1), 0) "
        "WHERE chat_id = $1 AND user_id = $2 AND status = 'request'",
        chatId,
        userId);
    co_return result.affectedRows() > 0;
}

drogon::Task<> RoomRepository::removeMember(int64_t chatId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "DELETE FROM chat_members WHERE chat_id = $1 AND user_id = $2", chatId, userId);
}

drogon::Task<> RoomRepository::setRole(int64_t chatId, int64_t userId, std::string role)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET role = $3 WHERE chat_id = $1 AND user_id = $2 AND role <> 'owner'",
        chatId,
        userId,
        role);
}

drogon::Task<> RoomRepository::transfer(int64_t chatId, int64_t ownerId, int64_t userId)
{
    auto transaction = co_await core::database::client()->newTransactionCoro();
    co_await transaction->execSqlCoro(
        "UPDATE chat_members SET role = 'admin' WHERE chat_id = $1 AND user_id = $2", chatId, ownerId);
    co_await transaction->execSqlCoro(
        "UPDATE chat_members SET role = 'owner' WHERE chat_id = $1 AND user_id = $2", chatId, userId);
}

drogon::Task<> RoomRepository::ban(int64_t chatId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO room_bans (chat_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING", chatId, userId);
}

drogon::Task<> RoomRepository::unban(int64_t chatId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "DELETE FROM room_bans WHERE chat_id = $1 AND user_id = $2", chatId, userId);
}

drogon::Task<bool> RoomRepository::isBanned(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT 1 FROM room_bans WHERE chat_id = $1 AND user_id = $2", chatId, userId);
    co_return !result.empty();
}

drogon::Task<bool> RoomRepository::restrictedBy(int64_t userId, int64_t adderId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT 1 FROM dm_pairs p JOIN chat_members m ON m.chat_id = p.chat_id AND m.user_id = $1 "
        "WHERE p.user_low = LEAST($1::bigint, $2::bigint) AND p.user_high = GREATEST($1::bigint, $2::bigint) "
        "AND m.status = 'restricted'",
        userId,
        adderId);
    co_return !result.empty();
}

drogon::Task<std::optional<int64_t>> RoomRepository::messageSender(int64_t chatId, int64_t messageId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT sender_id FROM messages WHERE chat_id = $1 AND id = $2", chatId, messageId);
    if (result.empty())
        co_return std::nullopt;
    co_return result[0]["sender_id"].as<int64_t>();
}

drogon::Task<int64_t> RoomRepository::onlineCount(int64_t chatId)
{
    const auto ids = co_await activeMemberIds(chatId);
    int64_t online = 0;
    for (const auto id : ids)
        if (realtime::hub().isOnline(id))
            ++online;
    co_return online;
}
}
