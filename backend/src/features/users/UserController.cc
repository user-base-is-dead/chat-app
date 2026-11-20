#include "features/users/UserController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "features/realtime/Presence.h"

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

drogon::Task<drogon::HttpResponsePtr> UserController::privacy(drogon::HttpRequestPtr request)
{
    Json::Value body;
    body["showLastSeen"] = (co_await users_.showsLastSeen(currentUserId(request))).value_or(true);
    co_return jsonResponse(body);
}

drogon::Task<drogon::HttpResponsePtr> UserController::setPrivacy(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        if (!body["showLastSeen"].isBool())
            throw ApiError(drogon::k400BadRequest, "invalid_value", "showLastSeen must be true or false");
        const bool show = body["showLastSeen"].asBool();
        const auto userId = currentUserId(request);
        co_await users_.setShowLastSeen(userId, show);
        co_await realtime::announceVisibility(userId, show);
        Json::Value reply;
        reply["showLastSeen"] = show;
        co_return jsonResponse(reply);
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
