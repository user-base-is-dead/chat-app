#pragma once

#include <cstdint>
#include <string>

namespace features::push
{
bool enabled();
void notifyNew(int64_t userId, std::string chatId);
}
