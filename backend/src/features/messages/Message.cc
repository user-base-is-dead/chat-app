#include "features/messages/Message.h"

namespace features::messages
{
const char *const kMessageColumns =
    "id, chat_id, sender_id, "
    "(SELECT su.display_name FROM users su WHERE su.id = sender_id) AS sender_name, "
    "translate(encode(ciphertext, 'base64'), E'\\n', '') AS ciphertext, "
    "(extract(epoch FROM edited_at) * 1000)::bigint AS edited_at, "
    "(extract(epoch FROM deleted_at) * 1000)::bigint AS deleted_at, "
    "(extract(epoch FROM created_at) * 1000)::bigint AS created_at, "
    "reply_to_id, "
    "(SELECT r.sender_id FROM messages r WHERE r.chat_id = messages.chat_id AND r.id = messages.reply_to_id) "
    "AS reply_sender_id, "
    "(SELECT ru.display_name FROM messages r JOIN users ru ON ru.id = r.sender_id "
    "WHERE r.chat_id = messages.chat_id AND r.id = messages.reply_to_id) AS reply_sender_name, "
    "(SELECT translate(encode(r.ciphertext, 'base64'), E'\\n', '') FROM messages r "
    "WHERE r.chat_id = messages.chat_id AND r.id = messages.reply_to_id) AS reply_ciphertext, "
    "forwarded_from_id, forwarded_from_chat_id, "
    "COALESCE((SELECT fr.title FROM rooms fr WHERE fr.chat_id = messages.forwarded_from_chat_id), "
    "(SELECT fu.display_name FROM users fu WHERE fu.id = messages.forwarded_from_id)) AS forwarded_from_name";

namespace
{
std::optional<int64_t> optionalInt(const drogon::orm::Field &field)
{
    if (field.isNull())
        return std::nullopt;
    return field.as<int64_t>();
}

std::string text(const drogon::orm::Field &field)
{
    return field.isNull() ? std::string() : field.as<std::string>();
}
}

Message messageFromRow(const drogon::orm::Row &row, const std::string &prefix)
{
    const auto ciphertext = row[prefix + "ciphertext"];
    return Message{
        row[prefix + "id"].as<int64_t>(),
        row[prefix + "chat_id"].as<int64_t>(),
        row[prefix + "sender_id"].as<int64_t>(),
        text(row[prefix + "sender_name"]),
        ciphertext.isNull() ? std::nullopt : std::optional<std::string>(ciphertext.as<std::string>()),
        optionalInt(row[prefix + "edited_at"]),
        optionalInt(row[prefix + "deleted_at"]),
        row[prefix + "created_at"].as<int64_t>(),
        std::nullopt,
        std::nullopt,
    };
}

Message fullMessageFromRow(const drogon::orm::Row &row)
{
    auto message = messageFromRow(row);
    if (const auto replyId = optionalInt(row["reply_to_id"]))
    {
        const auto ciphertext = row["reply_ciphertext"];
        message.replyTo = ReplyPreview{
            *replyId,
            optionalInt(row["reply_sender_id"]),
            text(row["reply_sender_name"]),
            ciphertext.isNull() ? std::nullopt : std::optional<std::string>(ciphertext.as<std::string>()),
        };
    }
    if (const auto chatId = optionalInt(row["forwarded_from_chat_id"]))
        message.forwardedFrom = ForwardSource{"channel", *chatId, text(row["forwarded_from_name"])};
    else if (const auto userId = optionalInt(row["forwarded_from_id"]))
        message.forwardedFrom = ForwardSource{"user", *userId, text(row["forwarded_from_name"])};
    return message;
}

Json::Value toJson(const Message &message)
{
    Json::Value json;
    json["id"] = std::to_string(message.id);
    json["chatId"] = std::to_string(message.chatId);
    json["senderId"] = std::to_string(message.senderId);
    json["senderName"] = message.senderName;
    json["ciphertext"] = message.ciphertext ? Json::Value(*message.ciphertext) : Json::Value();
    json["editedAt"] = message.editedAt ? Json::Value(static_cast<Json::Int64>(*message.editedAt)) : Json::Value();
    json["deletedAt"] = message.deletedAt ? Json::Value(static_cast<Json::Int64>(*message.deletedAt)) : Json::Value();
    json["createdAt"] = static_cast<Json::Int64>(message.createdAt);
    json["replyTo"] = Json::Value();
    if (message.replyTo)
    {
        const auto &reply = *message.replyTo;
        Json::Value item;
        item["id"] = std::to_string(reply.id);
        item["deleted"] = !reply.ciphertext.has_value();
        item["senderId"] = reply.senderId ? Json::Value(std::to_string(*reply.senderId)) : Json::Value();
        item["senderName"] = reply.senderName;
        item["ciphertext"] = reply.ciphertext ? Json::Value(*reply.ciphertext) : Json::Value();
        json["replyTo"] = item;
    }
    json["forwardedFrom"] = Json::Value();
    if (message.forwardedFrom)
    {
        Json::Value item;
        item["kind"] = message.forwardedFrom->kind;
        item["id"] = std::to_string(message.forwardedFrom->id);
        item["name"] = message.forwardedFrom->name;
        json["forwardedFrom"] = item;
    }
    return json;
}
}
