export function lastSeenText(ms) {
    if (!ms) return 'last seen recently'
    const seen = new Date(ms)
    const time = seen.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
    if (seen.toDateString() === new Date().toDateString()) return `last seen at ${time}`
    return `last seen ${seen.toLocaleDateString([], { day: 'numeric', month: 'short' })} at ${time}`
}

export function peerStatus(chat, typing) {
    if (chat.peerDeleted) return ''
    if (chat.peerDisabled) return 'account disabled'
    if (typing) return 'typing…'
    return chat.peerOnline ? 'online' : lastSeenText(chat.peerLastSeenAt)
}
