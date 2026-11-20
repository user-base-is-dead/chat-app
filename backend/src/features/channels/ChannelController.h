#pragma once

#include <string>

#include <drogon/HttpController.h>

#include "features/channels/ChannelService.h"

namespace features::channels
{
class ChannelController : public drogon::HttpController<ChannelController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(ChannelController::list, "/v1/rooms/{1}/channels", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::create, "/v1/rooms/{1}/channels", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::update, "/v1/rooms/{1}/channels/{2}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::remove, "/v1/rooms/{1}/channels/{2}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::createCategory, "/v1/rooms/{1}/categories", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::updateCategory, "/v1/rooms/{1}/categories/{2}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::removeCategory, "/v1/rooms/{1}/categories/{2}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::channelPermissions, "/v1/rooms/{1}/channels/{2}/permissions", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::setChannelOverride, "/v1/rooms/{1}/channels/{2}/permissions/{3}/{4}", drogon::Put, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::syncChannel, "/v1/rooms/{1}/channels/{2}/sync", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::categoryPermissions, "/v1/rooms/{1}/categories/{2}/permissions", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(ChannelController::setCategoryOverride, "/v1/rooms/{1}/categories/{2}/permissions/{3}/{4}", drogon::Put, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> list(drogon::HttpRequestPtr request, std::string roomId);
    drogon::Task<drogon::HttpResponsePtr> create(drogon::HttpRequestPtr request, std::string roomId);
    drogon::Task<drogon::HttpResponsePtr> update(drogon::HttpRequestPtr request, std::string roomId, std::string channelId);
    drogon::Task<drogon::HttpResponsePtr> remove(drogon::HttpRequestPtr request, std::string roomId, std::string channelId);
    drogon::Task<drogon::HttpResponsePtr> createCategory(drogon::HttpRequestPtr request, std::string roomId);
    drogon::Task<drogon::HttpResponsePtr> updateCategory(drogon::HttpRequestPtr request, std::string roomId, std::string categoryId);
    drogon::Task<drogon::HttpResponsePtr> removeCategory(drogon::HttpRequestPtr request, std::string roomId, std::string categoryId);
    drogon::Task<drogon::HttpResponsePtr> channelPermissions(drogon::HttpRequestPtr request, std::string roomId, std::string channelId);
    drogon::Task<drogon::HttpResponsePtr> setChannelOverride(drogon::HttpRequestPtr request,
                                                             std::string roomId,
                                                             std::string channelId,
                                                             std::string targetType,
                                                             std::string targetId);
    drogon::Task<drogon::HttpResponsePtr> syncChannel(drogon::HttpRequestPtr request, std::string roomId, std::string channelId);
    drogon::Task<drogon::HttpResponsePtr> categoryPermissions(drogon::HttpRequestPtr request, std::string roomId, std::string categoryId);
    drogon::Task<drogon::HttpResponsePtr> setCategoryOverride(drogon::HttpRequestPtr request,
                                                              std::string roomId,
                                                              std::string categoryId,
                                                              std::string targetType,
                                                              std::string targetId);

  private:
    ChannelService service_;
};
}
