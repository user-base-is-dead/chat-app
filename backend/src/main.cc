#include <drogon/drogon.h>

#include "core/app/AppVersions.h"
#include "core/config/Settings.h"
#include "core/http/ApiError.h"
#include "core/database/Database.h"
#include "core/security/Password.h"
#include "features/realtime/Hub.h"

int main()
{
    drogon::app().loadConfigFile("config/config.json");
    core::config::load();
    core::security::init();

    drogon::app().registerHandler(
        "/v1/health",
        [](const drogon::HttpRequestPtr &, std::function<void(const drogon::HttpResponsePtr &)> &&callback) {
            auto shared = std::make_shared<std::function<void(const drogon::HttpResponsePtr &)>>(std::move(callback));
            core::database::client()->execSqlAsync(
                "SELECT 1",
                [shared](const drogon::orm::Result &) {
                    Json::Value body;
                    body["status"] = "ok";
                    body["database"] = "ok";
                    (*shared)(drogon::HttpResponse::newHttpJsonResponse(body));
                },
                [shared](const drogon::orm::DrogonDbException &) {
                    Json::Value body;
                    body["status"] = "error";
                    body["database"] = "down";
                    auto response = drogon::HttpResponse::newHttpJsonResponse(body);
                    response->setStatusCode(drogon::k503ServiceUnavailable);
                    (*shared)(response);
                });
        },
        {drogon::Get});

    drogon::app().registerPostRoutingAdvice(
        [](const drogon::HttpRequestPtr &request, drogon::AdviceCallback &&stop, drogon::AdviceChainCallback &&next) {
            const auto &path = request->path();
            const bool open = path == "/v1/health" || path == "/v1/app/version" || path.rfind("/v1/app/download/", 0) == 0 || path == "/v1/ws" ||
                              (request->method() == drogon::Get && path.rfind("/v1/media/", 0) == 0);
            if (open || core::app::allowed(request->getHeader("x-app-platform"), request->getHeader("x-app-version")))
            {
                next();
                return;
            }
            stop(core::http::errorResponse(core::http::ApiError(
                drogon::k426UpgradeRequired, "update_required", "Please update Civenth to keep using it")));
        });
    drogon::app().registerBeginningAdvice([] {
        core::app::onVersionChange([](const std::string &platform) {
            Json::Value data;
            data["platform"] = platform;
            features::realtime::hub().broadcast("app.version", data);
        });
        core::app::startVersionWatch();
    });

    drogon::app().run();
}
