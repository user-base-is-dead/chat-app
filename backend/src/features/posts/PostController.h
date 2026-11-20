#pragma once

#include <string>

#include <drogon/HttpController.h>

#include "features/posts/PostService.h"

namespace features::posts
{
class PostController : public drogon::HttpController<PostController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(PostController::view, "/v1/rooms/{1}/views", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::react, "/v1/rooms/{1}/posts/{2}/reaction", drogon::Put, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::unreact, "/v1/rooms/{1}/posts/{2}/reaction", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::comments, "/v1/rooms/{1}/posts/{2}/comments", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::comment, "/v1/rooms/{1}/posts/{2}/comments", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::removeComment, "/v1/rooms/{1}/posts/{2}/comments/{3}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(PostController::setSwitches, "/v1/rooms/{1}/posts/{2}", drogon::Patch, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> view(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> react(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> unreact(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> comments(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> comment(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);
    drogon::Task<drogon::HttpResponsePtr> removeComment(drogon::HttpRequestPtr request,
                                                        std::string chatId,
                                                        std::string messageId,
                                                        std::string commentId);
    drogon::Task<drogon::HttpResponsePtr> setSwitches(drogon::HttpRequestPtr request, std::string chatId, std::string messageId);

  private:
    PostService service_;
};
}
