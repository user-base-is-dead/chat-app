#pragma once

#include <cstdint>
#include <optional>
#include <string>

#include <drogon/orm/Field.h>
#include <drogon/orm/Row.h>
#include <json/value.h>

namespace features::messages
{
struct Message
{
    int64_t id;
    int64_t chatId;
    int64_t senderId;
    std::optional<std::string> ciphertext;
    std::optional<int64_t> editedAt;
    std::optional<int64_t> deletedAt;
    int64_t createdAt;
};

extern const char *const kMessageColumns;

Message messageFromRow(const drogon::orm::Row &row, const std::string &prefix = "");
Json::Value toJson(const Message &message);
}
