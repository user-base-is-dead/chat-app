#pragma once

#include <cstddef>
#include <string>

namespace core::text
{
std::string trim(const std::string &value);
size_t characterCount(const std::string &value);
bool hasStrikeMarks(const std::string &value);
bool isReservedName(const std::string &value);
}
