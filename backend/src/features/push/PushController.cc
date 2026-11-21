#include "features/push/PushController.h"

#include "core/auth/AuthFilter.h"
#include "core/http/ApiError.h"
#include "features/push/PushSender.h"

namespace features::push
{
namespace
{
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;

constexpr size_t kMaxTokenSize = 4096;
}

drogon::Task<drogon::HttpResponsePtr> PushController::save(drogon::HttpRequestPtr request)
{
    try
    {
        const auto token = core::http::stringField(core::http::requireJson(request), "token");
        if (token.empty() || token.size() > kMaxTokenSize)
            throw ApiError(drogon::k400BadRequest, "invalid_token", "Push token is not valid");
        co_await devices_.setPushToken(core::auth::currentDeviceId(request), token);
        Json::Value body;
        body["enabled"] = enabled();
        co_return jsonResponse(body);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> PushController::remove(drogon::HttpRequestPtr request)
{
    co_await devices_.setPushToken(core::auth::currentDeviceId(request), "");
    co_return drogon::HttpResponse::newHttpResponse(drogon::k204NoContent, drogon::CT_NONE);
}
}
