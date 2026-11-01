#include "core/auth/AuthFilter.h"

#include <memory>

#include "core/database/Database.h"
#include "core/http/ApiError.h"
#include "core/security/Tokens.h"

namespace core::auth
{
void AuthFilter::doFilter(const drogon::HttpRequestPtr &request,
                          drogon::FilterCallback &&reject,
                          drogon::FilterChainCallback &&next)
{
    const auto unauthorized = [] {
        return http::errorResponse(
            http::ApiError(drogon::k401Unauthorized, "unauthorized", "Missing or invalid access token"));
    };

    const auto &header = request->getHeader("authorization");
    const auto claims = header.rfind("Bearer ", 0) == 0 ? security::verifyAccessToken(header.substr(7)) : std::nullopt;
    if (!claims)
    {
        reject(unauthorized());
        return;
    }

    auto rejectShared = std::make_shared<drogon::FilterCallback>(std::move(reject));
    auto nextShared = std::make_shared<drogon::FilterChainCallback>(std::move(next));
    const auto userId = claims->userId;
    const auto deviceId = claims->deviceId;
    database::client()->execSqlAsync(
        "SELECT 1 FROM devices WHERE id = $1 AND user_id = $2",
        [request, rejectShared, nextShared, unauthorized, userId, deviceId](const drogon::orm::Result &result) {
            if (result.empty())
            {
                (*rejectShared)(unauthorized());
                return;
            }
            request->attributes()->insert("userId", userId);
            request->attributes()->insert("deviceId", deviceId);
            (*nextShared)();
        },
        [rejectShared](const drogon::orm::DrogonDbException &) {
            (*rejectShared)(http::errorResponse(
                http::ApiError(drogon::k503ServiceUnavailable, "unavailable", "Server is busy, try again")));
        },
        deviceId,
        userId);
}

int64_t currentUserId(const drogon::HttpRequestPtr &request)
{
    return request->attributes()->get<int64_t>("userId");
}

int64_t currentDeviceId(const drogon::HttpRequestPtr &request)
{
    return request->attributes()->get<int64_t>("deviceId");
}
}
