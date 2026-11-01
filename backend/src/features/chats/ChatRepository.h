#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

#include "features/messages/Message.h"
#include "features/users/UserRepository.h"

namespace features::chats
{
struct ChatSummary
{
    int64_t id;
    std::string status;
    users::User peer;
    int64_t lastReadId;
    int64_t peerLastReadId;
    std::optional<messages::Message> lastMessage;
    std::optional<int64_t> peerLastSeenAt;
    bool peerVisible;
    int64_t unreadCount;
    std::optional<int64_t> pinnedAt;
    bool muted;
    bool peerDisabled;
    bool peerDeleted;
};

struct DmLink
{
    int64_t chatId;
    int64_t peerId;
};

struct Membership
{
    std::string status;
    int64_t peerId;
};

class ChatRepository
{
  public:
    drogon::Task<std::vector<ChatSummary>> listForUser(int64_t userId);
    drogon::Task<std::optional<ChatSummary>> findForUser(int64_t chatId, int64_t userId);
    drogon::Task<std::optional<int64_t>> findDm(int64_t userA, int64_t userB);
    drogon::Task<int64_t> createDm(int64_t chatId, int64_t creatorId, int64_t peerId);
    drogon::Task<> ensureMember(int64_t chatId, int64_t userId, std::string status);
    drogon::Task<> addRecipient(int64_t chatId, int64_t userId);
    drogon::Task<std::optional<Membership>> membership(int64_t chatId, int64_t userId);
    drogon::Task<bool> accept(int64_t chatId, int64_t userId);
    drogon::Task<bool> changeStatus(int64_t chatId, int64_t userId, std::string from, std::string to);
    drogon::Task<bool> pin(int64_t chatId, int64_t userId, int64_t limit);
    drogon::Task<> unpin(int64_t chatId, int64_t userId);
    drogon::Task<> setMuted(int64_t chatId, int64_t userId, bool muted);
    drogon::Task<> remove(int64_t chatId);
    drogon::Task<bool> removeIfEmpty(int64_t chatId);
    drogon::Task<> markRead(int64_t chatId, int64_t userId, int64_t messageId);
    drogon::Task<std::vector<int64_t>> peerIds(int64_t userId);
    drogon::Task<std::vector<int64_t>> allPeerIds(int64_t userId);
    drogon::Task<std::vector<DmLink>> dmsOf(int64_t userId);
};
}
