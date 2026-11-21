export const ROLE_COLORS = ['red', 'orange', 'amber', 'yellow', 'lime', 'green', 'teal', 'cyan', 'blue', 'indigo', 'purple', 'pink']

export const PERMISSION_GROUPS = [
    { id: 'general', title: 'General' },
    { id: 'members', title: 'Members' },
    { id: 'messages', title: 'Messages' },
    { id: 'advanced', title: 'Advanced' },
]

export const PERMISSIONS = [
    { key: 'manageGroup', group: 'general', label: 'Manage group', hint: 'Change the group name, photo and invite link, and turn the group-wide switches on or off.' },
    { key: 'manageChannels', group: 'general', label: 'Manage channels', hint: 'Create, rename, reorder and delete channels and categories. Private channels still need their own channel permission.' },
    { key: 'manageRoles', group: 'general', label: 'Manage roles', hint: 'Create, edit and give roles that sit below their own highest role. Also lets them change channel permissions.' },
    { key: 'addMembers', group: 'members', label: 'Add members', hint: 'Invite people to the group, even when regular members are not allowed to.' },
    { key: 'removeMembers', group: 'members', label: 'Remove members', hint: 'Remove and ban members who rank below them. The owner and higher roles are safe.' },
    { key: 'sendAlways', group: 'messages', label: 'Always send messages', hint: 'Keep sending messages even when "Members can send messages" is off for the group.' },
    { key: 'deleteMessages', group: 'messages', label: 'Delete messages', hint: "Delete other members' messages in channels they can view. The owner's messages cannot be deleted." },
    { key: 'administrator', group: 'advanced', label: 'Administrator', hint: 'Every permission above, in every channel, and channel overrides do not apply. Give this to people you fully trust.' },
]

export const ROLE_NAME_LIMIT = 32

export const can = (chat, key) => Boolean(chat?.permissions?.includes(key))

export const isCustomColor = (key) => typeof key === 'string' && key.startsWith('#')

export const roleColor = (key) => (key ? (isCustomColor(key) ? key : `var(--role-${key})`) : undefined)

export function rankOf(roleIds, roles) {
    let best = null
    for (const role of roles) if (roleIds?.includes(role.id) && (best === null || role.position < best)) best = role.position
    return best
}

export function colorOf(roleIds, roles) {
    for (const role of roles) if (role.color && roleIds?.includes(role.id)) return role.color
    return null
}

export function separateRoleOf(roleIds, roles) {
    return roles.find((role) => role.separate && roleIds?.includes(role.id)) ?? null
}

export function outranks(me, other) {
    if (other.owner) return false
    if (me.owner) return true
    if (me.rank === null) return false
    return other.rank === null || me.rank < other.rank
}

export function above(me, position) {
    return me.owner || (me.rank !== null && me.rank < position)
}

export const CHANNEL_PERMISSIONS = [
    { key: 'viewChannel', label: 'View channel', hint: 'See the channel, read its messages and get its notifications. Turn it off to make the channel private.' },
    { key: 'sendMessages', label: 'Send messages', hint: 'Send messages in this channel. Turn it off for a read-only channel.' },
    { key: 'deleteMessages', label: 'Delete messages', hint: "Delete other members' messages in this channel only." },
    { key: 'manageChannel', label: 'Manage channel', hint: 'Rename, move and delete this channel.' },
    { key: 'managePermissions', label: 'Manage permissions', hint: "Change who can do what in this channel, and sync it with its category." },
]

