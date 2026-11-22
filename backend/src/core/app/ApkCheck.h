#pragma once

#include <optional>
#include <string>

namespace core::app
{
struct ApkInfo
{
    std::string packageName;
    int versionCode = 0;
};

std::optional<ApkInfo> readApk(const std::string &path);
bool releaseFileMatches(const std::string &downloadUrl, int versionCode);
}
