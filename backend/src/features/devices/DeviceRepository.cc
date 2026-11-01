#include "features/devices/DeviceRepository.h"

#include "core/database/Database.h"

namespace features::devices
{
drogon::Task<> DeviceRepository::create(int64_t id,
                                        int64_t userId,
                                        std::string platform,
                                        std::string name,
                                        std::string refreshHash)
{
    co_await core::database::client()->execSqlCoro(
        "INSERT INTO devices (id, user_id, platform, name, refresh_token_hash, refresh_expires_at) "
        "VALUES ($1, $2, $3, $4, decode($5, 'hex'), now() + interval '30 days')",
        id,
        userId,
        platform,
        name,
        refreshHash);
}

drogon::Task<std::optional<DeviceSession>> DeviceRepository::findByRefreshHash(std::string refreshHash)
{
    const auto result = co_await core::database::client()->execSqlCoro(
        "SELECT id, user_id, refresh_expires_at < now() AS expired FROM devices "
        "WHERE refresh_token_hash = decode($1, 'hex')",
        refreshHash);
    if (result.empty())
        co_return std::nullopt;
    co_return DeviceSession{
        result[0]["id"].as<int64_t>(), result[0]["user_id"].as<int64_t>(), result[0]["expired"].as<bool>()};
}

drogon::Task<> DeviceRepository::replaceRefreshHash(int64_t id, std::string refreshHash)
{
    co_await core::database::client()->execSqlCoro(
        "UPDATE devices SET refresh_token_hash = decode($2, 'hex'), "
        "refresh_expires_at = now() + interval '30 days', last_active_at = now() WHERE id = $1",
        id,
        refreshHash);
}

drogon::Task<> DeviceRepository::remove(int64_t id)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM devices WHERE id = $1", id);
}

drogon::Task<> DeviceRepository::removeAllForUser(int64_t userId)
{
    co_await core::database::client()->execSqlCoro("DELETE FROM devices WHERE user_id = $1", userId);
}

drogon::Task<> DeviceRepository::removeOthersForUser(int64_t userId, int64_t keepDeviceId)
{
    co_await core::database::client()->execSqlCoro(
        "DELETE FROM devices WHERE user_id = $1 AND id <> $2", userId, keepDeviceId);
}
}
