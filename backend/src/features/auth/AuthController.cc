#include "features/auth/AuthController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"

namespace features::auth
{
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::requireJson;
using core::http::stringField;

drogon::Task<drogon::HttpResponsePtr> AuthController::registerUser(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        auto user = co_await service_.registerUser(
            stringField(body, "displayName"), stringField(body, "username"), stringField(body, "password"));
        Json::Value json;
        json["user"] = users::toJson(user);
        co_return jsonResponse(json, drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> AuthController::login(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        auto session = co_await service_.login(stringField(body, "username"),
                                               stringField(body, "password"),
                                               stringField(body, "platform"),
                                               stringField(body, "deviceName"));
        co_return jsonResponse(toJson(session));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> AuthController::refresh(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        auto session = co_await service_.refresh(stringField(body, "refreshToken"));
        co_return jsonResponse(toJson(session));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> AuthController::logout(drogon::HttpRequestPtr request)
{
    co_await service_.logout(core::auth::currentDeviceId(request));
    co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
}

drogon::Task<drogon::HttpResponsePtr> AuthController::me(drogon::HttpRequestPtr request)
{
    try
    {
        auto user = co_await service_.me(core::auth::currentUserId(request));
        co_return jsonResponse(users::toJson(user));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
