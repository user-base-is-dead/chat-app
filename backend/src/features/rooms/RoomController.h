#pragma once

#include <string>

#include <drogon/HttpController.h>

#include "features/rooms/RoomService.h"

namespace features::rooms
{
class RoomController : public drogon::HttpController<RoomController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(RoomController::list, "/v1/rooms", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::create, "/v1/rooms", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::search, "/v1/find/rooms", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::preview, "/v1/join/{1}", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::join, "/v1/join/{1}", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::detail, "/v1/rooms/{1}", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::update, "/v1/rooms/{1}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::remove, "/v1/rooms/{1}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::accept, "/v1/rooms/{1}/accept", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::decline, "/v1/rooms/{1}/decline", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::leave, "/v1/rooms/{1}/leave", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::mute, "/v1/rooms/{1}/mute", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::unmute, "/v1/rooms/{1}/unmute", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::pin, "/v1/rooms/{1}/pin", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::unpin, "/v1/rooms/{1}/unpin", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::resetInvite, "/v1/rooms/{1}/invite", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::transfer, "/v1/rooms/{1}/transfer", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::members, "/v1/rooms/{1}/members", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::addMembers, "/v1/rooms/{1}/members", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::setRole, "/v1/rooms/{1}/members/{2}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoomController::removeMember, "/v1/rooms/{1}/members/{2}", drogon::Delete, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> list(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> create(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> search(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> preview(drogon::HttpRequestPtr request, std::string key);
    drogon::Task<drogon::HttpResponsePtr> join(drogon::HttpRequestPtr request, std::string key);
    drogon::Task<drogon::HttpResponsePtr> detail(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> update(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> remove(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> accept(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> decline(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> leave(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> mute(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> unmute(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> pin(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> unpin(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> resetInvite(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> transfer(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> members(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> addMembers(drogon::HttpRequestPtr request, std::string chatId);
    drogon::Task<drogon::HttpResponsePtr> setRole(drogon::HttpRequestPtr request, std::string chatId, std::string userId);
    drogon::Task<drogon::HttpResponsePtr> removeMember(drogon::HttpRequestPtr request, std::string chatId, std::string userId);

  private:
    RoomService service_;
};
}
