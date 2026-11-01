#include "core/http/ApiError.h"

namespace core::http
{
drogon::HttpResponsePtr jsonResponse(const Json::Value &body, drogon::HttpStatusCode status)
{
    auto response = drogon::HttpResponse::newHttpJsonResponse(body);
    response->setStatusCode(status);
    return response;
}

drogon::HttpResponsePtr errorResponse(const ApiError &error)
{
    Json::Value body;
    body["error"]["code"] = error.code();
    body["error"]["message"] = error.what();
    return jsonResponse(body, error.status());
}

const Json::Value &requireJson(const drogon::HttpRequestPtr &request)
{
    const auto &json = request->getJsonObject();
    if (!json || !json->isObject())
        throw ApiError(drogon::k400BadRequest, "invalid_json", "Request body must be a JSON object");
    return *json;
}

std::string stringField(const Json::Value &body, const char *name)
{
    const auto &value = body[name];
    return value.isString() ? value.asString() : std::string();
}
}
