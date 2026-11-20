#include "features/posts/PostController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "core/http/Params.h"

namespace features::posts
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
constexpr int kDefaultLimit = 50;

std::vector<int64_t> idList(const Json::Value &value)
{
    std::vector<int64_t> ids;
    if (!value.isArray())
        return ids;
    for (const auto &item : value)
        ids.push_back(parseId(item.isString() ? item.asString() : std::string(), "ids"));
    return ids;
}
}

drogon::Task<drogon::HttpResponsePtr> PostController::view(drogon::HttpRequestPtr request, std::string chatId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto list = co_await service_.view(parseId(chatId, "chatId"), currentUserId(request), idList(body["ids"]));
        Json::Value posts(Json::arrayValue);
        for (const auto &stats : list)
            posts.append(toJson(stats));
        Json::Value json;
        json["posts"] = posts;
        co_return jsonResponse(json);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::react(drogon::HttpRequestPtr request,
                                                            std::string chatId,
                                                            std::string messageId)
{
    try
    {
        const auto &body = requireJson(request);
        co_return jsonResponse(toJson(co_await service_.react(parseId(chatId, "chatId"),
                                                              parseId(messageId, "messageId"),
                                                              currentUserId(request),
                                                              stringField(body, "emoji"))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::unreact(drogon::HttpRequestPtr request,
                                                              std::string chatId,
                                                              std::string messageId)
{
    try
    {
        co_return jsonResponse(toJson(
            co_await service_.unreact(parseId(chatId, "chatId"), parseId(messageId, "messageId"), currentUserId(request))));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::comments(drogon::HttpRequestPtr request,
                                                               std::string chatId,
                                                               std::string messageId)
{
    try
    {
        const auto before = parseOptionalId(request->getParameter("before"), "before");
        const auto list = co_await service_.comments(
            parseId(chatId, "chatId"), parseId(messageId, "messageId"), currentUserId(request), before, kDefaultLimit);
        Json::Value items(Json::arrayValue);
        for (const auto &item : list)
            items.append(toJson(item));
        Json::Value json;
        json["comments"] = items;
        co_return jsonResponse(json);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::comment(drogon::HttpRequestPtr request,
                                                              std::string chatId,
                                                              std::string messageId)
{
    try
    {
        const auto &body = requireJson(request);
        const auto added = co_await service_.comment(
            parseId(chatId, "chatId"), parseId(messageId, "messageId"), currentUserId(request), stringField(body, "text"));
        co_return jsonResponse(toJson(added), drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::removeComment(drogon::HttpRequestPtr request,
                                                                    std::string chatId,
                                                                    std::string messageId,
                                                                    std::string commentId)
{
    try
    {
        co_await service_.removeComment(parseId(chatId, "chatId"),
                                        parseId(messageId, "messageId"),
                                        currentUserId(request),
                                        parseId(commentId, "commentId"));
        co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PostController::setSwitches(drogon::HttpRequestPtr request,
                                                                  std::string chatId,
                                                                  std::string messageId)
{
    try
    {
        const auto &body = requireJson(request);
        co_return jsonResponse(toJson(co_await service_.setSwitches(
            parseId(chatId, "chatId"), parseId(messageId, "messageId"), currentUserId(request), body)));
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}
}
