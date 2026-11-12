const JOIN_PATH = /\/join\/([A-Za-z0-9_]+)\/?$/

export function roomLink(room) {
    const key = room.isPublic ? room.username : room.inviteCode
    return key ? `${window.location.origin}/join/${key}` : ''
}

export function joinKey(text) {
    return text.trim().match(JOIN_PATH)?.[1] ?? null
}

export function roomStatus(room) {
    const members = `${room.memberCount} ${room.memberCount === 1 ? 'member' : 'members'}`
    return room.onlineCount > 0 ? `${members}, ${room.onlineCount} online` : members
}

export function roomLabel(kind) {
    return kind === 'channel' ? 'Channel' : 'Group'
}
