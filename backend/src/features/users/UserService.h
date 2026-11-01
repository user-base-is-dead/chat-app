#pragma once

#include <cstdint>
#include <string>

#include <drogon/utils/coroutine.h>

#include "features/devices/DeviceRepository.h"
#include "features/users/UserRepository.h"

namespace features::users
{
class UserService
{
  public:
    drogon::Task<User> updateProfile(int64_t userId, std::string username, std::string displayName, std::string bio);
    drogon::Task<> changePassword(int64_t userId, int64_t deviceId, std::string currentPassword, std::string newPassword);
    drogon::Task<> disable(int64_t userId, std::string password);
    drogon::Task<> deleteAccount(int64_t userId, std::string password);

  private:
    drogon::Task<> requirePassword(int64_t userId, std::string password);

    UserRepository users_;
    devices::DeviceRepository devices_;
};
}
