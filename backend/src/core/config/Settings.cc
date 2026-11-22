#include "core/config/Settings.h"

#include <cstdlib>
#include <stdexcept>

#include <drogon/drogon.h>

namespace core::config
{
namespace
{
Settings current;
}

void load()
{
    const auto &custom = drogon::app().getCustomConfig();

    const char *envSecret = std::getenv("CIVENTH_JWT_SECRET");
    current.jwtSecret = envSecret ? envSecret : custom["jwt_secret"].asString();
    if (current.jwtSecret.size() < 32)
        throw std::runtime_error("jwt_secret must be at least 32 characters");

    current.nodeId = custom.get("node_id", 1).asInt();
    if (current.nodeId < 0 || current.nodeId > 1023)
        throw std::runtime_error("node_id must be between 0 and 1023");

    const char *envFcm = std::getenv("CIVENTH_FCM_KEY");
    current.fcmKeyFile = envFcm ? envFcm : custom.get("fcm_key_file", "").asString();
}

const Settings &settings()
{
    return current;
}
}
