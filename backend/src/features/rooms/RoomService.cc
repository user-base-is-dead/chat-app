#include "features/rooms/RoomService.h"

#include "core/http/ApiError.h"
#include "core/http/Params.h"
#include "core/id/Snowflake.h"
#include "core/security/Tokens.h"
#include "core/text/Text.h"
#include "features/realtime/Hub.h"
#include "features/users/Username.h"

namespace features::rooms
{
namespace
{
using core::http::ApiError;
using core::text::characterCount;
using core::text::hasStrikeMarks;
using core::text::isReservedName;
using core::text::trim;

constexpr size_t kMaxTitle = 64;
constexpr size_t kMaxAbout = 255;
constexpr size_t kMaxAddAtOnce = 50;
constexpr int64_t kMembersPage = 100;
constexpr int64_t kPinLimit = 5;
constexpr int kSearchLimit = 20;

ApiError roomNotFound()
{
    return ApiError(drogon::k404NotFound, "room_not_found", "Not found");
}

ApiError notAllowed()
{
    return ApiError(drogon::k403Forbidden, "not_allowed", "You do not have permission to do this");
}

bool isAdminRole(const std::string &role)
{
    return role == "owner" || role == "admin";
}

bool canSend(const std::string &kind, const std::string &role, bool membersCanSend)
{
    return isAdminRole(role) || (kind == "group" && membersCanSend);
}

bool canAdd(const std::string &kind, const std::string &role, bool membersCanAdd)
{
    return isAdminRole(role) || (kind == "group" && membersCanAdd);
}

Json::Value chatEvent(int64_t chatId)
{
    Json::Value data;
    data["chatId"] = std::to_string(chatId);
    return data;
}

void requireReader(const RoomAccess &room)
{
    if (room.status != "active")
        throw roomNotFound();
}
}

Json::Value toJson(const RoomSummary &room)
{
    const bool active = room.status == "active";
    Json::Value json;
    json["id"] = std::to_string(room.id);
    json["kind"] = room.kind;
    json["title"] = room.title;
    json["about"] = room.about;
    json["username"] = room.username;
    json["isPublic"] = room.isPublic;
    json["photo"] = room.photo;
    json["inviteCode"] = active && isAdminRole(room.role) ? room.inviteCode : "";
    json["membersCanSend"] = room.membersCanSend;
    json["membersCanAdd"] = room.membersCanAdd;
    json["status"] = room.status;
    json["role"] = room.role;
    json["canSend"] = active && canSend(room.kind, room.role, room.membersCanSend);
    json["canAdd"] = active && canAdd(room.kind, room.role, room.membersCanAdd);
    json["lastReadId"] = std::to_string(room.lastReadId);
    json["readUpTo"] = std::to_string(room.kind == "group" ? room.readUpTo : 0);
    json["unreadCount"] = static_cast<Json::Int64>(active ? room.unreadCount : 0);
    json["memberCount"] = static_cast<Json::Int64>(room.memberCount);
    json["pinnedAt"] = room.pinnedAt ? Json::Value(static_cast<Json::Int64>(*room.pinnedAt)) : Json::Value();
    json["muted"] = room.muted;
    json["lastMessage"] = active && room.lastMessage ? messages::toJson(*room.lastMessage) : Json::Value();
    return json;
}

Json::Value toJson(const RoomPreview &room)
{
    Json::Value json;
    json["id"] = std::to_string(room.id);
    json["kind"] = room.kind;
    json["title"] = room.title;
    json["about"] = room.about;
    json["username"] = room.username;
    json["isPublic"] = room.isPublic;
    json["photo"] = room.photo;
    json["memberCount"] = static_cast<Json::Int64>(room.memberCount);
    json["status"] = room.status;
    return json;
}

Json::Value toJson(const RoomMember &member)
{
    Json::Value json;
    json["id"] = std::to_string(member.id);
    json["username"] = member.deleted ? "" : member.username;
    json["displayName"] = member.deleted ? "" : member.displayName;
    json["role"] = member.role;
    json["status"] = member.status;
    json["disabled"] = member.disabled;
    json["deleted"] = member.deleted;
    return json;
}

drogon::Task<> notifyRoomChanged(int64_t chatId)
{
    const auto data = chatEvent(chatId);
    const auto ids = co_await RoomRepository().allMemberIds(chatId);
    for (const auto id : ids)
        realtime::hub().send(id, "room.changed", data);
}

drogon::Task<std::vector<RoomSummary>> RoomService::list(int64_t userId)
{
    co_return co_await rooms_.listForUser(userId);
}

drogon::Task<> RoomService::validate(RoomFields &fields, int64_t chatId, int64_t userId, int64_t currentPhotoId)
{
    fields.title = trim(fields.title);
    fields.about = trim(fields.about);
    fields.username = trim(fields.username);
    if (fields.title.empty() || characterCount(fields.title) > kMaxTitle)
        throw ApiError(drogon::k400BadRequest, "invalid_title", "Name must be 1-64 characters");
    if (isReservedName(fields.title))
        throw ApiError(drogon::k400BadRequest, "invalid_title", "This name is reserved");
    if (hasStrikeMarks(fields.title))
        throw ApiError(drogon::k400BadRequest, "invalid_title", "Name cannot contain strikethrough characters");
    if (characterCount(fields.about) > kMaxAbout)
        throw ApiError(drogon::k400BadRequest, "invalid_about", "Description can be at most 255 characters");
    if (fields.photoId != 0 && fields.photoId != currentPhotoId && !co_await rooms_.ownsPhoto(fields.photoId, userId))
        throw ApiError(drogon::k400BadRequest, "invalid_photo", "Photo not found");

    if (!fields.isPublic)
    {
        fields.username.clear();
        co_return;
    }
    users::validateUsername(fields.username);
    if (!co_await rooms_.usernameFree(fields.username, chatId))
        throw ApiError(drogon::k409Conflict, "username_taken", "This username is already taken");
}

drogon::Task<Json::Value> RoomService::create(int64_t userId,
                                              std::string kind,
                                              RoomFields fields,
                                              std::vector<int64_t> memberIds)
{
    if (kind != "group" && kind != "channel")
        throw ApiError(drogon::k400BadRequest, "invalid_kind", "Unknown type");
    if (memberIds.size() > kMaxAddAtOnce)
        throw ApiError(drogon::k400BadRequest, "too_many", "You can add up to 50 people at once");
    co_await validate(fields, 0, userId, 0);

    const auto chatId = core::id::next();
    if (!co_await rooms_.create(chatId, kind, fields, core::security::randomHex(16), userId))
        throw ApiError(drogon::k409Conflict, "username_taken", "This username is already taken");

    const auto room = co_await requireRoom(chatId, userId);
    const auto added = co_await addAll(chatId, room, userId, std::move(memberIds));
    auto json = toJson(co_await summary(chatId, userId));
    json["added"] = added.added;
    json["invited"] = added.invited;
    json["skipped"] = added.skipped;
    co_return json;
}

drogon::Task<Json::Value> RoomService::detail(int64_t chatId, int64_t userId)
{
    co_await requireRoom(chatId, userId);
    auto json = toJson(co_await summary(chatId, userId));
    json["onlineCount"] = static_cast<Json::Int64>(co_await rooms_.onlineCount(chatId));
    co_return json;
}

drogon::Task<RoomSummary> RoomService::update(int64_t chatId, int64_t userId, Json::Value body)
{
    co_await requireAdmin(chatId, userId);
    const auto current = co_await summary(chatId, userId);
    const auto textOr = [&body](const char *name, const std::string &fallback) {
        return body[name].isString() ? body[name].asString() : fallback;
    };
    const auto flagOr = [&body](const char *name, bool fallback) {
        return body[name].isBool() ? body[name].asBool() : fallback;
    };

    const auto currentPhoto = co_await rooms_.photoOf(chatId);
    auto photo = currentPhoto;
    if (body["photoId"].isString())
        photo = core::http::parseOptionalId(body["photoId"].asString(), "photoId");

    RoomFields fields{textOr("title", current.title),
                      textOr("about", current.about),
                      textOr("username", current.username),
                      flagOr("isPublic", current.isPublic),
                      photo,
                      flagOr("membersCanSend", current.membersCanSend),
                      flagOr("membersCanAdd", current.membersCanAdd)};
    co_await validate(fields, chatId, userId, currentPhoto);
    if (!co_await rooms_.update(chatId, fields))
        throw ApiError(drogon::k409Conflict, "username_taken", "This username is already taken");
    co_await notifyRoomChanged(chatId);
    co_return co_await summary(chatId, userId);
}

drogon::Task<> RoomService::remove(int64_t chatId, int64_t userId)
{
    co_await requireOwner(chatId, userId);
    const auto ids = co_await rooms_.allMemberIds(chatId);
    co_await chats_.remove(chatId);
    const auto data = chatEvent(chatId);
    for (const auto id : ids)
        realtime::hub().send(id, "room.removed", data);
}

drogon::Task<std::vector<RoomPreview>> RoomService::search(std::string query, std::string kind, int64_t userId)
{
    co_return co_await rooms_.search(std::move(query), std::move(kind), userId, kSearchLimit);
}

drogon::Task<RoomPreview> RoomService::preview(std::string key, int64_t userId)
{
    auto room = co_await rooms_.findByKey(std::move(key), userId);
    if (!room)
        throw ApiError(drogon::k404NotFound, "invalid_link", "This link is not valid");
    co_return *room;
}

drogon::Task<RoomSummary> RoomService::join(std::string key, int64_t userId)
{
    const auto room = co_await preview(std::move(key), userId);
    if (co_await rooms_.isBanned(room.id, userId))
        throw ApiError(drogon::k403Forbidden, "banned", "You were removed from here");
    if (room.status == "request")
        co_await rooms_.acceptInvite(room.id, userId);
    else if (room.status.empty())
        co_await rooms_.addMember(room.id, userId, "active");
    co_await notifyRoomChanged(room.id);
    co_return co_await summary(room.id, userId);
}

drogon::Task<RoomSummary> RoomService::accept(int64_t chatId, int64_t userId)
{
    if (!co_await rooms_.acceptInvite(chatId, userId))
        throw ApiError(drogon::k409Conflict, "no_invite", "This invite is no longer valid");
    co_await notifyRoomChanged(chatId);
    co_return co_await summary(chatId, userId);
}

drogon::Task<> RoomService::decline(int64_t chatId, int64_t userId)
{
    const auto room = co_await requireRoom(chatId, userId);
    if (room.status != "request")
        throw ApiError(drogon::k409Conflict, "no_invite", "This invite is no longer valid");
    co_await rooms_.removeMember(chatId, userId);
    realtime::hub().send(userId, "room.removed", chatEvent(chatId));
}

drogon::Task<> RoomService::leave(int64_t chatId, int64_t userId)
{
    const auto room = co_await requireActive(chatId, userId);
    if (room.role == "owner")
        throw ApiError(drogon::k409Conflict, "owner_must_transfer", "Transfer ownership before leaving");
    co_await rooms_.removeMember(chatId, userId);
    realtime::hub().send(userId, "room.removed", chatEvent(chatId));
    co_await notifyRoomChanged(chatId);
}

drogon::Task<RoomSummary> RoomService::setMuted(int64_t chatId, int64_t userId, bool muted)
{
    co_await requireActive(chatId, userId);
    co_await chats_.setMuted(chatId, userId, muted);
    realtime::hub().send(userId, "room.changed", chatEvent(chatId));
    co_return co_await summary(chatId, userId);
}

drogon::Task<RoomSummary> RoomService::setPinned(int64_t chatId, int64_t userId, bool pinned)
{
    co_await requireActive(chatId, userId);
    if (!pinned)
        co_await chats_.unpin(chatId, userId);
    else if (!co_await chats_.pin(chatId, userId, kPinLimit))
        throw ApiError(drogon::k409Conflict, "pin_limit", "You can pin up to 5 chats");
    realtime::hub().send(userId, "room.changed", chatEvent(chatId));
    co_return co_await summary(chatId, userId);
}

drogon::Task<RoomSummary> RoomService::resetInvite(int64_t chatId, int64_t userId)
{
    co_await requireAdmin(chatId, userId);
    co_await rooms_.setInviteCode(chatId, core::security::randomHex(16));
    co_await notifyRoomChanged(chatId);
    co_return co_await summary(chatId, userId);
}

drogon::Task<> RoomService::transfer(int64_t chatId, int64_t userId, int64_t targetId)
{
    co_await requireOwner(chatId, userId);
    if (targetId == userId || !co_await rooms_.memberRole(chatId, targetId))
        throw ApiError(drogon::k400BadRequest, "invalid_member", "Choose a current member");
    co_await rooms_.transfer(chatId, userId, targetId);
    co_await notifyRoomChanged(chatId);
}

drogon::Task<std::vector<RoomMember>> RoomService::members(int64_t chatId, int64_t userId, int64_t offset)
{
    co_await requireActive(chatId, userId);
    co_return co_await rooms_.members(chatId, kMembersPage, offset);
}

drogon::Task<AddResult> RoomService::addMembers(int64_t chatId, int64_t userId, std::vector<int64_t> ids)
{
    const auto room = co_await requireActive(chatId, userId);
    if (!canAdd(room.kind, room.role, room.membersCanAdd))
        throw notAllowed();
    if (ids.size() > kMaxAddAtOnce)
        throw ApiError(drogon::k400BadRequest, "too_many", "You can add up to 50 people at once");
    auto result = co_await addAll(chatId, room, userId, std::move(ids));
    co_await notifyRoomChanged(chatId);
    co_return result;
}

drogon::Task<AddResult> RoomService::addAll(int64_t chatId, RoomAccess room, int64_t userId, std::vector<int64_t> ids)
{
    AddResult result;
    const auto data = chatEvent(chatId);
    for (const auto id : ids)
    {
        const auto account = co_await users_.findCredentialsById(id);
        const bool banned = co_await rooms_.isBanned(chatId, id);
        if (id == userId || !account || account->deleted || account->disabled ||
            co_await rooms_.restrictedBy(id, userId) || (banned && !isAdminRole(room.role)))
        {
            ++result.skipped;
            continue;
        }
        if (banned)
            co_await rooms_.unban(chatId, id);
        if (!co_await rooms_.addMember(chatId, id, room.isPublic ? "active" : "request"))
        {
            ++result.skipped;
            continue;
        }
        ++(room.isPublic ? result.added : result.invited);
        realtime::hub().send(id, "room.changed", data);
    }
    co_return result;
}

drogon::Task<> RoomService::setRole(int64_t chatId, int64_t userId, int64_t targetId, std::string role)
{
    co_await requireOwner(chatId, userId);
    if (role != "admin" && role != "member")
        throw ApiError(drogon::k400BadRequest, "invalid_role", "Unknown role");
    if (!co_await rooms_.memberRole(chatId, targetId))
        throw ApiError(drogon::k400BadRequest, "invalid_member", "Choose a current member");
    co_await rooms_.setRole(chatId, targetId, role);
    co_await notifyRoomChanged(chatId);
}

drogon::Task<> RoomService::removeMember(int64_t chatId, int64_t userId, int64_t targetId)
{
    const auto room = co_await requireAdmin(chatId, userId);
    const auto target = co_await rooms_.access(chatId, targetId);
    if (!target || target->status.empty() || targetId == userId)
        throw ApiError(drogon::k400BadRequest, "invalid_member", "Choose a current member");
    if (target->role == "owner" || (target->role == "admin" && room.role != "owner"))
        throw notAllowed();
    co_await rooms_.removeMember(chatId, targetId);
    co_await rooms_.ban(chatId, targetId);
    realtime::hub().send(targetId, "room.removed", chatEvent(chatId));
    co_await notifyRoomChanged(chatId);
}

drogon::Task<messages::Message> RoomService::send(int64_t chatId,
                                                  RoomAccess room,
                                                  int64_t userId,
                                                  int64_t deviceId,
                                                  std::string ciphertext)
{
    requireReader(room);
    if (!canSend(room.kind, room.role, room.membersCanSend))
        throw ApiError(drogon::k403Forbidden, "cannot_send", "You cannot send messages here");
    auto message = co_await messages_.insert(core::id::next(), chatId, userId, deviceId, ciphertext);
    co_await chats_.markRead(chatId, userId, message.id);
    co_await broadcast(chatId, "message.new", messages::toJson(message));
    co_return message;
}

drogon::Task<messages::Message> RoomService::edit(int64_t chatId,
                                                  RoomAccess room,
                                                  int64_t userId,
                                                  int64_t messageId,
                                                  std::string ciphertext)
{
    requireReader(room);
    auto message = co_await messages_.edit(chatId, messageId, userId, ciphertext);
    if (!message)
        throw ApiError(drogon::k404NotFound, "message_not_found", "Message not found");
    co_await broadcast(chatId, "message.edited", messages::toJson(*message));
    co_return *message;
}

drogon::Task<> RoomService::deleteMessage(int64_t chatId, RoomAccess room, int64_t userId, int64_t messageId)
{
    requireReader(room);
    const auto sender = co_await rooms_.messageSender(chatId, messageId);
    if (!sender)
        throw ApiError(drogon::k404NotFound, "message_not_found", "Message not found");
    if (*sender != userId && !isAdminRole(room.role))
        throw notAllowed();
    co_await messages_.remove(chatId, messageId);

    auto data = chatEvent(chatId);
    data["id"] = std::to_string(messageId);
    co_await broadcast(chatId, "message.deleted", data);
}

drogon::Task<> RoomService::markRead(int64_t chatId, RoomAccess room, int64_t userId, int64_t messageId)
{
    requireReader(room);
    co_await chats_.markRead(chatId, userId, messageId);

    auto data = chatEvent(chatId);
    data["userId"] = std::to_string(userId);
    data["upToMessageId"] = std::to_string(messageId);
    realtime::hub().send(userId, "message.read", data);
    if (room.kind != "group")
        co_return;
    const auto sender = co_await rooms_.messageSender(chatId, messageId);
    if (sender && *sender != userId)
        realtime::hub().send(*sender, "message.read", data);
}

drogon::Task<> RoomService::broadcast(int64_t chatId, std::string event, Json::Value data)
{
    data["room"] = true;
    const auto ids = co_await rooms_.activeMemberIds(chatId);
    for (const auto id : ids)
        realtime::hub().send(id, event, data);
}

drogon::Task<RoomAccess> RoomService::requireRoom(int64_t chatId, int64_t userId)
{
    auto room = co_await rooms_.access(chatId, userId);
    if (!room || room->status.empty())
        throw roomNotFound();
    co_return *room;
}

drogon::Task<RoomAccess> RoomService::requireActive(int64_t chatId, int64_t userId)
{
    auto room = co_await requireRoom(chatId, userId);
    requireReader(room);
    co_return room;
}

drogon::Task<RoomAccess> RoomService::requireAdmin(int64_t chatId, int64_t userId)
{
    auto room = co_await requireActive(chatId, userId);
    if (!isAdminRole(room.role))
        throw notAllowed();
    co_return room;
}

drogon::Task<RoomAccess> RoomService::requireOwner(int64_t chatId, int64_t userId)
{
    auto room = co_await requireActive(chatId, userId);
    if (room.role != "owner")
        throw notAllowed();
    co_return room;
}

drogon::Task<RoomSummary> RoomService::summary(int64_t chatId, int64_t userId)
{
    auto room = co_await rooms_.findForUser(chatId, userId);
    if (!room)
        throw roomNotFound();
    co_return *room;
}
}
