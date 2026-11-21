#pragma once

#include <drogon/HttpController.h>

namespace features::app
{
class AppController : public drogon::HttpController<AppController>
{
  public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(AppController::version, "/v1/app/version", drogon::Get);
    ADD_METHOD_TO(AppController::download, "/v1/app/download/{1}", drogon::Get);
    METHOD_LIST_END

    drogon::Task<drogon::HttpResponsePtr> version(drogon::HttpRequestPtr request);
    drogon::Task<drogon::HttpResponsePtr> download(drogon::HttpRequestPtr request, std::string name);
};
}
