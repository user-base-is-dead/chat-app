#pragma once

#include <string>

#include <drogon/HttpController.h>

namespace features::media
{
class MediaController : public drogon::HttpController<MediaController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(MediaController::uploadPhoto, "/v1/media/photo", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(MediaController::photo, "/v1/media/{1}", drogon::Get);
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> uploadPhoto(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> photo(drogon::HttpRequestPtr request, std::string key);
};
}
