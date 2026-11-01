#include "features/messages/Message.h"

namespace features::messages
{
const char *const kMessageColumns =
    "id, chat_id, sender_id, "
    "translate(encode(ciphertext, 'base64'), E'\\n', '') AS ciphertext, "
    "(extract(epoch FROM edited_at) * 1000)::bigint AS edited_at, "
    "(extract(epoch FROM deleted_at) * 1000)::bigint AS deleted_at, "
    "(extract(epoch FROM created_at) * 1000)::bigint AS created_at";

namespace
{
std::optional<int64_t> optionalInt(const drogon::orm::Field &field)
{
    if (field.isNull())
        return std::nullopt;
    return field.as<int64_t>();
}
}

Message messageFromRow(const drogon::orm::Row &row, const std::string &prefix)
{
    const auto ciphertext = row[prefix + "ciphertext"];
    return Message{
        row[prefix + "id"].as<int64_t>(),
        row[prefix + "chat_id"].as<int64_t>(),
        row[prefix + "sender_id"].as<int64_t>(),
        ciphertext.isNull() ? std::nullopt : std::optional<std::string>(ciphertext.as<std::string>()),
        optionalInt(row[prefix + "edited_at"]),
        optionalInt(row[prefix + "deleted_at"]),
        row[prefix + "created_at"].as<int64_t>(),
    };
}

Json::Value toJson(const Message &message)
{
    Json::Value json;
    json["id"] = std::to_string(message.id);
    json["chatId"] = std::to_string(message.chatId);
    json["senderId"] = std::to_string(message.senderId);
    json["ciphertext"] = message.ciphertext ? Json::Value(*message.ciphertext) : Json::Value();
    json["editedAt"] = message.editedAt ? Json::Value(static_cast<Json::Int64>(*message.editedAt)) : Json::Value();
    json["deletedAt"] = message.deletedAt ? Json::Value(static_cast<Json::Int64>(*message.deletedAt)) : Json::Value();
    json["createdAt"] = static_cast<Json::Int64>(message.createdAt);
    return json;
}
}
