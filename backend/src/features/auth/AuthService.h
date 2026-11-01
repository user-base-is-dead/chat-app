#pragma once

#include <cstdint>
#include <string>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/devices/DeviceRepository.h"
#include "features/users/UserRepository.h"

namespace features::auth
{
struct Session
{
    users::User user;
    int64_t deviceId;
    std::string accessToken;
    std::string refreshToken;
};

Json::Value toJson(const Session &session);

class AuthService
{
  public:
    drogon::Task<users::User> registerUser(std::string displayName, std::string username, std::string password);
    drogon::Task<Session> login(std::string username, std::string password, std::string platform, std::string deviceName);
    drogon::Task<Session> refresh(std::string refreshToken);
    drogon::Task<> logout(int64_t deviceId);
    drogon::Task<users::User> me(int64_t userId);

  private:
    drogon::Task<Session> openSession(users::User user, std::string platform, std::string deviceName);

    users::UserRepository users_;
    devices::DeviceRepository devices_;
};
}
