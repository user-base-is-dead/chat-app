#include "features/posts/PostRepository.h"

#include <algorithm>
#include <sstream>

#include <json/json.h>

#include "core/database/Database.h"

namespace features::posts
{
namespace
{
const char *const kCommentSelect =
    "SELECT c.id, c.message_id, c.user_id, c.body, "
    "(extract(epoch FROM c.created_at) * 1000)::bigint AS created_at, "
    "u.display_name, (u.deleted_at IS NOT NULL) AS deleted "
    "FROM post_comments c JOIN users u ON u.id = c.user_id ";

const char *const kStatsSelect =
    "SELECT m.id, m.view_count, m.comment_count, m.allow_reactions, m.allow_comments, "
    "COALESCE((SELECT r.emoji FROM post_reactions r "
    "WHERE r.chat_id = m.chat_id AND r.message_id = m.id AND r.user_id = $3), '') AS my_reaction, "
    "COALESCE((SELECT json_agg(json_build_object('emoji', g.emoji, 'count', g.n) ORDER BY g.n DESC, g.emoji) "
    "FROM (SELECT emoji, count(*) AS n FROM post_reactions "
    "WHERE chat_id = m.chat_id AND message_id = m.id GROUP BY emoji) g), '[]')::text AS reactions "
    "FROM messages m WHERE m.chat_id = $1 AND m.id = ANY($2::bigint[])";

std::string arrayOf(const std::vector<int64_t> &ids)
{
    std::string text = "{";
    for (size_t i = 0; i < ids.size(); ++i)
    {
        if (i > 0)
            text += ",";
        text += std::to_string(ids[i]);
    }
    return text + "}";
}

std::vector<ReactionCount> parseReactions(const std::string &text)
{
    std::vector<ReactionCount> reactions;
    Json::Value json;
    Json::CharReaderBuilder builder;
    std::istringstream stream(text);
    std::string errors;
    if (!Json::parseFromStream(builder, stream, &json, &errors) || !json.isArray())
        return reactions;
    for (const auto &item : json)
        reactions.push_back(ReactionCount{item["emoji"].asString(), item["count"].asInt64()});
    return reactions;
}

PostStats statsFromRow(const drogon::orm::Row &row)
{
    PostStats stats{row["id"].as<int64_t>(),
                    row["view_count"].as<int64_t>(),
                    row["comment_count"].as<int64_t>(),
                    row["allow_reactions"].as<bool>(),
                    row["allow_comments"].as<bool>(),
                    parseReactions(row["reactions"].as<std::string>()),
                    row["my_reaction"].as<std::string>()};
    if (!stats.allowReactions)
    {
        stats.reactions.clear();
        stats.myReaction.clear();
    }
    if (!stats.allowComments)
        stats.comments = 0;
    return stats;
}

Comment commentFromRow(const drogon::orm::Row &row)
{
    const bool deleted = row["deleted"].as<bool>();
    return Comment{row["id"].as<int64_t>(),
                   row["message_id"].as<int64_t>(),
                   row["user_id"].as<int64_t>(),
                   deleted ? std::string() : row["display_name"].as<std::string>(),
                   deleted,
                   row["body"].as<std::string>(),
                   row["created_at"].as<int64_t>()};
}
}

drogon::Task<> PostRepository::markViewed(int64_t chatId, std::vector<int64_t> ids, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "WITH added AS ("
        "INSERT INTO post_views (chat_id, message_id, user_id) "
        "SELECT $1, m.id, $3 FROM messages m WHERE m.chat_id = $1 AND m.id = ANY($2::bigint[]) "
        "ON CONFLICT DO NOTHING RETURNING message_id) "
        "UPDATE messages SET view_count = view_count + 1 "
        "WHERE chat_id = $1 AND id IN (SELECT message_id FROM added)",
        chatId,
        arrayOf(ids),
        userId);
}

drogon::Task<std::vector<PostStats>> PostRepository::stats(int64_t chatId, std::vector<int64_t> ids, int64_t userId)
{
    const auto result = co_await core::database::client()->execSqlCoro(kStatsSelect, chatId, arrayOf(ids), userId);
    std::vector<PostStats> list;
    list.reserve(result.size());
    for (const auto &row : result)
        list.push_back(statsFromRow(row));
    co_return list;
}

drogon::Task<std::optional<PostStats>> PostRepository::one(int64_t chatId, int64_t messageId, int64_t userId)
{
    auto list = co_await stats(chatId, {messageId}, userId);
    if (list.empty())
        co_return std::nullopt;
    co_return list[0];
}

drogon::Task<> PostRepository::setReaction(int64_t chatId, int64_t messageId, int64_t userId, std::string emoji)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO post_reactions (chat_id, message_id, user_id, emoji) VALUES ($1, $2, $3, $4) "
        "ON CONFLICT (chat_id, message_id, user_id) DO UPDATE SET emoji = EXCLUDED.emoji",
        chatId,
        messageId,
        userId,
        emoji);
}

drogon::Task<> PostRepository::clearReaction(int64_t chatId, int64_t messageId, int64_t userId)
{
    co_await core::database::client()->execSqlCoro(
        "DELETE FROM post_reactions WHERE chat_id = $1 AND message_id = $2 AND user_id = $3",
        chatId,
        messageId,
        userId);
}

drogon::Task<std::vector<Comment>> PostRepository::comments(int64_t chatId, int64_t messageId, int64_t beforeId, int limit)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        std::string(kCommentSelect) +
            "WHERE c.chat_id = $1 AND c.message_id = $2 AND ($3::bigint = 0 OR c.id < $3::bigint) "
            "ORDER BY c.id DESC LIMIT $4",
        chatId,
        messageId,
        beforeId,
        static_cast<int64_t>(limit));
    std::vector<Comment> list;
    list.reserve(result.size());
    for (const auto &row : result)
        list.push_back(commentFromRow(row));
    std::reverse(list.begin(), list.end());
    co_return list;
}

drogon::Task<Comment> PostRepository::addComment(int64_t id, int64_t chatId, int64_t messageId, int64_t userId, std::string body)
{
    co_await core::database::client()->execSqlCoro(
        "WITH added AS ("
        "INSERT INTO post_comments (id, chat_id, message_id, user_id, body) VALUES ($1, $2, $3, $4, $5) RETURNING id) "
        "UPDATE messages SET comment_count = comment_count + 1 "
        "WHERE chat_id = $2 AND id = $3 AND EXISTS (SELECT 1 FROM added)",
        id,
        chatId,
        messageId,
        userId,
        body);
    const auto result =
        co_await core::database::client()->execSqlCoro(std::string(kCommentSelect) + "WHERE c.id = $1", id);
    co_return commentFromRow(result[0]);
}

drogon::Task<std::optional<int64_t>> PostRepository::commentAuthor(int64_t chatId, int64_t messageId, int64_t commentId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT user_id FROM post_comments WHERE chat_id = $1 AND message_id = $2 AND id = $3",
        chatId,
        messageId,
        commentId);
    if (result.empty())
        co_return std::nullopt;
    co_return result[0]["user_id"].as<int64_t>();
}

drogon::Task<int64_t> PostRepository::removeComment(int64_t chatId, int64_t messageId, int64_t commentId)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "WITH gone AS ("
        "DELETE FROM post_comments WHERE chat_id = $1 AND message_id = $2 AND id = $3 RETURNING id) "
        "UPDATE messages SET comment_count = comment_count - 1 "
        "WHERE chat_id = $1 AND id = $2 AND EXISTS (SELECT 1 FROM gone) RETURNING comment_count",
        chatId,
        messageId,
        commentId);
    if (result.empty())
        co_return -1;
    co_return result[0]["comment_count"].as<int64_t>();
}

drogon::Task<> PostRepository::setSwitches(int64_t chatId, int64_t messageId, bool allowReactions, bool allowComments)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE messages SET allow_reactions = $3, allow_comments = $4 WHERE chat_id = $1 AND id = $2",
        chatId,
        messageId,
        allowReactions,
        allowComments);
}
}
