#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

namespace features::users
{
struct User
{
    int64_t id;
    std::string username;
    std::string displayName;
    std::string bio;
};

struct UserCredentials
{
    User user;
    std::string passwordHash;
    bool disabled;
    bool deleted;
};

class UserRepository
{
  public:
    drogon::Task<std::optional<User>> create(int64_t id,
                                             std::string username,
                                             std::string displayName,
                                             std::string passwordHash);
    drogon::Task<std::optional<UserCredentials>> findCredentials(std::string username);
    drogon::Task<std::optional<UserCredentials>> findCredentialsById(int64_t id);
    drogon::Task<std::optional<User>> findById(int64_t id);
    drogon::Task<std::vector<User>> search(std::string query, int64_t excludeUserId, int limit);
    drogon::Task<int64_t> touchLastSeen(int64_t id);
    drogon::Task<std::optional<User>> updateProfile(int64_t id, std::string username, std::string displayName, std::string bio);
    drogon::Task<> setDisabled(int64_t id, bool disabled);
    drogon::Task<> setPasswordHash(int64_t id, std::string passwordHash);
    drogon::Task<> deleteAccount(int64_t id);
};

Json::Value toJson(const User &user);
}
