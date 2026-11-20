#include "features/rooms/RoomController.h"

#include <algorithm>

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "core/http/Params.h"

namespace features::rooms
{
using core::auth::currentUserId;
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::parseId;
using core::http::parseOptionalId;
using core::http::requireJson;
using core::http::stringField;

namespace
{
drogon::HttpResponsePtr noContent()
{
    return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
}

std::vector<int64_t> idList(const Json::Value &value)
{
    std::vector<int64_t> ids;
    if (!value.isArray())
        return ids;
    for (const auto &item : value)
        ids.push_back(parseId(item.isString() ? item.asString() : std::string(), "userIds"));
    return ids;
}
}

drogon::Task<drogon::HttpResponsePtr> RoomController::list(drogon::HttpRequestPtr request)
{
    const auto rooms = co_await service_.list(currentUserId(request));
    Json::Value body(Json::arrayValue);
    for (const auto &room : rooms)
        body.append(toJson(room));
    co_return jsonResponse(body);
}

drogon::Task<drogon::HttpResponsePtr> RoomController::create(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        const auto kind = stringField(body, "kind");
        RoomFields fields{stringField(body, "title"),
                          stringField(body, "about"),
                          stringField(body, "username"),
                          body["isPublic"].asBool(),
                          parseOptionalId(stringField(body, "photoId"), "photoId"),
                          true,
                          false};
        auto json = co_await service_.create(currentUserId(request), kind, std::move(fields), idList(body["userIds"]));
        co_return jsonResponse(json, drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::search(drogon::HttpRequestPtr request)
{
    const auto query = request->getParameter("q");
    Json::Value body(Json::arrayValue);
    if (query.size() < 2 || query.size() > 64)
        co_return jsonResponse(body);
    const auto rooms = co_await service_.search(query, request->getParameter("kind"), currentUserId(request));
    for (const auto &room : rooms)
        body.append(toJson(room));
    co_return jsonResponse(body);
}

drogon::Task<drogon::HttpResponsePtr> RoomController::preview(drogon::HttpRequestPtr request, std::string key)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.preview(key, currentUserId(request))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::join(drogon::HttpRequestPtr request, std::string key)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.join(key, currentUserId(request))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::detail(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(co_await service_.detail(parseId(chatId, "chatId"), currentUserId(request)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::update(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        co_return jsonResponse(toJson(co_await service_.update(parseId(chatId, "chatId"), currentUserId(request), body)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::remove(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_await service_.remove(parseId(chatId, "chatId"), currentUserId(request));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::accept(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.accept(parseId(chatId, "chatId"), currentUserId(request))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::decline(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_await service_.decline(parseId(chatId, "chatId"), currentUserId(request));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::leave(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_await service_.leave(parseId(chatId, "chatId"), currentUserId(request));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::mute(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.setMuted(parseId(chatId, "chatId"), currentUserId(request), true)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::unmute(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.setMuted(parseId(chatId, "chatId"), currentUserId(request), false)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::pin(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.setPinned(parseId(chatId, "chatId"), currentUserId(request), true)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::unpin(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.setPinned(parseId(chatId, "chatId"), currentUserId(request), false)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::resetInvite(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_return jsonResponse(toJson(co_await service_.resetInvite(parseId(chatId, "chatId"), currentUserId(request))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::transfer(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.transfer(
            parseId(chatId, "chatId"), currentUserId(request), parseId(stringField(body, "userId"), "userId"));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::members(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto offset = std::max(0, std::atoi(request->getParameter("offset").c_str()));
        const auto channel = request->getParameter("channel");
        const auto list = co_await service_.members(
            parseId(chatId, "chatId"), currentUserId(request), offset, channel.empty() ? 0 : parseId(channel, "channel"));
        Json::Value body(Json::arrayValue);
        for (const auto &member : list)
            body.append(toJson(member));
        co_return jsonResponse(body);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::addMembers(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto result =
            co_await service_.addMembers(parseId(chatId, "chatId"), currentUserId(request), idList(body["userIds"]));
        Json::Value json;
        json["added"] = result.added;
        json["invited"] = result.invited;
        json["skipped"] = result.skipped;
        co_return jsonResponse(json);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::setRole(drogon::HttpRequestPtr request,
                                                              std::string chatId,
                                                              std::string userId)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.setRole(
            parseId(chatId, "chatId"), currentUserId(request), parseId(userId, "userId"), stringField(body, "role"));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> RoomController::removeMember(drogon::HttpRequestPtr request,
                                                                   std::string chatId,
                                                                   std::string userId)
{
    try
    {
        co_await service_.removeMember(parseId(chatId, "chatId"), currentUserId(request), parseId(userId, "userId"));
        co_return noContent();
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
