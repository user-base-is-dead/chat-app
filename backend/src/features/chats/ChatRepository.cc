#include "features/chats/ChatRepository.h"

#include <algorithm>

#include "core/database/Database.h"
#include "core/http/ApiError.h"

namespace features::chats
{
namespace
{
const char *const kSummarySelect =
    "SELECT me.chat_id, me.status, me.last_read_message_id, me.muted, "
    "(extract(epoch FROM me.pinned_at) * 1000)::bigint AS pinned_at, "
    "peer.last_read_message_id AS peer_last_read, peer.status AS peer_status, "
    "u.id AS peer_id, u.username AS peer_username, u.display_name AS peer_display_name, u.bio AS peer_bio, "
    "(extract(epoch FROM u.last_seen_at) * 1000)::bigint AS peer_last_seen, u.disabled_at IS NOT NULL AS peer_disabled, u.deleted_at IS NOT NULL AS peer_deleted, "
    "(SELECT count(*) FROM messages um WHERE um.chat_id = me.chat_id AND um.id > me.last_read_message_id "
    "AND um.sender_id <> me.user_id) AS unread_count, "
    "lm.id AS lm_id, lm.chat_id AS lm_chat_id, lm.sender_id AS lm_sender_id, lm.ciphertext AS lm_ciphertext, "
    "lm.edited_at AS lm_edited_at, lm.deleted_at AS lm_deleted_at, lm.created_at AS lm_created_at "
    "FROM chat_members me "
    "JOIN dm_pairs p ON p.chat_id = me.chat_id "
    "JOIN users u ON u.id = CASE WHEN p.user_low = me.user_id THEN p.user_high ELSE p.user_low END "
    "LEFT JOIN chat_members peer ON peer.chat_id = me.chat_id AND peer.user_id = u.id "
    "LEFT JOIN LATERAL ("
    "    SELECT id, chat_id, sender_id, "
    "    translate(encode(ciphertext, 'base64'), E'\\n', '') AS ciphertext, "
    "    (extract(epoch FROM edited_at) * 1000)::bigint AS edited_at, "
    "    (extract(epoch FROM deleted_at) * 1000)::bigint AS deleted_at, "
    "    (extract(epoch FROM created_at) * 1000)::bigint AS created_at "
    "    FROM messages m WHERE m.chat_id = me.chat_id ORDER BY m.id DESC LIMIT 1"
    ") lm ON TRUE ";

ChatSummary summaryFromRow(const drogon::orm::Row &row)
{
    ChatSummary summary{
        row["chat_id"].as<int64_t>(),
        row["status"].as<std::string>(),
        users::User{row["peer_id"].as<int64_t>(),
                    row["peer_username"].as<std::string>(),
                    row["peer_display_name"].as<std::string>(),
                    row["peer_bio"].as<std::string>()},
        row["last_read_message_id"].as<int64_t>(),
        row["peer_last_read"].isNull() ? 0 : row["peer_last_read"].as<int64_t>(),
        std::nullopt,
        row["peer_last_seen"].isNull() ? std::nullopt : std::optional<int64_t>(row["peer_last_seen"].as<int64_t>()),
        !row["peer_status"].isNull() && row["peer_status"].as<std::string>() == "active",
        row["unread_count"].as<int64_t>(),
        row["pinned_at"].isNull() ? std::nullopt : std::optional<int64_t>(row["pinned_at"].as<int64_t>()),
        row["muted"].as<bool>(),
        row["peer_disabled"].as<bool>(),
        row["peer_deleted"].as<bool>(),
    };
    if (!summary.peerVisible)
        summary.peerLastReadId = 0;
    if (!summary.peerVisible || summary.peerDisabled || summary.peerDeleted)
        summary.peerLastSeenAt = std::nullopt;
    if (!row["lm_id"].isNull())
        summary.lastMessage = messages::messageFromRow(row, "lm_");
    return summary;
}
}

drogon::Task<std::vector<ChatSummary>> ChatRepository::listForUser(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kSummarySelect) + "WHERE me.user_id = $1 ORDER BY me.pinned_at DESC NULLS LAST, COALESCE(lm.id, me.chat_id) DESC", userId);

    std::vector<ChatSummary> chats;
    chats.reserve(result.size());
    for (const auto &row : result)
        chats.push_back(summaryFromRow(row));
    co_return chats;
}

drogon::Task<std::optional<ChatSummary>> ChatRepository::findForUser(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kSummarySelect) + "WHERE me.user_id = $1 AND me.chat_id = $2", userId, chatId);
    if (result.empty())
        co_return std::nullopt;
    co_return summaryFromRow(result[0]);
}

drogon::Task<std::optional<int64_t>> ChatRepository::findDm(int64_t userA, int64_t userB)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT chat_id FROM dm_pairs WHERE user_low = $1 AND user_high = $2",
        std::min(userA, userB),
        std::max(userA, userB));
    if (result.empty())
        co_return std::nullopt;
    co_return result[0]["chat_id"].as<int64_t>();
}

drogon::Task<int64_t> ChatRepository::createDm(int64_t chatId, int64_t creatorId, int64_t peerId)
{
    const auto created = co_await core::database::client()->execSqlCoro(
        "WITH chat AS (INSERT INTO chats (id, type) VALUES ($1, 'dm') RETURNING id), "
        "pair AS (INSERT INTO dm_pairs (user_low, user_high, chat_id) SELECT $3, $4, id FROM chat "
        "ON CONFLICT (user_low, user_high) DO NOTHING RETURNING chat_id) "
        "INSERT INTO chat_members (chat_id, user_id, status) SELECT chat_id, $2, 'active' FROM pair RETURNING chat_id",
        chatId,
        creatorId,
        std::min(creatorId, peerId),
        std::max(creatorId, peerId));
    if (!created.empty())
        co_return chatId;

    co_await core::database::client()->execSqlCoro("DELETE FROM chats WHERE id = $1", chatId);
    auto existing = co_await findDm(creatorId, peerId);
    if (!existing)
        throw core::http::ApiError(drogon::k409Conflict, "chat_busy", "Could not open the chat, try again");
    co_await ensureMember(*existing, creatorId, "active");
    co_return *existing;
}

drogon::Task<> ChatRepository::ensureMember(int64_t chatId, int64_t userId, std::string status)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO chat_members (chat_id, user_id, status) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING",
        chatId,
        userId,
        status);
}

drogon::Task<> ChatRepository::addRecipient(int64_t chatId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO chat_members (chat_id, user_id, status) VALUES ($1, $2, 'request') "
        "ON CONFLICT (chat_id, user_id) DO UPDATE SET status = 'request' "
        "WHERE NOT EXISTS (SELECT 1 FROM messages WHERE chat_id = $1)",
        chatId,
        userId);
}

drogon::Task<std::optional<Membership>> ChatRepository::membership(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT me.status, CASE WHEN p.user_low = $2 THEN p.user_high ELSE p.user_low END AS peer_id "
        "FROM chat_members me JOIN dm_pairs p ON p.chat_id = me.chat_id "
        "WHERE me.chat_id = $1 AND me.user_id = $2",
        chatId,
        userId);
    if (result.empty())
        co_return std::nullopt;
    co_return Membership{result[0]["status"].as<std::string>(), result[0]["peer_id"].as<int64_t>()};
}

drogon::Task<bool> ChatRepository::accept(int64_t chatId, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET status = 'active' WHERE chat_id = $1 AND user_id = $2 AND status = 'request'",
        chatId,
        userId);
    co_return result.affectedRows() > 0;
}

drogon::Task<bool> ChatRepository::changeStatus(int64_t chatId, int64_t userId, std::string from, std::string to)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET status = $4 WHERE chat_id = $1 AND user_id = $2 AND status = $3",
        chatId,
        userId,
        from,
        to);
    co_return result.affectedRows() > 0;
}

drogon::Task<bool> ChatRepository::pin(int64_t chatId, int64_t userId, int64_t limit)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET pinned_at = COALESCE(pinned_at, now()) "
        "WHERE chat_id = $1 AND user_id = $2 AND status = 'active' AND (pinned_at IS NOT NULL OR "
        "(SELECT count(*) FROM chat_members WHERE user_id = $2 AND status = 'active' AND pinned_at IS NOT NULL) < $3)",
        chatId,
        userId,
        limit);
    co_return result.affectedRows() > 0;
}

drogon::Task<> ChatRepository::unpin(int64_t chatId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET pinned_at = NULL WHERE chat_id = $1 AND user_id = $2", chatId, userId);
}

drogon::Task<> ChatRepository::setMuted(int64_t chatId, int64_t userId, bool muted)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET muted = $3 WHERE chat_id = $1 AND user_id = $2", chatId, userId, muted);
}

drogon::Task<> ChatRepository::remove(int64_t chatId)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM chats WHERE id = $1", chatId);
}

drogon::Task<bool> ChatRepository::removeIfEmpty(int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "DELETE FROM chats WHERE id = $1 AND NOT EXISTS (SELECT 1 FROM messages WHERE chat_id = $1)", chatId);
    co_return result.affectedRows() > 0;
}

drogon::Task<std::vector<int64_t>> ChatRepository::peerIds(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT CASE WHEN p.user_low = $1 THEN p.user_high ELSE p.user_low END AS peer_id "
        "FROM dm_pairs p JOIN chat_members me ON me.chat_id = p.chat_id AND me.user_id = $1 "
        "WHERE me.status = 'active'",
        userId);
    std::vector<int64_t> ids;
    ids.reserve(result.size());
    for (const auto &row : result)
        ids.push_back(row["peer_id"].as<int64_t>());
    co_return ids;
}

drogon::Task<std::vector<int64_t>> ChatRepository::allPeerIds(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT CASE WHEN user_low = $1 THEN user_high ELSE user_low END AS peer_id "
        "FROM dm_pairs WHERE user_low = $1 OR user_high = $1",
        userId);
    std::vector<int64_t> ids;
    ids.reserve(result.size());
    for (const auto &row : result)
        ids.push_back(row["peer_id"].as<int64_t>());
    co_return ids;
}

drogon::Task<std::vector<DmLink>> ChatRepository::dmsOf(int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT chat_id, CASE WHEN user_low = $1 THEN user_high ELSE user_low END AS peer_id "
        "FROM dm_pairs WHERE user_low = $1 OR user_high = $1",
        userId);
    std::vector<DmLink> links;
    links.reserve(result.size());
    for (const auto &row : result)
        links.push_back(DmLink{row["chat_id"].as<int64_t>(), row["peer_id"].as<int64_t>()});
    co_return links;
}

drogon::Task<> ChatRepository::markRead(int64_t chatId, int64_t userId, int64_t messageId)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE chat_members SET last_read_message_id = GREATEST(last_read_message_id, $3) "
        "WHERE chat_id = $1 AND user_id = $2",
        chatId,
        userId,
        messageId);
}
}
