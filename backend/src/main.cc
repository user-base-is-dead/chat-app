#include <drogon/drogon.h>

#include "core/config/Settings.h"
#include "core/database/Database.h"
#include "core/security/Password.h"

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

    drogon::app().run();
}
