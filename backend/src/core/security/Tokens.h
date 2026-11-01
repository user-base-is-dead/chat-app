#pragma once

#include <cstdint>
#include <optional>
#include <string>

namespace core::security
{
constexpr int kAccessTokenSeconds = 15 * 60;

struct AccessClaims
{
    int64_t userId;
    int64_t deviceId;
};

std::string createAccessToken(int64_t userId, int64_t deviceId);
std::optional<AccessClaims> verifyAccessToken(const std::string &token);
std::string createRefreshToken();
std::string hashRefreshToken(const std::string &token);
}
