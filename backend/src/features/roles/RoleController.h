#pragma once

#include <string>

#include <drogon/HttpController.h>

#include "features/roles/RoleService.h"

namespace features::roles
{
class RoleController : public drogon::HttpController<RoleController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(RoleController::list, "/v1/rooms/{1}/roles", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoleController::create, "/v1/rooms/{1}/roles", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoleController::update, "/v1/rooms/{1}/roles/{2}", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoleController::remove, "/v1/rooms/{1}/roles/{2}", drogon::Delete, "core::auth::AuthFilter");
    ADD_METHOD_TO(RoleController::setMemberRoles, "/v1/rooms/{1}/members/{2}/roles", drogon::Put, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> list(drogon::HttpRequestPtr request, std::string roomId);
    drogon::Task<drogon::HttpResponsePtr> create(drogon::HttpRequestPtr request, std::string roomId);
    drogon::Task<drogon::HttpResponsePtr> update(drogon::HttpRequestPtr request, std::string roomId, std::string roleId);
    drogon::Task<drogon::HttpResponsePtr> remove(drogon::HttpRequestPtr request, std::string roomId, std::string roleId);
    drogon::Task<drogon::HttpResponsePtr> setMemberRoles(drogon::HttpRequestPtr request, std::string roomId, std::string userId);

  private:
    RoleService service_;
};
}
