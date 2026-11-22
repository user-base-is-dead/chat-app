#include "features/chats/ChatService.h"

#include <cctype>

#include "core/http/ApiError.h"
#include "core/id/Snowflake.h"
#include "features/realtime/Hub.h"

namespace features::chats
{
namespace
{
using core::http::ApiError;

constexpr size_t kMaxCiphertextLength = 65536;
constexpr int64_t kPinLimit = 5;

ApiError chatNotFound()
{
    return ApiError(drogon::k404NotFound, "chat_not_found", "Chat not found");
}

ApiError messageNotFound()
{
    return ApiError(drogon::k404NotFound, "message_not_found", "Message not found");
}

Json::Value pinEvent(int64_t chatId, const std::optional<messages::Message> &message)
{
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    data["message"] = message ? messages::toJson(*message) : Json::Value();
    return data;
}

bool isBase64(const std::string &value)
{
    size_t padding = 0;
    for (const char c : value)
    {
        if (c == '=')
        {
            ++padding;
            continue;
        }
        if (padding > 0 || !(std::isalnum(static_cast<unsigned char>(c)) || c == '+' || c == '/'))
            return false;
    }
    return padding <= 2;
}

void validateCiphertext(const std::string &ciphertext)
{
    if (ciphertext.empty() || ciphertext.size() > kMaxCiphertextLength || ciphertext.size() % 4 != 0 ||
        !isBase64(ciphertext))
        throw ApiError(drogon::k400BadRequest, "invalid_ciphertext", "Message content is missing or invalid");
}

void notifyBoth(int64_t userId, int64_t peerId, const std::string &event, const Json::Value &data)
{
    realtime::hub().send(userId, event, data);
    realtime::hub().send(peerId, event, data);
}

void notifyReadUpTo(int64_t chatId, int64_t userId, int64_t messageId, int64_t receiverId)
{
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    data["userId"] = std::to_string(userId);
    data["upToMessageId"] = std::to_string(messageId);
    realtime::hub().send(receiverId, "message.read", data);
}

void notifyChatDeleted(int64_t userId, int64_t peerId, int64_t chatId)
{
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    notifyBoth(userId, peerId, "chat.deleted", data);
}
}

Json::Value toJson(const ChatSummary &chat)
{
    Json::Value json;
    json["id"] = std::to_string(chat.id);
    json["status"] = chat.status;
    json["peer"] = users::toJson(chat.peer);
    json["peer"]["online"] = chat.peerVisible && chat.peerShowsLastSeen && !chat.peerDisabled && !chat.peerDeleted &&
                             realtime::hub().isOnline(chat.peer.id);
    json["peer"]["disabled"] = chat.peerDisabled;
    json["peer"]["deleted"] = chat.peerDeleted;
    if (chat.peerDisabled)
        json["peer"]["bio"] = "";
    if (chat.peerDeleted)
    {
        json["peer"]["username"] = "";
        json["peer"]["displayName"] = "";
        json["peer"]["bio"] = "";
    }
    json["peer"]["lastSeenAt"] =
        chat.peerLastSeenAt ? Json::Value(static_cast<Json::Int64>(*chat.peerLastSeenAt)) : Json::Value();
    json["lastReadId"] = std::to_string(chat.lastReadId);
    json["peerLastReadId"] = std::to_string(chat.peerLastReadId);
    json["unreadCount"] = static_cast<Json::Int64>(chat.unreadCount);
    json["pinnedAt"] = chat.pinnedAt ? Json::Value(static_cast<Json::Int64>(*chat.pinnedAt)) : Json::Value();
    json["muted"] = chat.muted;
    json["lastMessage"] = chat.lastMessage ? messages::toJson(*chat.lastMessage) : Json::Value();
    return json;
}

drogon::Task<> notifyPeersChanged(int64_t userId)
{
    Json::Value data;
    data["userId"] = std::to_string(userId);
    const auto peers = co_await ChatRepository().allPeerIds(userId);
    for (const auto peerId : peers)
        realtime::hub().send(peerId, "chat.changed", data);
}

void notifyChatsChanged(const std::vector<DmLink> &links)
{
    for (const auto &link : links)
    {
        Json::Value data;
        data["chatId"] = std::to_string(link.chatId);
        realtime::hub().send(link.peerId, "chat.changed", data);
    }
}

drogon::Task<std::vector<ChatSummary>> ChatService::list(int64_t userId)
{
    co_return co_await chats_.listForUser(userId);
}

drogon::Task<ChatSummary> ChatService::openDm(int64_t userId, int64_t peerId)
{
    if (userId == peerId)
        throw ApiError(drogon::k400BadRequest, "invalid_peer", "You cannot start a chat with yourself");
    if (!co_await users_.findById(peerId))
        throw ApiError(drogon::k404NotFound, "user_not_found", "User not found");

    auto chatId = co_await chats_.findDm(userId, peerId);
    if (chatId)
        co_await chats_.ensureMember(*chatId, userId, "active");
    else
        chatId = co_await chats_.createDm(core::id::next(), userId, peerId);

    auto chat = co_await chats_.findForUser(*chatId, userId);
    if (!chat)
        throw chatNotFound();
    co_return *chat;
}

drogon::Task<ChatSummary> ChatService::accept(int64_t chatId, int64_t userId)
{
    co_return co_await changeStatus(chatId, userId, "request", "active");
}

drogon::Task<ChatSummary> ChatService::restrictChat(int64_t chatId, int64_t userId)
{
    co_return co_await changeStatus(chatId, userId, "active", "restricted");
}

drogon::Task<ChatSummary> ChatService::unrestrictChat(int64_t chatId, int64_t userId)
{
    co_return co_await changeStatus(chatId, userId, "restricted", "active");
}

drogon::Task<ChatSummary> ChatService::changeStatus(int64_t chatId, int64_t userId, std::string from, std::string to)
{
    const auto member = co_await requireMember(chatId, userId);
    if (!co_await chats_.changeStatus(chatId, userId, from, to))
        throw ApiError(drogon::k409Conflict, "invalid_status", "This chat cannot be changed right now");

    auto chat = co_await chats_.findForUser(chatId, userId);
    if (!chat)
        throw chatNotFound();

    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    notifyBoth(userId, member.peerId, "chat.changed", data);
    if (to == "active")
        notifyReadUpTo(chatId, userId, chat->lastReadId, member.peerId);
    co_return *chat;
}

drogon::Task<ChatSummary> ChatService::pin(int64_t chatId, int64_t userId)
{
    co_await requireMember(chatId, userId);
    if (!co_await chats_.pin(chatId, userId, kPinLimit))
        throw ApiError(drogon::k409Conflict, "pin_limit", "You can pin up to 5 chats");
    co_return co_await ownChanged(chatId, userId);
}

drogon::Task<ChatSummary> ChatService::unpin(int64_t chatId, int64_t userId)
{
    co_await requireMember(chatId, userId);
    co_await chats_.unpin(chatId, userId);
    co_return co_await ownChanged(chatId, userId);
}

drogon::Task<ChatSummary> ChatService::setMuted(int64_t chatId, int64_t userId, bool muted)
{
    co_await requireMember(chatId, userId);
    co_await chats_.setMuted(chatId, userId, muted);
    co_return co_await ownChanged(chatId, userId);
}

drogon::Task<ChatSummary> ChatService::ownChanged(int64_t chatId, int64_t userId)
{
    auto chat = co_await chats_.findForUser(chatId, userId);
    if (!chat)
        throw chatNotFound();

    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    realtime::hub().send(userId, "chat.changed", data);
    co_return *chat;
}

drogon::Task<> ChatService::remove(int64_t chatId, int64_t userId)
{
    const auto member = co_await requireMember(chatId, userId);
    co_await chats_.remove(chatId);
    notifyChatDeleted(userId, member.peerId, chatId);
}

drogon::Task<std::vector<messages::Message>> ChatService::messages(int64_t chatId,
                                                                   int64_t userId,
                                                                   int64_t beforeId,
                                                                   int limit)
{
    co_await requireReadable(chatId, userId);
    co_return co_await messages_.list(chatId, beforeId, limit);
}

drogon::Task<std::vector<messages::Message>> ChatService::messagesAfter(int64_t chatId,
                                                                        int64_t userId,
                                                                        int64_t afterId,
                                                                        int limit)
{
    co_await requireReadable(chatId, userId);
    co_return co_await messages_.listAfter(chatId, afterId, limit);
}

drogon::Task<messages::Message> ChatService::send(int64_t chatId,
                                                  int64_t userId,
                                                  int64_t deviceId,
                                                  std::string ciphertext,
                                                  bool allowReactions,
                                                  bool allowComments,
                                                  messages::Extras extras)
{
    validateCiphertext(ciphertext);
    if (const auto room = co_await roomAccess_.access(chatId, userId))
        co_return co_await rooms_.send(chatId, *room, userId, deviceId, ciphertext, allowReactions, allowComments, extras);
    const auto member = co_await requireMember(chatId, userId);
    if (member.status == "request")
        throw ApiError(drogon::k403Forbidden, "accept_first", "Accept the message request before replying");
    if (member.status == "restricted")
        throw ApiError(drogon::k403Forbidden, "unrestrict_first", "Unrestrict this chat before replying");
    const auto peer = co_await users_.findCredentialsById(member.peerId);
    if (!peer || peer->deleted)
        throw ApiError(drogon::k403Forbidden, "peer_deleted", "This account is deleted");
    if (peer->disabled)
        throw ApiError(drogon::k403Forbidden, "peer_disabled", "This account is disabled");

    if (extras.replyToId && !co_await messages_.exists(chatId, extras.replyToId))
        throw ApiError(drogon::k400BadRequest, "reply_not_found", "The message you are replying to is gone");

    co_await chats_.addRecipient(chatId, member.peerId);
    auto message = co_await messages_.insert(core::id::next(), chatId, userId, deviceId, ciphertext, true, true, extras);
    co_await chats_.markRead(chatId, userId, message.id);
    notifyBoth(userId, member.peerId, "message.new", messages::toJson(message));
    co_return message;
}

drogon::Task<messages::Message> ChatService::edit(int64_t chatId,
                                                  int64_t userId,
                                                  int64_t messageId,
                                                  std::string ciphertext)
{
    validateCiphertext(ciphertext);
    if (const auto room = co_await roomAccess_.access(chatId, userId))
        co_return co_await rooms_.edit(chatId, *room, userId, messageId, ciphertext);
    const auto member = co_await requireMember(chatId, userId);
    auto message = co_await messages_.edit(chatId, messageId, userId, ciphertext);
    if (!message)
        throw ApiError(drogon::k404NotFound, "message_not_found", "Message not found");
    notifyBoth(userId, member.peerId, "message.edited", messages::toJson(*message));
    co_return *message;
}

drogon::Task<bool> ChatService::deleteMessage(int64_t chatId, int64_t userId, int64_t messageId)
{
    if (const auto room = co_await roomAccess_.access(chatId, userId))
    {
        co_await rooms_.deleteMessage(chatId, *room, userId, messageId);
        co_return false;
    }
    const auto member = co_await requireMember(chatId, userId);
    if (!co_await messages_.remove(chatId, messageId))
        throw ApiError(drogon::k404NotFound, "message_not_found", "Message not found");

    if (co_await chats_.removeIfEmpty(chatId))
    {
        notifyChatDeleted(userId, member.peerId, chatId);
        co_return true;
    }
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    data["id"] = std::to_string(messageId);
    notifyBoth(userId, member.peerId, "message.deleted", data);
    co_return false;
}

drogon::Task<> ChatService::markRead(int64_t chatId, int64_t userId, int64_t messageId)
{
    if (const auto room = co_await roomAccess_.access(chatId, userId))
    {
        co_await rooms_.markRead(chatId, *room, userId, messageId);
        co_return;
    }
    const auto member = co_await requireMember(chatId, userId);
    co_await chats_.markRead(chatId, userId, messageId);

    notifyReadUpTo(chatId, userId, messageId, userId);
    if (member.status == "active")
        notifyReadUpTo(chatId, userId, messageId, member.peerId);
}

drogon::Task<Forwarded> ChatService::forwardFrom(int64_t sourceChatId, int64_t userId, int64_t messageId)
{
    co_await requireReadable(sourceChatId, userId);
    const auto source = co_await messages_.find(sourceChatId, messageId);
    if (!source || !source->ciphertext)
        throw messageNotFound();
    Forwarded forwarded{*source->ciphertext, {}};
    if (source->forwardedFrom)
    {
        if (source->forwardedFrom->kind == "channel")
            forwarded.extras.forwardedFromChatId = source->forwardedFrom->id;
        else
            forwarded.extras.forwardedFromId = source->forwardedFrom->id;
    }
    else if (co_await isChannel(sourceChatId, userId))
        forwarded.extras.forwardedFromChatId = sourceChatId;
    else
        forwarded.extras.forwardedFromId = source->senderId;
    co_return forwarded;
}

drogon::Task<std::optional<messages::Message>> ChatService::pinned(int64_t chatId, int64_t userId)
{
    co_await requireReadable(chatId, userId);
    co_return co_await messages_.pinned(chatId);
}

drogon::Task<messages::Message> ChatService::pinMessage(int64_t chatId, int64_t userId, int64_t messageId)
{
    if (const auto room = co_await roomAccess_.access(chatId, userId))
        co_return co_await rooms_.pinMessage(chatId, *room, userId, messageId);
    const auto member = co_await requireMember(chatId, userId);
    const auto message = co_await messages_.find(chatId, messageId);
    if (!message)
        throw messageNotFound();
    co_await messages_.setPinned(chatId, messageId);
    notifyBoth(userId, member.peerId, "message.pinned", pinEvent(chatId, message));
    co_return *message;
}

drogon::Task<> ChatService::unpinMessage(int64_t chatId, int64_t userId)
{
    if (const auto room = co_await roomAccess_.access(chatId, userId))
    {
        co_await rooms_.unpinMessage(chatId, *room, userId);
        co_return;
    }
    const auto member = co_await requireMember(chatId, userId);
    co_await messages_.setPinned(chatId, 0);
    notifyBoth(userId, member.peerId, "message.pinned", pinEvent(chatId, std::nullopt));
}

drogon::Task<Json::Value> ChatService::place(int64_t chatId, int64_t userId)
{
    co_await requireReadable(chatId, userId);
    Json::Value body;
    body["chatId"] = std::to_string(chatId);
    if (const auto room = co_await roomAccess_.access(chatId, userId))
    {
        body["kind"] = room->kind == "group" ? "text" : "channel";
        body["roomId"] = std::to_string(room->roomId);
    }
    else
        body["kind"] = "dm";
    co_return body;
}

drogon::Task<bool> ChatService::isChannel(int64_t chatId, int64_t userId)
{
    const auto room = co_await roomAccess_.access(chatId, userId);
    co_return room && room->kind == "channel";
}

drogon::Task<> ChatService::requireReadable(int64_t chatId, int64_t userId)
{
    if (const auto room = co_await roomAccess_.access(chatId, userId))
    {
        if (room->status != "active" || (room->kind == "group" && !room->canInChannel("viewChannel")))
            throw chatNotFound();
        co_return;
    }
    co_await requireMember(chatId, userId);
}

drogon::Task<Membership> ChatService::requireMember(int64_t chatId, int64_t userId)
{
    auto member = co_await chats_.membership(chatId, userId);
    if (!member)
        throw chatNotFound();
    co_return *member;
}
}
