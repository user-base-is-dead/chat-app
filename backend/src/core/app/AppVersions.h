#pragma once

#include <functional>
#include <string>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

namespace core::app
{
void startVersionWatch();
void onVersionChange(std::function<void(const std::string &platform)> listener);
bool allowed(const std::string &platform, const std::string &version);
drogon::Task<Json::Value> versionInfo(std::string platform);
}
