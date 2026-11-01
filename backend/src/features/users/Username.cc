#include "features/users/Username.h"

#include <regex>

#include "core/http/ApiError.h"

namespace features::users
{
void validateUsername(const std::string &username)
{
    static const std::regex pattern("^[A-Za-z0-9_]{3,32}$");
    if (!std::regex_match(username, pattern))
        throw core::http::ApiError(drogon::k400BadRequest,
                                   "invalid_username",
                                   "Username must be 3-32 characters: letters, numbers or _");
    static const std::regex reserved("^deleted_.*", std::regex::icase);
    if (std::regex_match(username, reserved))
        throw core::http::ApiError(drogon::k400BadRequest, "invalid_username", "This username is reserved");
}
}
