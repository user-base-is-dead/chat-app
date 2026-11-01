#pragma once

#include <cstdint>
#include <optional>
#include <string>

#include <drogon/utils/coroutine.h>

namespace features::devices
{
struct DeviceSession
{
    int64_t deviceId;
    int64_t userId;
    bool expired;
};

class DeviceRepository
{
  public:
    drogon::Task<> create(int64_t id, int64_t userId, std::string platform, std::string name, std::string refreshHash);
    drogon::Task<std::optional<DeviceSession>> findByRefreshHash(std::string refreshHash);
    drogon::Task<> replaceRefreshHash(int64_t id, std::string refreshHash);
    drogon::Task<> remove(int64_t id);
    drogon::Task<> removeAllForUser(int64_t userId);
    drogon::Task<> removeOthersForUser(int64_t userId, int64_t keepDeviceId);
};
}
