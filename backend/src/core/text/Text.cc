#include "core/text/Text.h"

#include <algorithm>
#include <cctype>

namespace core::text
{
std::string trim(const std::string &value)
{
    const auto start = value.find_first_not_of(" \t\r\n");
    if (start == std::string::npos)
        return "";
    const auto end = value.find_last_not_of(" \t\r\n");
    return value.substr(start, end - start + 1);
}

size_t characterCount(const std::string &value)
{
    size_t count = 0;
    for (const unsigned char c : value)
        if ((c & 0xC0) != 0x80)
            ++count;
    return count;
}

bool hasStrikeMarks(const std::string &value)
{
    for (size_t i = 0; i + 1 < value.size(); ++i)
    {
        const auto lead = static_cast<unsigned char>(value[i]);
        const auto next = static_cast<unsigned char>(value[i + 1]);
        if (lead == 0xCC && next >= 0xB4 && next <= 0xB8)
            return true;
    }
    return false;
}

bool isReservedName(const std::string &value)
{
    std::string lower = value;
    std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char c) { return std::tolower(c); });
    return lower == "deleted account";
}
}
