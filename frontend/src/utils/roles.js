export const ROLE_COLORS = ['red', 'orange', 'amber', 'yellow', 'lime', 'green', 'teal', 'cyan', 'blue', 'indigo', 'purple', 'pink']

export const PERMISSIONS = [
    { key: 'manageGroup', label: 'Manage group', hint: 'Name, photo, link and group switches' },
    { key: 'manageChannels', label: 'Manage channels', hint: 'Create, rename, move and delete channels' },
    { key: 'manageRoles', label: 'Manage roles', hint: 'Edit and give roles below their own' },
    { key: 'addMembers', label: 'Add members', hint: 'Even when members cannot add' },
    { key: 'removeMembers', label: 'Remove members', hint: 'Remove and ban members below them' },
    { key: 'deleteMessages', label: 'Delete messages', hint: "Delete other members' messages" },
    { key: 'sendAlways', label: 'Always send', hint: 'Even when members cannot send' },
    { key: 'administrator', label: 'Administrator', hint: 'Every permission above' },
]

export const ROLE_NAME_LIMIT = 32

export const can = (chat, key) => Boolean(chat?.permissions?.includes(key))

export const roleColor = (key) => (key ? `var(--role-${key})` : undefined)

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
    { key: 'viewChannel', label: 'View channel', hint: 'See the channel and its messages' },
    { key: 'sendMessages', label: 'Send messages', hint: 'Write in this channel' },
    { key: 'deleteMessages', label: 'Delete messages', hint: "Delete other members' messages here" },
    { key: 'manageChannel', label: 'Manage channel', hint: 'Rename, move and delete this channel' },
    { key: 'managePermissions', label: 'Manage permissions', hint: "Change this channel's permissions" },
]

