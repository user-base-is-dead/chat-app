#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/chats/ChatRepository.h"
#include "features/messages/MessageRepository.h"
#include "features/rooms/RoomService.h"
#include "features/users/UserRepository.h"

namespace features::chats
{
struct Forwarded
{
    std::string ciphertext;
    messages::Extras extras;
};

Json::Value toJson(const ChatSummary &chat);
drogon::Task<> notifyPeersChanged(int64_t userId);
void notifyChatsChanged(const std::vector<DmLink> &links);

class ChatService
{
  public:
    drogon::Task<std::vector<ChatSummary>> list(int64_t userId);
    drogon::Task<ChatSummary> openDm(int64_t userId, int64_t peerId);
    drogon::Task<ChatSummary> accept(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> restrictChat(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> unrestrictChat(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> pin(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> unpin(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> setMuted(int64_t chatId, int64_t userId, bool muted);
    drogon::Task<> remove(int64_t chatId, int64_t userId);

    drogon::Task<std::vector<messages::Message>> messages(int64_t chatId, int64_t userId, int64_t beforeId, int limit);
    drogon::Task<std::vector<messages::Message>> messagesAfter(int64_t chatId, int64_t userId, int64_t afterId, int limit);
    drogon::Task<messages::Message> send(int64_t chatId,
                                         int64_t userId,
                                         int64_t deviceId,
                                         std::string ciphertext,
                                         bool allowReactions = true,
                                         bool allowComments = true,
                                         messages::Extras extras = {});
    drogon::Task<Forwarded> forwardFrom(int64_t sourceChatId, int64_t userId, int64_t messageId);
    drogon::Task<std::optional<messages::Message>> pinned(int64_t chatId, int64_t userId);
    drogon::Task<messages::Message> pinMessage(int64_t chatId, int64_t userId, int64_t messageId);
    drogon::Task<> unpinMessage(int64_t chatId, int64_t userId);
    drogon::Task<Json::Value> place(int64_t chatId, int64_t userId);
    drogon::Task<bool> isChannel(int64_t chatId, int64_t userId);
    drogon::Task<messages::Message> edit(int64_t chatId, int64_t userId, int64_t messageId, std::string ciphertext);
    drogon::Task<bool> deleteMessage(int64_t chatId, int64_t userId, int64_t messageId);
    drogon::Task<> markRead(int64_t chatId, int64_t userId, int64_t messageId);

  private:
    drogon::Task<Membership> requireMember(int64_t chatId, int64_t userId);
    drogon::Task<> requireReadable(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> ownChanged(int64_t chatId, int64_t userId);
    drogon::Task<ChatSummary> changeStatus(int64_t chatId, int64_t userId, std::string from, std::string to);

    ChatRepository chats_;
    messages::MessageRepository messages_;
    users::UserRepository users_;
    rooms::RoomRepository roomAccess_;
    rooms::RoomService rooms_;
};
}
