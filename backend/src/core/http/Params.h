#pragma once

#include <cstdint>
#include <string>

namespace core::http
{
int64_t parseId(const std::string &value, const char *name);
int64_t parseOptionalId(const std::string &value, const char *name);
}
