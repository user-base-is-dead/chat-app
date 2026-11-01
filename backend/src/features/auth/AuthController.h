#pragma once

#include <drogon/HttpController.h>

#include "features/auth/AuthService.h"

namespace features::auth
{
class AuthController : public drogon::HttpController<AuthController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(AuthController::registerUser, "/v1/auth/register", drogon::Post);
    ADD_METHOD_TO(AuthController::login, "/v1/auth/login", drogon::Post);
    ADD_METHOD_TO(AuthController::refresh, "/v1/auth/refresh", drogon::Post);
    ADD_METHOD_TO(AuthController::logout, "/v1/auth/logout", drogon::Post, "core::auth::AuthFilter");
    ADD_METHOD_TO(AuthController::me, "/v1/auth/me", drogon::Get, "core::auth::AuthFilter");
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> registerUser(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> login(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> refresh(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> logout(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> me(drogon::HttpRequestPtr request);

  private:
    AuthService service_;
};
}
