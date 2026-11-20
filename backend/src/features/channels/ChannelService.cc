#include "features/channels/ChannelService.h"
#include "features/rooms/Authority.h"

#include "core/http/ApiError.h"
#include "core/http/Params.h"
#include "core/id/Snowflake.h"
#include "core/text/Text.h"
#include "features/realtime/Hub.h"

#include <algorithm>
#include <set>

namespace features::channels
{
namespace
{
using core::http::ApiError;

constexpr size_t kMaxName = 32;
constexpr int64_t kMaxChannels = 50;
constexpr int64_t kMaxCategories = 20;

ApiError notFound()
{
    return ApiError(drogon::k404NotFound, "room_not_found", "Not found");
}

ApiError invalidName()
{
    return ApiError(drogon::k400BadRequest, "invalid_name", "Enter a name of 1 to 32 characters");
}

std::string channelNameFrom(const Json::Value &value)
{
    const auto name = cleanChannelName(value.isString() ? value.asString() : std::string());
    const auto length = core::text::characterCount(name);
    if (length < 1 || length > kMaxName)
        throw invalidName();
    return name;
}

std::string categoryNameFrom(const Json::Value &value)
{
    const auto name = core::text::trim(value.isString() ? value.asString() : std::string());
    const auto length = core::text::characterCount(name);
    if (length < 1 || length > kMaxName)
        throw invalidName();
    return name;
}

const std::vector<std::string> kChannelKeys = {
    "viewChannel", "sendMessages", "deleteMessages", "manageChannel", "managePermissions"};

ApiError notAllowed()
{
    return ApiError(drogon::k403Forbidden, "not_allowed", "You do not have permission to do this");
}

bool contains(const std::vector<std::string> &list, const std::string &key)
{
    return std::find(list.begin(), list.end(), key) != list.end();
}

Json::Value keysJson(const std::vector<std::string> &keys)
{
    Json::Value json(Json::arrayValue);
    for (const auto &key : keys)
        json.append(key);
    return json;
}

std::vector<std::string> keysFrom(const Json::Value &value)
{
    if (!value.isNull() && !value.isArray())
        throw ApiError(drogon::k400BadRequest, "invalid_permission", "Unknown permission");
    std::set<std::string> keys;
    for (const auto &item : value)
    {
        if (!item.isString() || !contains(kChannelKeys, item.asString()))
            throw ApiError(drogon::k400BadRequest, "invalid_permission", "Unknown permission");
        keys.insert(item.asString());
    }
    std::vector<std::string> ordered;
    for (const auto &key : kChannelKeys)
        if (keys.contains(key))
            ordered.push_back(key);
    return ordered;
}

int stateOf(const Override *item, const std::string &key)
{
    if (!item)
        return 0;
    if (contains(item->allow, key))
        return 1;
    if (contains(item->deny, key))
        return -1;
    return 0;
}

bool moveUp(const Json::Value &value)
{
    const auto move = value.asString();
    if (move != "up" && move != "down")
        throw ApiError(drogon::k400BadRequest, "invalid_move", "Unknown move");
    return move == "up";
}
}

std::string cleanChannelName(const std::string &text)
{
    std::string name;
    for (const unsigned char c : core::text::trim(text))
    {
        if (c == ' ')
            name += '-';
        else if (c >= 'A' && c <= 'Z')
            name += static_cast<char>(c - 'A' + 'a');
        else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c >= 0x80)
            name += static_cast<char>(c);
    }
    return name;
}

Json::Value toJson(const Category &category)
{
    Json::Value json;
    json["id"] = std::to_string(category.id);
    json["name"] = category.name;
    json["position"] = category.position;
    json["permissions"] = keysJson(category.permissions);
    return json;
}

Json::Value toJson(const Channel &channel)
{
    Json::Value json;
    json["id"] = std::to_string(channel.id);
    json["name"] = channel.name;
    json["categoryId"] = channel.categoryId ? Json::Value(std::to_string(*channel.categoryId)) : Json::Value();
    json["position"] = channel.position;
    json["unreadCount"] = static_cast<Json::Int64>(channel.unreadCount);
    json["lastReadId"] = std::to_string(channel.lastReadId);
    json["readUpTo"] = std::to_string(channel.readUpTo);
    json["lastMessage"] = channel.lastMessage ? messages::toJson(*channel.lastMessage) : Json::Value();
    json["permissions"] = keysJson(channel.permissions);
    json["private"] = channel.isPrivate;
    json["synced"] = channel.synced;
    return json;
}

drogon::Task<Json::Value> ChannelService::list(int64_t roomId, int64_t userId)
{
    const auto room = co_await requireMember(roomId, userId);
    const bool manager = rooms::authorityOf(room).has("manageChannels");
    const auto visible = co_await channels_.channels(roomId, userId);
    std::set<int64_t> used;
    Json::Value channels(Json::arrayValue);
    for (const auto &channel : visible)
    {
        if (channel.categoryId)
            used.insert(*channel.categoryId);
        channels.append(toJson(channel));
    }
    Json::Value categories(Json::arrayValue);
    for (const auto &category : co_await channels_.categories(roomId, userId))
        if (manager || used.contains(category.id))
            categories.append(toJson(category));
    Json::Value json;
    json["categories"] = categories;
    json["channels"] = channels;
    co_return json;
}

drogon::Task<Channel> ChannelService::create(int64_t roomId, int64_t userId, Json::Value body)
{
    co_await requireManager(roomId, userId);
    const auto name = channelNameFrom(body["name"]);
    const auto categoryId = co_await categoryFrom(roomId, body["categoryId"]);
    if (co_await channels_.channelCount(roomId) >= kMaxChannels)
        throw ApiError(drogon::k409Conflict, "too_many", "A group can have up to 50 channels");
    const auto id = core::id::next();
    co_await channels_.createChannel(id, roomId, categoryId, name);
    co_await notify(roomId);
    co_return co_await requireChannel(roomId, id, userId);
}

drogon::Task<Channel> ChannelService::update(int64_t roomId, int64_t channelId, int64_t userId, Json::Value body)
{
    co_await requireMember(roomId, userId);
    if (!contains((co_await requireChannel(roomId, channelId, userId)).permissions, "manageChannel"))
        throw notAllowed();
    if (body.isMember("name"))
        co_await channels_.renameChannel(channelId, channelNameFrom(body["name"]));
    if (body.isMember("categoryId"))
        co_await channels_.setCategory(roomId, channelId, co_await categoryFrom(roomId, body["categoryId"]));
    if (body.isMember("move"))
        co_await channels_.moveChannel(roomId, channelId, moveUp(body["move"]));
    co_await notify(roomId);
    co_return co_await requireChannel(roomId, channelId, userId);
}

drogon::Task<> ChannelService::remove(int64_t roomId, int64_t channelId, int64_t userId)
{
    co_await requireMember(roomId, userId);
    if (!contains((co_await requireChannel(roomId, channelId, userId)).permissions, "manageChannel"))
        throw notAllowed();
    if (channelId == roomId)
        throw ApiError(drogon::k409Conflict, "default_channel", "This channel cannot be deleted");
    co_await channels_.removeChannel(channelId);
    co_await notify(roomId);
}

drogon::Task<Category> ChannelService::createCategory(int64_t roomId, int64_t userId, Json::Value body)
{
    co_await requireManager(roomId, userId);
    const auto name = categoryNameFrom(body["name"]);
    if (co_await channels_.categoryCount(roomId) >= kMaxCategories)
        throw ApiError(drogon::k409Conflict, "too_many", "A group can have up to 20 categories");
    const auto id = core::id::next();
    co_await channels_.createCategory(id, roomId, name);
    co_await notify(roomId);
    co_return co_await requireCategory(roomId, id);
}

drogon::Task<Category> ChannelService::updateCategory(int64_t roomId, int64_t categoryId, int64_t userId, Json::Value body)
{
    co_await requireManager(roomId, userId);
    co_await requireCategory(roomId, categoryId);
    if (body.isMember("name"))
        co_await channels_.renameCategory(categoryId, categoryNameFrom(body["name"]));
    if (body.isMember("move"))
        co_await channels_.moveCategory(roomId, categoryId, moveUp(body["move"]));
    co_await notify(roomId);
    co_return co_await requireCategory(roomId, categoryId);
}

drogon::Task<> ChannelService::removeCategory(int64_t roomId, int64_t categoryId, int64_t userId)
{
    co_await requireManager(roomId, userId);
    co_await requireCategory(roomId, categoryId);
    co_await channels_.removeCategory(categoryId);
    co_await notify(roomId);
}

drogon::Task<rooms::RoomAccess> ChannelService::requireMember(int64_t roomId, int64_t userId)
{
    const auto room = co_await rooms_.access(roomId, userId);
    if (!room || room->kind != "group" || room->roomId != roomId || room->status != "active")
        throw notFound();
    co_return *room;
}

drogon::Task<> ChannelService::requireManager(int64_t roomId, int64_t userId)
{
    const auto room = co_await requireMember(roomId, userId);
    if (!rooms::authorityOf(room).has("manageChannels"))
        throw ApiError(drogon::k403Forbidden, "not_allowed", "You do not have permission to do this");
}

drogon::Task<Channel> ChannelService::requireChannel(int64_t roomId, int64_t channelId, int64_t userId)
{
    const auto channel = co_await channels_.channel(roomId, channelId, userId);
    if (!channel)
        throw ApiError(drogon::k404NotFound, "channel_not_found", "Channel not found");
    co_return *channel;
}

drogon::Task<Category> ChannelService::requireCategory(int64_t roomId, int64_t categoryId)
{
    const auto category = co_await channels_.category(roomId, categoryId);
    if (!category)
        throw ApiError(drogon::k404NotFound, "category_not_found", "Category not found");
    co_return *category;
}

drogon::Task<int64_t> ChannelService::categoryFrom(int64_t roomId, const Json::Value &value)
{
    if (!value.isString() || value.asString().empty())
        co_return 0;
    const auto id = core::http::parseId(value.asString(), "categoryId");
    if (!co_await channels_.category(roomId, id))
        throw ApiError(drogon::k400BadRequest, "invalid_category", "Choose a category of this group");
    co_return id;
}

drogon::Task<> ChannelService::notify(int64_t roomId)
{
    Json::Value data;
    data["chatId"] = std::to_string(roomId);
    const auto ids = co_await rooms_.activeMemberIds(roomId);
    for (const auto id : ids)
        realtime::hub().send(id, "room.channels", data);
}

Json::Value ChannelService::permissionsJson(const std::vector<Override> &overrides, const std::vector<std::string> &permissions)
{
    Json::Value json;
    json["overrides"] = Json::Value(Json::arrayValue);
    for (const auto &item : overrides)
    {
        Json::Value entry;
        entry["targetType"] = item.targetType;
        entry["targetId"] = std::to_string(item.targetId);
        entry["allow"] = keysJson(item.allow);
        entry["deny"] = keysJson(item.deny);
        json["overrides"].append(entry);
    }
    json["permissions"] = keysJson(permissions);
    return json;
}

drogon::Task<Override> ChannelService::checkOverride(int64_t roomId,
                                                     const rooms::Authority &authority,
                                                     const std::vector<std::string> &scopePermissions,
                                                     const std::vector<Override> &current,
                                                     const std::string &targetType,
                                                     const std::string &targetId,
                                                     const Json::Value &body)
{
    const bool full = authority.has("administrator");
    if (!full && !contains(scopePermissions, "managePermissions"))
        throw notAllowed();

    Override item{targetType, 0, keysFrom(body["allow"]), keysFrom(body["deny"])};
    for (const auto &key : item.allow)
        if (contains(item.deny, key))
            throw ApiError(drogon::k400BadRequest, "invalid_permission", "A permission cannot be both allowed and denied");

    if (targetType == "everyone")
    {
        if (targetId != "0")
            throw ApiError(drogon::k400BadRequest, "invalid_target", "Unknown target");
    }
    else if (targetType == "role")
    {
        item.targetId = core::http::parseId(targetId, "targetId");
        const auto role = co_await roles_.find(roomId, item.targetId);
        if (!role)
            throw ApiError(drogon::k400BadRequest, "invalid_target", "Unknown role");
        if (!full && !authority.above(role->position))
            throw notAllowed();
    }
    else if (targetType == "member")
    {
        item.targetId = core::http::parseId(targetId, "targetId");
        const auto target = co_await rooms_.access(roomId, item.targetId);
        if (!target || target->status != "active" || target->roomId != roomId)
            throw ApiError(drogon::k400BadRequest, "invalid_target", "Choose a current member");
        const auto targetAuthority = rooms::authorityOf(*target);
        if (targetAuthority.owner)
            throw notAllowed();
        if (!authority.owner && !authority.outranks(false, targetAuthority.rank))
            throw notAllowed();
    }
    else
    {
        throw ApiError(drogon::k400BadRequest, "invalid_target", "Unknown target");
    }

    if (!full)
    {
        const Override *before = nullptr;
        for (const auto &existing : current)
            if (existing.targetType == item.targetType && existing.targetId == item.targetId)
                before = &existing;
        for (const auto &key : kChannelKeys)
            if (stateOf(before, key) != stateOf(&item, key) && !contains(scopePermissions, key))
                throw notAllowed();
    }
    co_return item;
}

drogon::Task<Json::Value> ChannelService::channelPermissions(int64_t roomId, int64_t channelId, int64_t userId)
{
    co_await requireMember(roomId, userId);
    const auto channel = co_await requireChannel(roomId, channelId, userId);
    if (!contains(channel.permissions, "viewChannel"))
        throw ApiError(drogon::k404NotFound, "channel_not_found", "Channel not found");
    const auto scope = channel.synced && channel.categoryId ? *channel.categoryId : channelId;
    auto json = permissionsJson(co_await channels_.overrides(scope), channel.permissions);
    json["synced"] = channel.synced && channel.categoryId.has_value();
    json["categoryId"] = channel.categoryId ? Json::Value(std::to_string(*channel.categoryId)) : Json::Value();
    co_return json;
}

drogon::Task<Json::Value> ChannelService::setChannelOverride(int64_t roomId,
                                                             int64_t channelId,
                                                             int64_t userId,
                                                             std::string targetType,
                                                             std::string targetId,
                                                             Json::Value body)
{
    const auto room = co_await requireMember(roomId, userId);
    const auto channel = co_await requireChannel(roomId, channelId, userId);
    const bool synced = channel.synced && channel.categoryId.has_value();
    const auto scope = synced ? *channel.categoryId : channelId;
    const auto current = co_await channels_.overrides(scope);
    const auto item = co_await checkOverride(
        roomId, rooms::authorityOf(room), channel.permissions, current, targetType, targetId, body);
    if (synced)
        co_await channels_.copyOverrides(scope, channelId, roomId);
    co_await channels_.setSynced(channelId, false);
    co_await channels_.setOverride(channelId, roomId, item);
    co_await notify(roomId);
    co_return co_await channelPermissions(roomId, channelId, userId);
}

drogon::Task<Json::Value> ChannelService::syncChannel(int64_t roomId, int64_t channelId, int64_t userId)
{
    const auto room = co_await requireMember(roomId, userId);
    const auto channel = co_await requireChannel(roomId, channelId, userId);
    if (!rooms::authorityOf(room).has("administrator") && !contains(channel.permissions, "managePermissions"))
        throw notAllowed();
    if (!channel.categoryId)
        throw ApiError(drogon::k409Conflict, "no_category", "This channel is not in a category");
    co_await channels_.clearOverrides(channelId);
    co_await channels_.setSynced(channelId, true);
    co_await notify(roomId);
    co_return co_await channelPermissions(roomId, channelId, userId);
}

drogon::Task<Json::Value> ChannelService::categoryPermissions(int64_t roomId, int64_t categoryId, int64_t userId)
{
    co_await requireManager(roomId, userId);
    const auto category = co_await channels_.category(roomId, categoryId, userId);
    if (!category)
        throw ApiError(drogon::k404NotFound, "category_not_found", "Category not found");
    co_return permissionsJson(co_await channels_.overrides(categoryId), category->permissions);
}

drogon::Task<Json::Value> ChannelService::setCategoryOverride(int64_t roomId,
                                                              int64_t categoryId,
                                                              int64_t userId,
                                                              std::string targetType,
                                                              std::string targetId,
                                                              Json::Value body)
{
    co_await requireManager(roomId, userId);
    const auto room = co_await requireMember(roomId, userId);
    const auto category = co_await channels_.category(roomId, categoryId, userId);
    if (!category)
        throw ApiError(drogon::k404NotFound, "category_not_found", "Category not found");
    const auto current = co_await channels_.overrides(categoryId);
    const auto item = co_await checkOverride(
        roomId, rooms::authorityOf(room), category->permissions, current, targetType, targetId, body);
    co_await channels_.setOverride(categoryId, roomId, item);
    co_await notify(roomId);
    co_return co_await categoryPermissions(roomId, categoryId, userId);
}
}
