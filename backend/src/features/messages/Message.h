#pragma once

#include <cstdint>
#include <optional>
#include <string>

#include <drogon/orm/Field.h>
#include <drogon/orm/Row.h>
#include <json/value.h>

namespace features::messages
{
struct ReplyPreview
{
    int64_t id;
    std::optional<int64_t> senderId;
    std::string senderName;
    std::optional<std::string> ciphertext;
};

struct ForwardSource
{
    std::string kind;
    int64_t id;
    std::string name;
};

struct Message
{
    int64_t id;
    int64_t chatId;
    int64_t senderId;
    std::string senderName;
    std::optional<std::string> ciphertext;
    std::optional<int64_t> editedAt;
    std::optional<int64_t> deletedAt;
    int64_t createdAt;
    std::optional<ReplyPreview> replyTo;
    std::optional<ForwardSource> forwardedFrom;
};

struct Extras
{
    int64_t replyToId = 0;
    int64_t forwardedFromId = 0;
    int64_t forwardedFromChatId = 0;
};

extern const char *const kMessageColumns;

Message messageFromRow(const drogon::orm::Row &row, const std::string &prefix = "");
Message fullMessageFromRow(const drogon::orm::Row &row);
Json::Value toJson(const Message &message);
}
