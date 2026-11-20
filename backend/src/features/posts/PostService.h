#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/posts/PostRepository.h"
#include "features/rooms/RoomRepository.h"

namespace features::posts
{
Json::Value toJson(const PostStats &stats);
Json::Value toJson(const Comment &comment);

class PostService
{
  public:
    drogon::Task<std::vector<PostStats>> view(int64_t chatId, int64_t userId, std::vector<int64_t> ids);
    drogon::Task<PostStats> react(int64_t chatId, int64_t messageId, int64_t userId, std::string emoji);
    drogon::Task<PostStats> unreact(int64_t chatId, int64_t messageId, int64_t userId);
    drogon::Task<std::vector<Comment>> comments(int64_t chatId, int64_t messageId, int64_t userId, int64_t beforeId, int limit);
    drogon::Task<Comment> comment(int64_t chatId, int64_t messageId, int64_t userId, std::string text);
    drogon::Task<> removeComment(int64_t chatId, int64_t messageId, int64_t userId, int64_t commentId);
    drogon::Task<PostStats> setSwitches(int64_t chatId, int64_t messageId, int64_t userId, Json::Value body);

  private:
    drogon::Task<rooms::RoomAccess> requireChannel(int64_t chatId, int64_t userId);
    drogon::Task<PostStats> requirePost(int64_t chatId, int64_t messageId, int64_t userId);
    drogon::Task<> broadcast(int64_t chatId, std::string event, Json::Value data);
    drogon::Task<> broadcastReactions(int64_t chatId, int64_t messageId, const PostStats &stats);

    PostRepository posts_;
    rooms::RoomRepository rooms_;
};
}
