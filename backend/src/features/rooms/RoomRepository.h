#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

#include "features/messages/Message.h"

namespace features::rooms
{
struct RoomAccess
{
    std::string kind;
    std::string status;
    std::string role;
    bool isPublic;
    bool membersCanSend;
    bool membersCanAdd;
    int64_t roomId;
    std::vector<std::string> rolePermissions;
    std::optional<int> rank;
    std::optional<std::vector<std::string>> channelPermissions;

    bool canInChannel(const std::string &key) const;
};

struct RoomFields
{
    std::string title;
    std::string about;
    std::string username;
    bool isPublic;
    int64_t photoId;
    bool membersCanSend;
    bool membersCanAdd;
};

struct RoomSummary
{
    int64_t id;
    std::string kind;
    std::string title;
    std::string about;
    std::string username;
    bool isPublic;
    std::string photo;
    std::string inviteCode;
    bool membersCanSend;
    bool membersCanAdd;
    std::string status;
    std::string role;
    int64_t lastReadId;
    int64_t readUpTo;
    int64_t unreadCount;
    int64_t memberCount;
    std::optional<int64_t> pinnedAt;
    bool muted;
    std::optional<messages::Message> lastMessage;
    std::vector<std::string> rolePermissions;
    std::optional<int> rank;
};

struct RoomPreview
{
    int64_t id;
    std::string kind;
    std::string title;
    std::string about;
    std::string username;
    bool isPublic;
    std::string photo;
    int64_t memberCount;
    std::string status;
};

struct RoomMember
{
    int64_t id;
    std::string username;
    std::string displayName;
    std::string role;
    std::string status;
    bool disabled;
    bool deleted;
    bool showsLastSeen;
    std::vector<int64_t> roleIds;
};

class RoomRepository
{
  public:
    drogon::Task<std::optional<RoomAccess>> access(int64_t chatId, int64_t userId);
    drogon::Task<std::vector<RoomSummary>> listForUser(int64_t userId);
    drogon::Task<std::optional<RoomSummary>> findForUser(int64_t chatId, int64_t userId);
    drogon::Task<bool> create(int64_t chatId, std::string kind, RoomFields fields, std::string inviteCode, int64_t ownerId);
    drogon::Task<bool> update(int64_t chatId, RoomFields fields);
    drogon::Task<> setInviteCode(int64_t chatId, std::string code);
    drogon::Task<int64_t> photoOf(int64_t chatId);
    drogon::Task<bool> usernameFree(std::string username, int64_t chatId);
    drogon::Task<bool> ownsPhoto(int64_t photoId, int64_t userId);
    drogon::Task<std::vector<RoomPreview>> search(std::string query, std::string kind, int64_t userId, int limit);
    drogon::Task<std::optional<RoomPreview>> findByKey(std::string key, int64_t userId);
    drogon::Task<std::vector<RoomMember>> members(int64_t chatId, int64_t limit, int64_t offset, int64_t channelId = 0);
    drogon::Task<std::vector<int64_t>> viewerIds(int64_t roomId, int64_t channelId);
    drogon::Task<std::vector<int64_t>> activeMemberIds(int64_t chatId);
    drogon::Task<std::vector<int64_t>> allMemberIds(int64_t chatId);
    drogon::Task<std::vector<int64_t>> roomsOf(int64_t userId);
    drogon::Task<std::optional<std::string>> memberRole(int64_t chatId, int64_t userId);
    drogon::Task<bool> addMember(int64_t chatId, int64_t userId, std::string status);
    drogon::Task<bool> acceptInvite(int64_t chatId, int64_t userId);
    drogon::Task<> removeMember(int64_t chatId, int64_t userId);
    drogon::Task<> setRole(int64_t chatId, int64_t userId, std::string role);
    drogon::Task<> transfer(int64_t chatId, int64_t ownerId, int64_t userId);
    drogon::Task<> ban(int64_t chatId, int64_t userId);
    drogon::Task<> unban(int64_t chatId, int64_t userId);
    drogon::Task<bool> isBanned(int64_t chatId, int64_t userId);
    drogon::Task<bool> restrictedBy(int64_t userId, int64_t adderId);
    drogon::Task<std::optional<int64_t>> messageSender(int64_t chatId, int64_t messageId);
    drogon::Task<int64_t> onlineCount(int64_t chatId);
    drogon::Task<int64_t> roomIdOf(int64_t chatId);
    drogon::Task<std::vector<int64_t>> groupMateIds(int64_t userId);
    drogon::Task<> markChannelRead(int64_t chatId, int64_t userId, int64_t messageId);
    drogon::Task<> addDefaultChannel(int64_t roomId);
    drogon::Task<> addDefaultRole(int64_t roomId, int64_t roleId);
    drogon::Task<> giveDefaultRole(int64_t roomId, int64_t userId);
    drogon::Task<> removeChannels(int64_t roomId);
};
}
