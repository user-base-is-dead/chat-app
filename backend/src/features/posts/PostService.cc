#include "features/posts/PostService.h"

#include <algorithm>
#include <array>

#include "core/http/ApiError.h"
#include "core/id/Snowflake.h"
#include "core/text/Text.h"
#include "features/realtime/Hub.h"

namespace features::posts
{
namespace
{
using core::http::ApiError;

constexpr size_t kMaxViewed = 100;
constexpr size_t kMaxComment = 2000;
constexpr int kCommentsPage = 50;

const std::array<std::string, 6> kReactions = {"👍", "❤️", "🔥", "😂", "😮", "😢"};

ApiError roomNotFound()
{
    return ApiError(drogon::k404NotFound, "room_not_found", "Not found");
}

ApiError notAllowed()
{
    return ApiError(drogon::k403Forbidden, "not_allowed", "You do not have permission to do this");
}

bool isAdminRole(const std::string &role)
{
    return role == "owner" || role == "admin";
}

Json::Value postEvent(int64_t chatId, int64_t messageId)
{
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    data["messageId"] = std::to_string(messageId);
    return data;
}

Json::Value reactionsJson(const std::vector<ReactionCount> &reactions)
{
    Json::Value list(Json::arrayValue);
    for (const auto &reaction : reactions)
    {
        Json::Value item;
        item["emoji"] = reaction.emoji;
        item["count"] = static_cast<Json::Int64>(reaction.count);
        list.append(item);
    }
    return list;
}
}

Json::Value toJson(const PostStats &stats)
{
    Json::Value json;
    json["id"] = std::to_string(stats.id);
    json["views"] = static_cast<Json::Int64>(stats.views);
    json["comments"] = static_cast<Json::Int64>(stats.comments);
    json["allowReactions"] = stats.allowReactions;
    json["allowComments"] = stats.allowComments;
    json["reactions"] = reactionsJson(stats.reactions);
    json["myReaction"] = stats.myReaction;
    return json;
}

Json::Value toJson(const Comment &comment)
{
    Json::Value json;
    json["id"] = std::to_string(comment.id);
    json["messageId"] = std::to_string(comment.messageId);
    json["userId"] = std::to_string(comment.userId);
    json["userName"] = comment.userName;
    json["userDeleted"] = comment.userDeleted;
    json["text"] = comment.body;
    json["createdAt"] = static_cast<Json::Int64>(comment.createdAt);
    return json;
}

drogon::Task<std::vector<PostStats>> PostService::view(int64_t chatId, int64_t userId, std::vector<int64_t> ids)
{
    co_await requireChannel(chatId, userId);
    if (ids.size() > kMaxViewed)
        throw ApiError(drogon::k400BadRequest, "too_many", "Too many posts at once");
    if (ids.empty())
        co_return std::vector<PostStats>();
    co_await posts_.markViewed(chatId, ids, userId);
    co_return co_await posts_.stats(chatId, ids, userId);
}

drogon::Task<PostStats> PostService::react(int64_t chatId, int64_t messageId, int64_t userId, std::string emoji)
{
    co_await requireChannel(chatId, userId);
    if (std::find(kReactions.begin(), kReactions.end(), emoji) == kReactions.end())
        throw ApiError(drogon::k400BadRequest, "invalid_emoji", "Choose one of the available reactions");
    const auto post = co_await requirePost(chatId, messageId, userId);
    if (!post.allowReactions)
        throw ApiError(drogon::k409Conflict, "reactions_off", "Reactions are turned off for this post");
    co_await posts_.setReaction(chatId, messageId, userId, emoji);
    const auto stats = co_await requirePost(chatId, messageId, userId);
    co_await broadcastReactions(chatId, messageId, stats);
    co_return stats;
}

drogon::Task<PostStats> PostService::unreact(int64_t chatId, int64_t messageId, int64_t userId)
{
    co_await requireChannel(chatId, userId);
    const auto post = co_await requirePost(chatId, messageId, userId);
    if (!post.allowReactions)
        throw ApiError(drogon::k409Conflict, "reactions_off", "Reactions are turned off for this post");
    co_await posts_.clearReaction(chatId, messageId, userId);
    const auto stats = co_await requirePost(chatId, messageId, userId);
    co_await broadcastReactions(chatId, messageId, stats);
    co_return stats;
}

drogon::Task<std::vector<Comment>> PostService::comments(int64_t chatId,
                                                         int64_t messageId,
                                                         int64_t userId,
                                                         int64_t beforeId,
                                                         int limit)
{
    co_await requireChannel(chatId, userId);
    const auto post = co_await requirePost(chatId, messageId, userId);
    if (!post.allowComments)
        throw ApiError(drogon::k409Conflict, "comments_off", "Comments are turned off for this post");
    co_return co_await posts_.comments(chatId, messageId, beforeId, std::clamp(limit, 1, kCommentsPage));
}

drogon::Task<Comment> PostService::comment(int64_t chatId, int64_t messageId, int64_t userId, std::string text)
{
    co_await requireChannel(chatId, userId);
    const auto body = core::text::trim(text);
    const auto length = core::text::characterCount(body);
    if (length < 1 || length > kMaxComment)
        throw ApiError(drogon::k400BadRequest, "invalid_text", "A comment must be 1 to 2000 characters");
    const auto post = co_await requirePost(chatId, messageId, userId);
    if (!post.allowComments)
        throw ApiError(drogon::k409Conflict, "comments_off", "Comments are turned off for this post");
    const auto added = co_await posts_.addComment(core::id::next(), chatId, messageId, userId, body);

    auto data = postEvent(chatId, messageId);
    data["comments"] = static_cast<Json::Int64>(post.comments + 1);
    data["comment"] = toJson(added);
    co_await broadcast(chatId, "post.comment", data);
    co_return added;
}

drogon::Task<> PostService::removeComment(int64_t chatId, int64_t messageId, int64_t userId, int64_t commentId)
{
    const auto room = co_await requireChannel(chatId, userId);
    const auto author = co_await posts_.commentAuthor(chatId, messageId, commentId);
    if (!author)
        throw ApiError(drogon::k404NotFound, "comment_not_found", "Comment not found");
    if (*author != userId && !isAdminRole(room.role))
        throw notAllowed();
    const auto left = co_await posts_.removeComment(chatId, messageId, commentId);
    if (left < 0)
        throw ApiError(drogon::k404NotFound, "comment_not_found", "Comment not found");

    auto data = postEvent(chatId, messageId);
    data["comments"] = static_cast<Json::Int64>(left);
    data["commentId"] = std::to_string(commentId);
    co_await broadcast(chatId, "post.commentDeleted", data);
}

drogon::Task<PostStats> PostService::setSwitches(int64_t chatId, int64_t messageId, int64_t userId, Json::Value body)
{
    const auto room = co_await requireChannel(chatId, userId);
    if (room.role != "owner")
        throw notAllowed();
    const auto post = co_await requirePost(chatId, messageId, userId);
    const bool reactions = body["allowReactions"].isBool() ? body["allowReactions"].asBool() : post.allowReactions;
    const bool comments = body["allowComments"].isBool() ? body["allowComments"].asBool() : post.allowComments;
    co_await posts_.setSwitches(chatId, messageId, reactions, comments);

    auto data = postEvent(chatId, messageId);
    data["allowReactions"] = reactions;
    data["allowComments"] = comments;
    co_await broadcast(chatId, "post.settings", data);
    co_return co_await requirePost(chatId, messageId, userId);
}

drogon::Task<rooms::RoomAccess> PostService::requireChannel(int64_t chatId, int64_t userId)
{
    const auto room = co_await rooms_.access(chatId, userId);
    if (!room || room->status.empty() || room->kind != "channel")
        throw roomNotFound();
    if (room->status != "active")
        throw notAllowed();
    co_return *room;
}

drogon::Task<PostStats> PostService::requirePost(int64_t chatId, int64_t messageId, int64_t userId)
{
    const auto post = co_await posts_.one(chatId, messageId, userId);
    if (!post)
        throw ApiError(drogon::k404NotFound, "message_not_found", "Message not found");
    co_return *post;
}

drogon::Task<> PostService::broadcast(int64_t chatId, std::string event, Json::Value data)
{
    data["room"] = true;
    const auto ids = co_await rooms_.activeMemberIds(chatId);
    for (const auto id : ids)
        realtime::hub().send(id, event, data);
}

drogon::Task<> PostService::broadcastReactions(int64_t chatId, int64_t messageId, const PostStats &stats)
{
    auto data = postEvent(chatId, messageId);
    data["reactions"] = reactionsJson(stats.reactions);
    co_await broadcast(chatId, "post.reactions", data);
}
}
