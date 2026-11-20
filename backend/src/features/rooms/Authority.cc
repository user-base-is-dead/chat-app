#include "features/rooms/Authority.h"

#include <algorithm>
#include <sstream>

namespace features::rooms
{
const std::vector<std::string> kPermissionKeys = {
    "manageGroup",
    "manageChannels",
    "manageRoles",
    "addMembers",
    "removeMembers",
    "deleteMessages",
    "sendAlways",
    "administrator",
};

bool Authority::has(const std::string &permission) const
{
    return owner || permissions.contains(permission);
}

bool Authority::above(std::optional<int> position) const
{
    if (owner)
        return true;
    if (!rank)
        return false;
    return !position || *rank < *position;
}

bool Authority::outranks(bool otherOwner, std::optional<int> otherRank) const
{
    if (otherOwner)
        return false;
    return above(otherRank);
}

bool isPermissionKey(const std::string &key)
{
    return std::find(kPermissionKeys.begin(), kPermissionKeys.end(), key) != kPermissionKeys.end();
}

std::vector<std::string> splitKeys(const std::string &joined)
{
    std::vector<std::string> keys;
    std::stringstream stream(joined);
    std::string key;
    while (std::getline(stream, key, ','))
        if (!key.empty())
            keys.push_back(key);
    return keys;
}

Authority authorityOf(const std::string &kind, const std::string &role, const std::vector<std::string> &rolePermissions,
                      std::optional<int> rank)
{
    Authority authority;
    authority.owner = role == "owner";
    const auto all = [&authority] { authority.permissions.insert(kPermissionKeys.begin(), kPermissionKeys.end()); };
    if (authority.owner)
    {
        all();
        return authority;
    }
    if (kind != "group")
    {
        if (role == "admin")
        {
            all();
            authority.rank = 0;
        }
        return authority;
    }
    authority.rank = rank;
    for (const auto &key : rolePermissions)
        if (isPermissionKey(key))
            authority.permissions.insert(key);
    if (authority.permissions.contains("administrator"))
        all();
    return authority;
}

Authority authorityOf(const RoomAccess &room)
{
    return authorityOf(room.kind, room.role, room.rolePermissions, room.rank);
}

Authority authorityOf(const RoomSummary &room)
{
    return authorityOf(room.kind, room.role, room.rolePermissions, room.rank);
}
}
