#include "features/roles/RoleController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "core/http/Params.h"

namespace features::roles
{
using core::auth::currentUserId;
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::parseId;
using core::http::requireJson;

drogon::Task<drogon::HttpResponsePtr> RoleController::list(drogon::HttpRequestPtr request, std::string roomId)
{
    try
    {
        co_return jsonResponse(co_await service_.list(parseId(roomId, "roomId"), currentUserId(request)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoleController::create(drogon::HttpRequestPtr request, std::string roomId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto role = co_await service_.create(parseId(roomId, "roomId"), currentUserId(request), body);
        co_return jsonResponse(toJson(role), drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoleController::update(drogon::HttpRequestPtr request,
                                                             std::string roomId,
                                                             std::string roleId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto role = co_await service_.update(
            parseId(roomId, "roomId"), parseId(roleId, "roleId"), currentUserId(request), body);
        co_return jsonResponse(toJson(role));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoleController::remove(drogon::HttpRequestPtr request,
                                                             std::string roomId,
                                                             std::string roleId)
{
    try
    {
        co_await service_.remove(parseId(roomId, "roomId"), parseId(roleId, "roleId"), currentUserId(request));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoleController::setMemberRoles(drogon::HttpRequestPtr request,
                                                                     std::string roomId,
                                                                     std::string userId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto ids = co_await service_.setMemberRoles(
            parseId(roomId, "roomId"), parseId(userId, "userId"), currentUserId(request), body);
        Json::Value reply;
        reply["roleIds"] = Json::Value(Json::arrayValue);
        for (const auto id : ids)
            reply["roleIds"].append(std::to_string(id));
        co_return jsonResponse(reply);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
