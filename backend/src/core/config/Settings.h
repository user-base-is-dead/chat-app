#pragma once

#include <string>

namespace core::config
{
struct Settings
{
    std::string jwtSecret;
    int nodeId = 1;
};

void load();
const Settings &settings();
}
