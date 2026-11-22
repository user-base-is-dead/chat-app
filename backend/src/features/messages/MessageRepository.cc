#include "features/messages/MessageRepository.h"

#include <algorithm>

#include "core/database/Database.h"

namespace features::messages
{
drogon::Task<std::vector<Message>> MessageRepository::list(int64_t chatId, int64_t beforeId, int limit)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kMessageColumns +
            " FROM messages WHERE chat_id = $1 AND ($2::bigint = 0 OR id < $2::bigint) ORDER BY id DESC LIMIT $3",
        chatId,
        beforeId,
        static_cast<int64_t>(limit));

    std::vector<Message> messages;
    messages.reserve(result.size());
    for (const auto &row : result)
        messages.push_back(fullMessageFromRow(row));
    std::reverse(messages.begin(), messages.end());
    co_return messages;
}

drogon::Task<std::vector<Message>> MessageRepository::listAfter(int64_t chatId, int64_t afterId, int limit)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kMessageColumns + " FROM messages WHERE chat_id = $1 AND id > $2 ORDER BY id ASC LIMIT $3",
        chatId,
        afterId,
        static_cast<int64_t>(limit));

    std::vector<Message> messages;
    messages.reserve(result.size());
    for (const auto &row : result)
        messages.push_back(fullMessageFromRow(row));
    co_return messages;
}

drogon::Task<Message> MessageRepository::insert(int64_t id,
                                                int64_t chatId,
                                                int64_t senderId,
                                                int64_t deviceId,
                                                std::string ciphertext,
                                                bool allowReactions,
                                                bool allowComments,
                                                Extras extras)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("INSERT INTO messages (id, chat_id, sender_id, sender_device_id, ciphertext, allow_reactions, allow_comments, "
                    "reply_to_id, forwarded_from_id, forwarded_from_chat_id) "
                    "VALUES ($1, $2, $3, $4, decode($5, 'base64'), $6, $7, NULLIF($8::bigint, 0), NULLIF($9::bigint, 0), "
                    "NULLIF($10::bigint, 0)) RETURNING ") +
            kMessageColumns,
        id,
        chatId,
        senderId,
        deviceId,
        ciphertext,
        allowReactions,
        allowComments,
        extras.replyToId,
        extras.forwardedFromId,
        extras.forwardedFromChatId);
    co_return fullMessageFromRow(result[0]);
}

drogon::Task<std::optional<Message>> MessageRepository::edit(int64_t chatId,
                                                             int64_t id,
                                                             int64_t senderId,
                                                             std::string ciphertext)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("UPDATE messages SET ciphertext = decode($4, 'base64'), edited_at = now() "
                    "WHERE chat_id = $1 AND id = $2 AND sender_id = $3 AND deleted_at IS NULL RETURNING ") +
            kMessageColumns,
        chatId,
        id,
        senderId,
        ciphertext);
    if (result.empty())
        co_return std::nullopt;
    co_return fullMessageFromRow(result[0]);
}

drogon::Task<std::optional<Message>> MessageRepository::find(int64_t chatId, int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kMessageColumns + " FROM messages WHERE chat_id = $1 AND id = $2", chatId, id);
    if (result.empty())
        co_return std::nullopt;
    co_return fullMessageFromRow(result[0]);
}

drogon::Task<bool> MessageRepository::exists(int64_t chatId, int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT 1 FROM messages WHERE chat_id = $1 AND id = $2", chatId, id);
    co_return !result.empty();
}

drogon::Task<std::optional<Message>> MessageRepository::pinned(int64_t chatId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string("SELECT ") + kMessageColumns +
            " FROM messages WHERE chat_id = $1 AND id = (SELECT pinned_message_id FROM chats WHERE id = $1)",
        chatId);
    if (result.empty())
        co_return std::nullopt;
    co_return fullMessageFromRow(result[0]);
}

drogon::Task<> MessageRepository::setPinned(int64_t chatId, int64_t messageId)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE chats SET pinned_message_id = NULLIF($2::bigint, 0) WHERE id = $1", chatId, messageId);
}

drogon::Task<bool> MessageRepository::remove(int64_t chatId, int64_t id)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "DELETE FROM messages WHERE chat_id = $1 AND id = $2", chatId, id);
    co_return result.affectedRows() > 0;
}
}
