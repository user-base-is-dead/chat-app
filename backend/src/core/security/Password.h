#pragma once

#include <string>

#include <drogon/utils/coroutine.h>

namespace core::security
{
void init();
drogon::Task<std::string> hashPassword(std::string password);
drogon::Task<bool> verifyPassword(std::string hash, std::string password);
const std::string &dummyHash();
}
