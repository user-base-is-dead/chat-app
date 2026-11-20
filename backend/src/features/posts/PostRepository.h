#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

namespace features::posts
{
struct ReactionCount
{
    std::string emoji;
    int64_t count;
};

struct PostStats
{
    int64_t id;
    int64_t views;
    int64_t comments;
    bool allowReactions;
    bool allowComments;
    std::vector<ReactionCount> reactions;
    std::string myReaction;
};

struct Comment
{
    int64_t id;
    int64_t messageId;
    int64_t userId;
    std::string userName;
    bool userDeleted;
    std::string body;
    int64_t createdAt;
};

class PostRepository
{
  public:
    drogon::Task<> markViewed(int64_t chatId, std::vector<int64_t> ids, int64_t userId);
    drogon::Task<std::vector<PostStats>> stats(int64_t chatId, std::vector<int64_t> ids, int64_t userId);
    drogon::Task<std::optional<PostStats>> one(int64_t chatId, int64_t messageId, int64_t userId);
    drogon::Task<> setReaction(int64_t chatId, int64_t messageId, int64_t userId, std::string emoji);
    drogon::Task<> clearReaction(int64_t chatId, int64_t messageId, int64_t userId);
    drogon::Task<std::vector<Comment>> comments(int64_t chatId, int64_t messageId, int64_t beforeId, int limit);
    drogon::Task<Comment> addComment(int64_t id, int64_t chatId, int64_t messageId, int64_t userId, std::string body);
    drogon::Task<std::optional<int64_t>> commentAuthor(int64_t chatId, int64_t messageId, int64_t commentId);
    drogon::Task<int64_t> removeComment(int64_t chatId, int64_t messageId, int64_t commentId);
    drogon::Task<> setSwitches(int64_t chatId, int64_t messageId, bool allowReactions, bool allowComments);
};
}
