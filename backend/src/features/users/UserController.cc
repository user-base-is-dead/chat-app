#include "features/users/UserController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"

namespace features::users
{
using core::auth::currentDeviceId;
using core::auth::currentUserId;
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::requireJson;
using core::http::stringField;

drogon::Task<drogon::HttpResponsePtr> UserController::search(drogon::HttpRequestPtr request)
{
    const auto query = request->getParameter("q");
    Json::Value body(Json::arrayValue);
    if (query.size() < 2 || query.size() > 32)
        co_return jsonResponse(body);

    const auto users = co_await users_.search(query, currentUserId(request), 20);
    for (const auto &user : users)
        body.append(toJson(user));
    co_return jsonResponse(body);
}

drogon::Task<drogon::HttpResponsePtr> UserController::updateProfile(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        auto user = co_await service_.updateProfile(
            currentUserId(request), stringField(body, "username"), stringField(body, "displayName"), stringField(body, "bio"));
        co_return jsonResponse(toJson(user));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> UserController::changePassword(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.changePassword(currentUserId(request),
                                         currentDeviceId(request),
                                         stringField(body, "currentPassword"),
                                         stringField(body, "newPassword"));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> UserController::disable(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.disable(currentUserId(request), stringField(body, "password"));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> UserController::deleteAccount(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.deleteAccount(currentUserId(request), stringField(body, "password"));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
