#include "core/security/Tokens.h"

#include <chrono>

#include <jwt-cpp/jwt.h>
#include <sodium.h>

#include "core/config/Settings.h"

namespace core::security
{
std::string createAccessToken(int64_t userId, int64_t deviceId)
{
    const auto now = std::chrono::system_clock::now();
    return jwt::create()
        .set_type("JWT")
        .set_subject(std::to_string(userId))
        .set_payload_claim("did", jwt::claim(std::to_string(deviceId)))
        .set_issued_at(now)
        .set_expires_at(now + std::chrono::seconds(kAccessTokenSeconds))
        .sign(jwt::algorithm::hs256{config::settings().jwtSecret});
}

std::optional<AccessClaims> verifyAccessToken(const std::string &token)
{
    try
    {
        const auto decoded = jwt::decode(token);
        jwt::verify().allow_algorithm(jwt::algorithm::hs256{config::settings().jwtSecret}).verify(decoded);
        return AccessClaims{std::stoll(decoded.get_subject()), std::stoll(decoded.get_payload_claim("did").as_string())};
    }
    catch (...)
    {
        return std::nullopt;
    }
}

std::string createRefreshToken()
{
    unsigned char bytes[32];
    randombytes_buf(bytes, sizeof bytes);
    char out[sodium_base64_ENCODED_LEN(sizeof bytes, sodium_base64_VARIANT_URLSAFE_NO_PADDING)];
    sodium_bin2base64(out, sizeof out, bytes, sizeof bytes, sodium_base64_VARIANT_URLSAFE_NO_PADDING);
    return out;
}

std::string hashRefreshToken(const std::string &token)
{
    unsigned char hash[crypto_hash_sha256_BYTES];
    crypto_hash_sha256(hash, reinterpret_cast<const unsigned char *>(token.data()), token.size());
    char hex[crypto_hash_sha256_BYTES * 2 + 1];
    sodium_bin2hex(hex, sizeof hex, hash, sizeof hash);
    return hex;
}
}
