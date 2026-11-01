#pragma once

#include <stdexcept>
#include <string>

#include <drogon/HttpRequest.h>
#include <drogon/HttpResponse.h>

namespace core::http
{
class ApiError : public std::runtime_error
{
  public:
    ApiError(drogon::HttpStatusCode status, std::string code, const std::string &message)
        : std::runtime_error(message), status_(status), code_(std::move(code))
    {
    }

    drogon::HttpStatusCode status() const
    {
        return status_;
    }

    const std::string &code() const
    {
        return code_;
    }

  private:
    drogon::HttpStatusCode status_;
    std::string code_;
};

drogon::HttpResponsePtr jsonResponse(const Json::Value &body, drogon::HttpStatusCode status = drogon::k200OK);
drogon::HttpResponsePtr errorResponse(const ApiError &error);
const Json::Value &requireJson(const drogon::HttpRequestPtr &request);
std::string stringField(const Json::Value &body, const char *name);
}
