#include "features/chats/ChatController.h"

#include <algorithm>

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "core/http/Params.h"

namespace features::chats
{
using core::auth::currentDeviceId;
using core::auth::currentUserId;
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;
using core::http::parseId;
using core::http::parseOptionalId;
using core::http::requireJson;
using core::http::stringField;

drogon::Task<drogon::HttpResponsePtr> ChatController::list(drogon::HttpRequestPtr request)
{
    const auto chats = co_await service_.list(currentUserId(request));
    Json::Value body(Json::arrayValue);
    for (const auto &chat : chats)
        body.append(toJson(chat));
    co_return jsonResponse(body);
}

drogon::Task<drogon::HttpResponsePtr> ChatController::openDm(drogon::HttpRequestPtr request)
{
    try
    {
        const auto &body = requireJson(request);
        const auto peerId = parseId(stringField(body, "userId"), "userId");
        auto chat = co_await service_.openDm(currentUserId(request), peerId);
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::accept(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.accept(parseId(chatId, "chatId"), currentUserId(request));
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::restrictChat(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.restrictChat(parseId(chatId, "chatId"), currentUserId(request));
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::unrestrictChat(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.unrestrictChat(parseId(chatId, "chatId"), currentUserId(request));
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::pin(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.pin(parseId(chatId, "chatId"), currentUserId(request));
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::unpin(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.unpin(parseId(chatId, "chatId"), currentUserId(request));
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::mute(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.setMuted(parseId(chatId, "chatId"), currentUserId(request), true);
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::unmute(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        auto chat = co_await service_.setMuted(parseId(chatId, "chatId"), currentUserId(request), false);
        co_return jsonResponse(toJson(chat));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::remove(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        co_await service_.remove(parseId(chatId, "chatId"), currentUserId(request));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::messages(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto before = parseOptionalId(request->getParameter("before"), "before");
        const auto afterParam = request->getParameter("after");
        const auto limitParam = request->getParameter("limit");
        const int limit = limitParam.empty() ? 50 : std::clamp(std::atoi(limitParam.c_str()), 1, 100);

        const auto id = parseId(chatId, "chatId");
        std::vector<messages::Message> list;
        if (afterParam.empty())
            list = co_await service_.messages(id, currentUserId(request), before, limit);
        else
            list = co_await service_.messagesAfter(
                id, currentUserId(request), afterParam == "0" ? 0 : parseId(afterParam, "after"), limit);
        Json::Value body(Json::arrayValue);
        for (const auto &message : list)
            body.append(messages::toJson(message));
        co_return jsonResponse(body);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::send(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        auto message = co_await service_.send(parseId(chatId, "chatId"),
                                              currentUserId(request),
                                              currentDeviceId(request),
                                              stringField(body, "ciphertext"));
        co_return jsonResponse(messages::toJson(message), drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::edit(drogon::HttpRequestPtr request,
                                                           std::string chatId,
                                                           std::string messageId)
{
    try
    {
        const auto &body = requireJson(request);
        auto message = co_await service_.edit(parseId(chatId, "chatId"),
                                              currentUserId(request),
                                              parseId(messageId, "messageId"),
                                              stringField(body, "ciphertext"));
        co_return jsonResponse(messages::toJson(message));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::deleteMessage(drogon::HttpRequestPtr request,
                                                                    std::string chatId,
                                                                    std::string messageId)
{
    try
    {
        Json::Value body;
        body["chatDeleted"] = co_await service_.deleteMessage(
            parseId(chatId, "chatId"), currentUserId(request), parseId(messageId, "messageId"));
        co_return jsonResponse(body);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> ChatController::markRead(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        co_await service_.markRead(
            parseId(chatId, "chatId"), currentUserId(request), parseId(stringField(body, "messageId"), "messageId"));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
