#include "features/channels/ChannelRepository.h"

#include "core/database/Database.h"
#include "features/rooms/Authority.h"

namespace features::channels
{
namespace
{
const char *const kChannelSelect =
    "SELECT rc.chat_id AS id, rc.name, rc.category_id, rc.position, "
    "COALESCE(CASE WHEN rc.chat_id = rc.room_id THEN me.last_read_message_id ELSE cr.last_read_message_id END, 0) "
    "AS last_read_id, "
    "(SELECT count(*) FROM messages um WHERE um.chat_id = rc.chat_id AND um.sender_id <> $2 AND um.id > "
    "COALESCE(CASE WHEN rc.chat_id = rc.room_id THEN me.last_read_message_id ELSE cr.last_read_message_id END, 0)) "
    "AS unread_count, "
    "CASE WHEN rc.chat_id = rc.room_id THEN "
    "(SELECT COALESCE(max(x.last_read_message_id), 0) FROM chat_members x "
    "WHERE x.chat_id = rc.room_id AND x.user_id <> $2 AND x.status = 'active') "
    "ELSE (SELECT COALESCE(max(x.last_read_message_id), 0) FROM channel_reads x "
    "WHERE x.chat_id = rc.chat_id AND x.user_id <> $2) END AS read_up_to, "
    "lm.id AS lm_id, lm.chat_id AS lm_chat_id, lm.sender_id AS lm_sender_id, lm.sender_name AS lm_sender_name, "
    "lm.ciphertext AS lm_ciphertext, lm.edited_at AS lm_edited_at, lm.deleted_at AS lm_deleted_at, "
    "lm.created_at AS lm_created_at, "
    "array_to_string(COALESCE(channel_permissions(rc.chat_id, $2), '{}'), ',') AS permissions, rc.synced, "
    "EXISTS (SELECT 1 FROM channel_overrides o WHERE o.scope_id = channel_scope(rc.chat_id) "
    "AND o.target_type = 'everyone' AND 'viewChannel' = ANY (o.deny)) AS is_private "
    "FROM room_channels rc "
    "LEFT JOIN chat_members me ON me.chat_id = rc.room_id AND me.user_id = $2 "
    "LEFT JOIN channel_reads cr ON cr.chat_id = rc.chat_id AND cr.user_id = $2 "
    "LEFT JOIN LATERAL ("
    "    SELECT id, chat_id, sender_id, "
    "    (SELECT display_name FROM users su WHERE su.id = m.sender_id) AS sender_name, "
    "    translate(encode(ciphertext, 'base64'), E'\\n', '') AS ciphertext, "
    "    (extract(epoch FROM edited_at) * 1000)::bigint AS edited_at, "
    "    (extract(epoch FROM deleted_at) * 1000)::bigint AS deleted_at, "
    "    (extract(epoch FROM created_at) * 1000)::bigint AS created_at "
    "    FROM messages m WHERE m.chat_id = rc.chat_id ORDER BY m.id DESC LIMIT 1"
    ") lm ON TRUE "
    "WHERE rc.room_id = $1 ";

Channel channelFromRow(const drogon::orm::Row &row)
{
    Channel channel{row["id"].as<int64_t>(),
                    row["name"].as<std::string>(),
                    row["category_id"].isNull() ? std::nullopt : std::optional<int64_t>(row["category_id"].as<int64_t>()),
                    row["position"].as<int>(),
                    row["unread_count"].as<int64_t>(),
                    row["last_read_id"].as<int64_t>(),
                    row["read_up_to"].as<int64_t>(),
                    std::nullopt,
                    rooms::splitKeys(row["permissions"].as<std::string>()),
                    row["is_private"].as<bool>(),
                    row["synced"].as<bool>()};
    if (!row["lm_id"].isNull())
        channel.lastMessage = messages::messageFromRow(row, "lm_");
    return channel;
}

Category categoryFromRow(const drogon::orm::Row &row)
{
    return Category{row["id"].as<int64_t>(),
                    row["name"].as<std::string>(),
                    row["position"].as<int>(),
                    rooms::splitKeys(row["permissions"].as<std::string>())};
}
}

drogon::Task<std::vector<Category>> ChannelRepository::categories(int64_t roomId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT id, name, position, array_to_string(COALESCE(scope_permissions(room_id, id, $2), '{}'), ',') AS permissions "
        "FROM room_categories WHERE room_id = $1 ORDER BY position, id",
        roomId,
        userId);
    std::vector<Category> list;
    list.reserve(result.size());
    for (const auto &row : result)
        list.push_back(categoryFromRow(row));
    co_return list;
}

drogon::Task<std::vector<Channel>> ChannelRepository::channels(int64_t roomId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kChannelSelect) + "AND can_view_channel(rc.chat_id, $2) ORDER BY rc.position, rc.chat_id", roomId, userId);
    std::vector<Channel> list;
    list.reserve(result.size());
    for (const auto &row : result)
        list.push_back(channelFromRow(row));
    co_return list;
}

drogon::Task<std::optional<Channel>> ChannelRepository::channel(int64_t roomId, int64_t channelId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kChannelSelect) + "AND rc.chat_id = $3", roomId, userId, channelId);
    if (result.empty())
        co_return std::nullopt;
    co_return channelFromRow(result[0]);
}

drogon::Task<std::optional<Category>> ChannelRepository::category(int64_t roomId, int64_t categoryId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT id, name, position, array_to_string(COALESCE(scope_permissions(room_id, id, $3), '{}'), ',') AS permissions "
        "FROM room_categories WHERE room_id = $1 AND id = $2",
        roomId,
        categoryId,
        userId);
    if (result.empty())
        co_return std::nullopt;
    co_return categoryFromRow(result[0]);
}

drogon::Task<int64_t> ChannelRepository::channelCount(int64_t roomId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT count(*) AS n FROM room_channels WHERE room_id = $1", roomId);
    co_return result[0]["n"].as<int64_t>();
}

drogon::Task<int64_t> ChannelRepository::categoryCount(int64_t roomId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT count(*) AS n FROM room_categories WHERE room_id = $1", roomId);
    co_return result[0]["n"].as<int64_t>();
}

drogon::Task<> ChannelRepository::createChannel(int64_t id, int64_t roomId, int64_t categoryId, std::string name)
{
    co_await core::database::client()->execSqlCoro(
        "WITH chat AS (INSERT INTO chats (id, type) VALUES ($1, 'text') RETURNING id) "
        "INSERT INTO room_channels (chat_id, room_id, category_id, name, position) "
        "SELECT id, $2, NULLIF($3::bigint, 0), $4, "
        "COALESCE((SELECT max(position) FROM room_channels WHERE room_id = $2), -1) + 1 FROM chat",
        id,
        roomId,
        categoryId,
        name);
}

drogon::Task<> ChannelRepository::renameChannel(int64_t channelId, std::string name)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_channels SET name = $2 WHERE chat_id = $1", channelId, name);
}

drogon::Task<> ChannelRepository::setCategory(int64_t roomId, int64_t channelId, int64_t categoryId)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_channels SET category_id = NULLIF($3::bigint, 0), "
        "position = COALESCE((SELECT max(position) FROM room_channels WHERE room_id = $1), -1) + 1 "
        "WHERE room_id = $1 AND chat_id = $2",
        roomId,
        channelId,
        categoryId);
}

drogon::Task<> ChannelRepository::moveChannel(int64_t roomId, int64_t channelId, bool up)
{
    co_await core::database::client()->execSqlCoro(
        "WITH cur AS (SELECT chat_id, position, category_id FROM room_channels WHERE room_id = $1 AND chat_id = $2), "
        "nb AS (SELECT rc.chat_id, rc.position FROM room_channels rc, cur "
        "WHERE rc.room_id = $1 AND rc.category_id IS NOT DISTINCT FROM cur.category_id "
        "AND CASE WHEN $3 THEN rc.position < cur.position ELSE rc.position > cur.position END "
        "ORDER BY CASE WHEN $3 THEN -rc.position ELSE rc.position END LIMIT 1) "
        "UPDATE room_channels t SET position = CASE WHEN t.chat_id = cur.chat_id THEN nb.position ELSE cur.position END "
        "FROM cur, nb WHERE t.chat_id IN (cur.chat_id, nb.chat_id)",
        roomId,
        channelId,
        up);
}

drogon::Task<> ChannelRepository::removeChannel(int64_t channelId)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM channel_overrides WHERE scope_id = $1", channelId);
    co_await core::database::client()->execSqlCoro("DELETE FROM chats WHERE id = $1 AND type = 'text'", channelId);
}

drogon::Task<> ChannelRepository::createCategory(int64_t id, int64_t roomId, std::string name)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO room_categories (id, room_id, name, position) VALUES ($1, $2, $3, "
        "COALESCE((SELECT max(position) FROM room_categories WHERE room_id = $2), -1) + 1)",
        id,
        roomId,
        name);
}

drogon::Task<> ChannelRepository::renameCategory(int64_t categoryId, std::string name)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_categories SET name = $2 WHERE id = $1", categoryId, name);
}

drogon::Task<> ChannelRepository::moveCategory(int64_t roomId, int64_t categoryId, bool up)
{
    co_await core::database::client()->execSqlCoro(
        "WITH cur AS (SELECT id, position FROM room_categories WHERE room_id = $1 AND id = $2), "
        "nb AS (SELECT rc.id, rc.position FROM room_categories rc, cur WHERE rc.room_id = $1 "
        "AND CASE WHEN $3 THEN rc.position < cur.position ELSE rc.position > cur.position END "
        "ORDER BY CASE WHEN $3 THEN -rc.position ELSE rc.position END LIMIT 1) "
        "UPDATE room_categories t SET position = CASE WHEN t.id = cur.id THEN nb.position ELSE cur.position END "
        "FROM cur, nb WHERE t.id IN (cur.id, nb.id)",
        roomId,
        categoryId,
        up);
}

drogon::Task<> ChannelRepository::removeCategory(int64_t categoryId)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM channel_overrides WHERE scope_id = $1", categoryId);
    co_await core::database::client()->execSqlCoro("DELETE FROM room_categories WHERE id = $1", categoryId);
}

namespace
{
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

drogon::Task<std::vector<Override>> ChannelRepository::overrides(int64_t scopeId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT target_type, target_id, array_to_string(allow, ',') AS allow, array_to_string(deny, ',') AS deny "
        "FROM channel_overrides WHERE scope_id = $1 "
        "ORDER BY CASE target_type WHEN 'everyone' THEN 0 WHEN 'role' THEN 1 ELSE 2 END, target_id",
        scopeId);
    std::vector<Override> list;
    list.reserve(result.size());
    for (const auto &row : result)
        list.push_back(Override{row["target_type"].as<std::string>(),
                                row["target_id"].as<int64_t>(),
                                rooms::splitKeys(row["allow"].as<std::string>()),
                                rooms::splitKeys(row["deny"].as<std::string>())});
    co_return list;
}

drogon::Task<> ChannelRepository::setOverride(int64_t scopeId, int64_t roomId, Override item)
{
    if (item.allow.empty() && item.deny.empty())
    {
        co_await core::database::client()->execSqlCoro(
            "DELETE FROM channel_overrides WHERE scope_id = $1 AND target_type = $2 AND target_id = $3",
            scopeId,
            item.targetType,
            item.targetId);
        co_return;
    }
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO channel_overrides (scope_id, room_id, target_type, target_id, allow, deny) "
        "VALUES ($1, $2, $3, $4, $5::text[], $6::text[]) "
        "ON CONFLICT (scope_id, target_type, target_id) DO UPDATE SET allow = EXCLUDED.allow, deny = EXCLUDED.deny",
        scopeId,
        roomId,
        item.targetType,
        item.targetId,
        arrayLiteral(item.allow),
        arrayLiteral(item.deny));
}

drogon::Task<> ChannelRepository::copyOverrides(int64_t fromScope, int64_t toScope, int64_t roomId)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO channel_overrides (scope_id, room_id, target_type, target_id, allow, deny) "
        "SELECT $2, $3, target_type, target_id, allow, deny FROM channel_overrides WHERE scope_id = $1 "
        "ON CONFLICT (scope_id, target_type, target_id) DO UPDATE SET allow = EXCLUDED.allow, deny = EXCLUDED.deny",
        fromScope,
        toScope,
        roomId);
}

drogon::Task<> ChannelRepository::clearOverrides(int64_t scopeId)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM channel_overrides WHERE scope_id = $1", scopeId);
}

drogon::Task<> ChannelRepository::setSynced(int64_t channelId, bool synced)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE room_channels SET synced = $2 WHERE chat_id = $1", channelId, synced);
}
}
