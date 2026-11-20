#pragma once

#include <optional>
#include <set>
#include <string>
#include <vector>

#include "features/rooms/RoomRepository.h"

namespace features::rooms
{
extern const std::vector<std::string> kPermissionKeys;

struct Authority
{
    bool owner = false;
    std::set<std::string> permissions;
    std::optional<int> rank;

    bool has(const std::string &permission) const;
    bool above(std::optional<int> position) const;
    bool outranks(bool otherOwner, std::optional<int> otherRank) const;
};

bool isPermissionKey(const std::string &key);
std::vector<std::string> splitKeys(const std::string &joined);
Authority authorityOf(const std::string &kind, const std::string &role, const std::vector<std::string> &rolePermissions,
                      std::optional<int> rank);
Authority authorityOf(const RoomAccess &room);
Authority authorityOf(const RoomSummary &room);
}
