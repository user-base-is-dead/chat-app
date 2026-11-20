#pragma once

#include <cstdint>

#include <drogon/utils/coroutine.h>

namespace features::realtime
{
drogon::Task<> announcePresence(int64_t userId, bool online);
drogon::Task<> announceVisibility(int64_t userId, bool show);
}
