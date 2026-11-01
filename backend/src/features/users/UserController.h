#pragma once

#include <drogon/HttpController.h>

#include "features/users/UserRepository.h"
#include "features/users/UserService.h"

namespace features::users
{
class UserController : public drogon::HttpController<UserController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(UserController::search, "/v1/users/search", drogon::Get, "core::auth::AuthFilter");
    ADD_METHOD_TO(UserController::updateProfile, "/v1/users/me", drogon::Patch, "core::auth::AuthFilter");
    ADD_METHOD_TO(UserController::changePassword, "/v1/users/me/password", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(UserController::disable, "/v1/users/me/disable", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(UserController::deleteAccount, "/v1/users/me/delete", drogon::Post, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> search(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> updateProfile(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> changePassword(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> disable(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> deleteAccount(drogon::HttpRequestPtr request);

  private:
    UserRepository users_;
    UserService service_;
};
}
