#include "core/id/Snowflake.h"

#include <chrono>
#include <mutex>

#include "core/config/Settings.h"

namespace core::id
{
namespace
{
constexpr int64_t kEpochMs = 1767225600000;
constexpr int64_t kSequenceMask = 0xFFF;

std::mutex mutex;
int64_t lastMs = 0;
int64_t sequence = 0;

int64_t nowMs()
{
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}
}

int64_t next()
{
    std::lock_guard lock(mutex);

    int64_t ms = nowMs();
    if (ms < lastMs)
        ms = lastMs;

    if (ms == lastMs)
    {
        sequence = (sequence + 1) & kSequenceMask;
        if (sequence == 0)
        {
            while (ms <= lastMs)
                ms = nowMs();
        }
    }
    else
    {
        sequence = 0;
    }

    lastMs = ms;
    return ((ms - kEpochMs) << 22) | (static_cast<int64_t>(config::settings().nodeId) << 12) | sequence;
}
}
