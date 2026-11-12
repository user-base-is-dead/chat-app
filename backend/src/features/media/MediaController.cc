#include "features/media/MediaController.h"

#include <filesystem>
#include <fstream>
#include <regex>

#include <drogon/drogon.h>

#include "core/auth/AuthFilter.h"
#include "core/database/Database.h"
#include "core/http/ApiError.h"
#include "core/id/Snowflake.h"
#include "core/security/Tokens.h"

namespace features::media
{
namespace
{
using core::http::ApiError;
using core::http::errorResponse;
using core::http::jsonResponse;

constexpr size_t kMaxPhotoSize = 5 * 1024 * 1024;

std::filesystem::path photoFolder()
{
    return std::filesystem::path(drogon::app().getUploadPath()) / "photos";
}

std::string photoExtension(std::string_view body)
{
    if (body.size() > 3 && body.substr(0, 3) == "\xFF\xD8\xFF")
        return "jpg";
    if (body.size() > 8 && body.substr(0, 8) == std::string_view("\x89PNG\r\n\x1A\n", 8))
        return "png";
    if (body.size() > 12 && body.substr(0, 4) == "RIFF" && body.substr(8, 4) == "WEBP")
        return "webp";
    return "";
}
}

drogon::Task<drogon::HttpResponsePtr> MediaController::uploadPhoto(drogon::HttpRequestPtr request)
{
    try
    {
        const auto body = request->body();
        if (body.empty() || body.size() > kMaxPhotoSize)
            throw ApiError(drogon::k400BadRequest, "invalid_photo", "Photo must be under 5 MB");
        const auto extension = photoExtension(body);
        if (extension.empty())
            throw ApiError(drogon::k400BadRequest, "invalid_photo", "Photo must be JPG, PNG or WebP");

        const auto key = core::security::randomHex(16) + "." + extension;
        std::filesystem::create_directories(photoFolder());
        {
            std::ofstream file(photoFolder() / key, std::ios::binary);
            file.write(body.data(), static_cast<std::streamsize>(body.size()));
            if (!file)
                throw ApiError(drogon::k500InternalServerError, "photo_failed", "Could not save the photo");
        }

        const auto id = core::id::next();
        co_await core::database::client()->execSqlCoro(
            "INSERT INTO media (id, owner_id, storage_key, size) VALUES ($1, $2, $3, $4)",
            id,
            core::auth::currentUserId(request),
            key,
            static_cast<int64_t>(body.size()));

        Json::Value json;
        json["id"] = std::to_string(id);
        json["url"] = "/v1/media/" + key;
        co_return jsonResponse(json, drogon::k201Created);
    }
    catch (const ApiError &error)
    {
        co_return errorResponse(error);
    }
}

drogon::Task<drogon::HttpResponsePtr> MediaController::photo(drogon::HttpRequestPtr, std::string key)
{
    static const std::regex pattern("^[0-9a-f]{32}\\.(jpg|png|webp)$");
    if (!std::regex_match(key, pattern) || !std::filesystem::exists(photoFolder() / key))
        co_return drogon::HttpResponse::newHttpResponse(drogon::k404NotFound, drogon::CT_NONE);

    auto response = drogon::HttpResponse::newFileResponse((photoFolder() / key).string());
    response->addHeader("X-Content-Type-Options", "nosniff");
    response->addHeader("Cache-Control", "public, max-age=31536000, immutable");
    co_return response;
}
}
