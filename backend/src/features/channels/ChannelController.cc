#include "features/channels/ChannelController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "core/http/Params.h"

namespace features::channels
{
using core::auth::currentUserId;
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::parseId;
using core::http::requireJson;

namespace
{
drogon::HttpResponsePtr noContent()
{
    return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
}
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::list(drogon::HttpRequestPtr request, std::string roomId)
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

drogon::Task<drogon::HttpResponsePtr> ChannelController::create(drogon::HttpRequestPtr request, std::string roomId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto channel = co_await service_.create(parseId(roomId, "roomId"), currentUserId(request), body);
        co_return jsonResponse(toJson(channel), drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::update(drogon::HttpRequestPtr request,
                                                                std::string roomId,
                                                                std::string channelId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto channel = co_await service_.update(
            parseId(roomId, "roomId"), parseId(channelId, "channelId"), currentUserId(request), body);
        co_return jsonResponse(toJson(channel));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::remove(drogon::HttpRequestPtr request,
                                                                std::string roomId,
                                                                std::string channelId)
{
    try
    {
        co_await service_.remove(parseId(roomId, "roomId"), parseId(channelId, "channelId"), currentUserId(request));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::createCategory(drogon::HttpRequestPtr request, std::string roomId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto category = co_await service_.createCategory(parseId(roomId, "roomId"), currentUserId(request), body);
        co_return jsonResponse(toJson(category), drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::updateCategory(drogon::HttpRequestPtr request,
                                                                        std::string roomId,
                                                                        std::string categoryId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto category = co_await service_.updateCategory(
            parseId(roomId, "roomId"), parseId(categoryId, "categoryId"), currentUserId(request), body);
        co_return jsonResponse(toJson(category));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::removeCategory(drogon::HttpRequestPtr request,
                                                                        std::string roomId,
                                                                        std::string categoryId)
{
    try
    {
        co_await service_.removeCategory(parseId(roomId, "roomId"), parseId(categoryId, "categoryId"), currentUserId(request));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::channelPermissions(drogon::HttpRequestPtr request,
                                                                            std::string roomId,
                                                                            std::string channelId)
{
    try
    {
        co_return jsonResponse(co_await service_.channelPermissions(
            parseId(roomId, "roomId"), parseId(channelId, "channelId"), currentUserId(request)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::setChannelOverride(drogon::HttpRequestPtr request,
                                                                            std::string roomId,
                                                                            std::string channelId,
                                                                            std::string targetType,
                                                                            std::string targetId)
{
    try
    {
        const auto &body = requireJson(request);
        co_return jsonResponse(co_await service_.setChannelOverride(parseId(roomId, "roomId"),
                                                                    parseId(channelId, "channelId"),
                                                                    currentUserId(request),
                                                                    targetType,
                                                                    targetId,
                                                                    body));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::syncChannel(drogon::HttpRequestPtr request,
                                                                     std::string roomId,
                                                                     std::string channelId)
{
    try
    {
        co_return jsonResponse(co_await service_.syncChannel(
            parseId(roomId, "roomId"), parseId(channelId, "channelId"), currentUserId(request)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::categoryPermissions(drogon::HttpRequestPtr request,
                                                                             std::string roomId,
                                                                             std::string categoryId)
{
    try
    {
        co_return jsonResponse(co_await service_.categoryPermissions(
            parseId(roomId, "roomId"), parseId(categoryId, "categoryId"), currentUserId(request)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChannelController::setCategoryOverride(drogon::HttpRequestPtr request,
                                                                             std::string roomId,
                                                                             std::string categoryId,
                                                                             std::string targetType,
                                                                             std::string targetId)
{
    try
    {
        const auto &body = requireJson(request);
        co_return jsonResponse(co_await service_.setCategoryOverride(parseId(roomId, "roomId"),
                                                                     parseId(categoryId, "categoryId"),
                                                                     currentUserId(request),
                                                                     targetType,
                                                                     targetId,
                                                                     body));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
