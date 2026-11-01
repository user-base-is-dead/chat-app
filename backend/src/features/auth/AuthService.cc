#include "features/auth/AuthService.h"

#include "core/http/ApiError.h"
#include "core/id/Snowflake.h"
#include "core/security/Password.h"
#include "core/security/Tokens.h"
#include "core/text/Text.h"
#include "features/chats/ChatService.h"
#include "features/users/Username.h"

namespace features::auth
{
namespace
{
using core::http::ApiError;

std::string normalizeDisplayName(const std::string &displayName)
{
    auto name = core::text::trim(displayName);
    if (name.empty() || core::text::characterCount(name) > 64)
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "Name must be 1-64 characters");
    if (core::text::isReservedName(name))
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "This name is reserved");
    if (core::text::hasStrikeMarks(name))
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "Name cannot contain strikethrough characters");
    return name;
}

void validatePassword(const std::string &password)
{
    if (password.size() < 6 || password.size() > 128)
        throw ApiError(drogon::k400BadRequest, "invalid_password", "Password must be 6-128 characters");
}

std::string normalizePlatform(const std::string &platform)
{
    if (platform.empty())
        return "web";
    if (platform != "web" && platform != "android" && platform != "ios")
        throw ApiError(drogon::k400BadRequest, "invalid_platform", "Platform must be web, android or ios");
    return platform;
}

std::string normalizeDeviceName(const std::string &name)
{
    return name.substr(0, 64);
}

ApiError invalidCredentials()
{
    return ApiError(drogon::k401Unauthorized, "invalid_credentials", "Invalid username or password");
}
}

Json::Value toJson(const Session &session)
{
    Json::Value json;
    json["user"] = users::toJson(session.user);
    json["deviceId"] = std::to_string(session.deviceId);
    json["accessToken"] = session.accessToken;
    json["refreshToken"] = session.refreshToken;
    json["expiresIn"] = core::security::kAccessTokenSeconds;
    return json;
}

drogon::Task<users::User> AuthService::registerUser(std::string displayName, std::string username, std::string password)
{
    displayName = normalizeDisplayName(displayName);
    users::validateUsername(username);
    validatePassword(password);

    auto passwordHash = co_await core::security::hashPassword(password);
    auto user = co_await users_.create(core::id::next(), username, displayName, passwordHash);
    if (!user)
        throw ApiError(drogon::k409Conflict, "account_exists", "An account with this username already exists");
    co_return *user;
}

drogon::Task<Session> AuthService::login(std::string username,
                                         std::string password,
                                         std::string platform,
                                         std::string deviceName)
{
    platform = normalizePlatform(platform);
    if (username.empty() || password.empty())
        throw invalidCredentials();

    auto credentials = co_await users_.findCredentials(username);
    if (!credentials)
    {
        co_await core::security::verifyPassword(core::security::dummyHash(), password);
        throw invalidCredentials();
    }

    if (!co_await core::security::verifyPassword(credentials->passwordHash, password))
        throw invalidCredentials();
    if (credentials->disabled)
    {
        co_await users_.setDisabled(credentials->user.id, false);
        co_await chats::notifyPeersChanged(credentials->user.id);
    }

    co_return co_await openSession(credentials->user, platform, normalizeDeviceName(deviceName));
}

drogon::Task<Session> AuthService::refresh(std::string refreshToken)
{
    const auto invalid =
        ApiError(drogon::k401Unauthorized, "invalid_refresh_token", "Refresh token is invalid or expired");
    if (refreshToken.empty())
        throw invalid;

    auto device = co_await devices_.findByRefreshHash(core::security::hashRefreshToken(refreshToken));
    if (!device || device->expired)
        throw invalid;

    auto user = co_await users_.findById(device->userId);
    if (!user)
        throw invalid;

    auto newRefreshToken = core::security::createRefreshToken();
    co_await devices_.replaceRefreshHash(device->deviceId, core::security::hashRefreshToken(newRefreshToken));

    co_return Session{*user,
                      device->deviceId,
                      core::security::createAccessToken(user->id, device->deviceId),
                      newRefreshToken};
}

drogon::Task<> AuthService::logout(int64_t deviceId)
{
    co_await devices_.remove(deviceId);
}

drogon::Task<users::User> AuthService::me(int64_t userId)
{
    auto user = co_await users_.findById(userId);
    if (!user)
        throw ApiError(drogon::k401Unauthorized, "unauthorized", "Account no longer exists");
    co_return *user;
}

drogon::Task<Session> AuthService::openSession(users::User user, std::string platform, std::string deviceName)
{
    const auto deviceId = core::id::next();
    auto refreshToken = core::security::createRefreshToken();
    co_await devices_.create(deviceId, user.id, platform, deviceName, core::security::hashRefreshToken(refreshToken));
    co_return Session{user, deviceId, core::security::createAccessToken(user.id, deviceId), refreshToken};
}
}
