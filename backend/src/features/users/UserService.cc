#include "features/users/UserService.h"

#include "core/http/ApiError.h"
#include "core/security/Password.h"
#include "core/text/Text.h"
#include "features/chats/ChatService.h"
#include "features/realtime/Hub.h"
#include "features/users/Username.h"

namespace features::users
{
namespace
{
using core::http::ApiError;
using core::text::characterCount;
using core::text::hasStrikeMarks;
using core::text::isReservedName;
using core::text::trim;

constexpr size_t kMaxDisplayName = 64;
constexpr size_t kMaxBio = 140;
}

drogon::Task<User> UserService::updateProfile(int64_t userId,
                                              std::string username,
                                              std::string displayName,
                                              std::string bio)
{
    username = trim(username);
    validateUsername(username);
    displayName = trim(displayName);
    bio = trim(bio);
    if (displayName.empty() || characterCount(displayName) > kMaxDisplayName)
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "Name must be 1-64 characters");
    if (isReservedName(displayName))
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "This name is reserved");
    if (hasStrikeMarks(displayName))
        throw ApiError(drogon::k400BadRequest, "invalid_display_name", "Name cannot contain strikethrough characters");
    if (characterCount(bio) > kMaxBio)
        throw ApiError(drogon::k400BadRequest, "invalid_bio", "Bio can be at most 140 characters");

    auto user = co_await users_.updateProfile(userId, username, displayName, bio);
    if (!user)
        throw ApiError(drogon::k409Conflict, "username_taken", "This username is already taken");
    co_return *user;
}

drogon::Task<> UserService::changePassword(int64_t userId,
                                           int64_t deviceId,
                                           std::string currentPassword,
                                           std::string newPassword)
{
    if (newPassword.size() < 6 || newPassword.size() > 128)
        throw ApiError(drogon::k400BadRequest, "invalid_password", "New password must be 6-128 characters");
    co_await requirePassword(userId, currentPassword);
    if (currentPassword == newPassword)
        throw ApiError(drogon::k400BadRequest, "same_password", "New password must be different");

    co_await users_.setPasswordHash(userId, co_await core::security::hashPassword(newPassword));
    co_await devices_.removeOthersForUser(userId, deviceId);
}

drogon::Task<> UserService::disable(int64_t userId, std::string password)
{
    co_await requirePassword(userId, password);
    co_await users_.setDisabled(userId, true);
    co_await devices_.removeAllForUser(userId);
    co_await chats::notifyPeersChanged(userId);
    realtime::hub().disconnect(userId);
}

drogon::Task<> UserService::deleteAccount(int64_t userId, std::string password)
{
    co_await requirePassword(userId, password);
    const auto links = co_await chats::ChatRepository().dmsOf(userId);
    co_await users_.deleteAccount(userId);
    chats::notifyChatsChanged(links);
    realtime::hub().disconnect(userId);
}

drogon::Task<> UserService::requirePassword(int64_t userId, std::string password)
{
    auto credentials = co_await users_.findCredentialsById(userId);
    if (!credentials)
        throw ApiError(drogon::k404NotFound, "user_not_found", "Account not found");
    if (password.empty() || !co_await core::security::verifyPassword(credentials->passwordHash, password))
        throw ApiError(drogon::k403Forbidden, "wrong_password", "Incorrect password");
}
}
