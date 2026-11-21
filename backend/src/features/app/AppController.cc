#include "features/app/AppController.h"

#include <algorithm>
#include <filesystem>
#include <fstream>
#include <iterator>

#include "core/app/AppVersions.h"
#include "core/http/ApiError.h"

namespace features::app
{
drogon::Task<drogon::HttpResponsePtr> AppController::version(drogon::HttpRequestPtr request)
{
    const auto platform = request->getParameter("platform");
    if (platform != "android" && platform != "web")
        co_return core::http::errorResponse(
            core::http::ApiError(drogon::k400BadRequest, "invalid_platform", "platform must be android or web"));
    co_return core::http::jsonResponse(co_await core::app::versionInfo(platform));
}

drogon::Task<drogon::HttpResponsePtr> AppController::download(drogon::HttpRequestPtr, std::string name)
{
    const auto valid = !name.empty() && name.size() <= 80 && name.ends_with(".apk") && name[0] != '.' &&
                       std::all_of(name.begin(), name.end(), [](const char c) {
                           return std::isalnum(static_cast<unsigned char>(c)) || c == '.' || c == '-' || c == '_';
                       });
    const auto path = std::filesystem::path("releases") / name;
    if (!valid || !std::filesystem::is_regular_file(path))
        co_return core::http::errorResponse(core::http::ApiError(drogon::k404NotFound, "not_found", "File not found"));
    std::ifstream file(path, std::ios::binary);
    std::string body((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());
    auto response = drogon::HttpResponse::newHttpResponse();
    response->setContentTypeString("application/vnd.android.package-archive");
    response->addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
    response->setBody(std::move(body));
    co_return response;
}
}
