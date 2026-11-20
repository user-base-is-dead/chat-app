#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>
#include <json/value.h>

#include "features/chats/ChatRepository.h"
#include "features/messages/MessageRepository.h"
#include "features/rooms/RoomRepository.h"
#include "features/users/UserRepository.h"

namespace features::rooms
{
struct AddResult
{
    int added = 0;
    int invited = 0;
    int skipped = 0;
};

Json::Value toJson(const RoomSummary &room);
Json::Value toJson(const RoomPreview &room);
Json::Value toJson(const RoomMember &member);
void hideSender(Json::Value &message);
drogon::Task<> notifyRoomChanged(int64_t chatId);

class RoomService
{
  public:
    drogon::Task<std::vector<RoomSummary>> list(int64_t userId);
    drogon::Task<Json::Value> create(int64_t userId, std::string kind, RoomFields fields, std::vector<int64_t> memberIds);
    drogon::Task<Json::Value> detail(int64_t chatId, int64_t userId);
    drogon::Task<RoomSummary> update(int64_t chatId, int64_t userId, Json::Value body);
    drogon::Task<> remove(int64_t chatId, int64_t userId);
    drogon::Task<std::vector<RoomPreview>> search(std::string query, std::string kind, int64_t userId);
    drogon::Task<RoomPreview> preview(std::string key, int64_t userId);
    drogon::Task<RoomSummary> join(std::string key, int64_t userId);
    drogon::Task<RoomSummary> accept(int64_t chatId, int64_t userId);
    drogon::Task<> decline(int64_t chatId, int64_t userId);
    drogon::Task<> leave(int64_t chatId, int64_t userId);
    drogon::Task<RoomSummary> setMuted(int64_t chatId, int64_t userId, bool muted);
    drogon::Task<RoomSummary> setPinned(int64_t chatId, int64_t userId, bool pinned);
    drogon::Task<RoomSummary> resetInvite(int64_t chatId, int64_t userId);
    drogon::Task<> transfer(int64_t chatId, int64_t userId, int64_t targetId);
    drogon::Task<std::vector<RoomMember>> members(int64_t chatId, int64_t userId, int64_t offset, int64_t channelId = 0);
    drogon::Task<AddResult> addMembers(int64_t chatId, int64_t userId, std::vector<int64_t> ids);
    drogon::Task<> setRole(int64_t chatId, int64_t userId, int64_t targetId, std::string role);
    drogon::Task<> removeMember(int64_t chatId, int64_t userId, int64_t targetId);

    drogon::Task<messages::Message> send(int64_t chatId,
                                         RoomAccess room,
                                         int64_t userId,
                                         int64_t deviceId,
                                         std::string ciphertext,
                                         bool allowReactions = true,
                                         bool allowComments = true);
    drogon::Task<messages::Message> edit(int64_t chatId, RoomAccess room, int64_t userId, int64_t messageId, std::string ciphertext);
    drogon::Task<> deleteMessage(int64_t chatId, RoomAccess room, int64_t userId, int64_t messageId);
    drogon::Task<> markRead(int64_t chatId, RoomAccess room, int64_t userId, int64_t messageId);

  private:
    drogon::Task<RoomAccess> requireRoom(int64_t chatId, int64_t userId);
    drogon::Task<RoomAccess> requireActive(int64_t chatId, int64_t userId);
    drogon::Task<RoomAccess> requirePermission(int64_t chatId, int64_t userId, const std::string &permission);
    drogon::Task<RoomAccess> requireOwner(int64_t chatId, int64_t userId);
    drogon::Task<RoomSummary> summary(int64_t chatId, int64_t userId);
    drogon::Task<> validate(RoomFields &fields, int64_t chatId, int64_t userId, int64_t currentPhotoId);
    drogon::Task<AddResult> addAll(int64_t chatId, RoomAccess room, int64_t userId, std::vector<int64_t> ids);
    drogon::Task<> broadcast(int64_t chatId, std::string event, Json::Value data);
    drogon::Task<> broadcastPost(int64_t chatId, int64_t senderId, std::string event, Json::Value data);

    RoomRepository rooms_;
    chats::ChatRepository chats_;
    messages::MessageRepository messages_;
    users::UserRepository users_;
};
}
