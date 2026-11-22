import { authorized } from './client'

function encodeContent(text) {
    const bytes = new TextEncoder().encode(text)
    let binary = ''
    for (const byte of bytes) binary += String.fromCharCode(byte)
    return btoa(binary)
}

function decodeContent(ciphertext) {
    const binary = atob(ciphertext)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
    return new TextDecoder().decode(bytes)
}

export function messageText(message) {
    return message.ciphertext ? decodeContent(message.ciphertext) : ''
}

export function searchUsers(query) {
    return authorized(`/users/search?q=${encodeURIComponent(query)}`)
}

export function listChats(onResponse) {
    return authorized('/chats', { onResponse })
}

export function openDm(userId) {
    return authorized('/chats/dm', { method: 'POST', body: { userId } })
}

export function acceptChat(chatId) {
    return authorized(`/chats/${chatId}/accept`, { method: 'POST' })
}

export function restrictChat(chatId) {
    return authorized(`/chats/${chatId}/restrict`, { method: 'POST' })
}

export function unrestrictChat(chatId) {
    return authorized(`/chats/${chatId}/unrestrict`, { method: 'POST' })
}

export function pinChat(chatId) {
    return authorized(`/chats/${chatId}/pin`, { method: 'POST' })
}

export function unpinChat(chatId) {
    return authorized(`/chats/${chatId}/unpin`, { method: 'POST' })
}

export function muteChat(chatId) {
    return authorized(`/chats/${chatId}/mute`, { method: 'POST' })
}

export function unmuteChat(chatId) {
    return authorized(`/chats/${chatId}/unmute`, { method: 'POST' })
}

export function deleteChat(chatId) {
    return authorized(`/chats/${chatId}`, { method: 'DELETE' })
}

export const MESSAGE_PAGE_SIZE = 50

export function loadMessages(chatId, beforeId) {
    const before = beforeId ? `&before=${beforeId}` : ''
    return authorized(`/chats/${chatId}/messages?limit=${MESSAGE_PAGE_SIZE}${before}`)
}

export function loadNewerMessages(chatId, afterId) {
    return authorized(`/chats/${chatId}/messages?limit=${MESSAGE_PAGE_SIZE}&after=${afterId}`)
}

export function editMessage(chatId, messageId, text) {
    return authorized(`/chats/${chatId}/messages/${messageId}`, { method: 'PATCH', body: { ciphertext: encodeContent(text) } })
}

export function sendMessage(chatId, text, switches, replyToId) {
    const body = { ciphertext: encodeContent(text), ...switches }
    if (replyToId) body.replyToId = replyToId
    return authorized(`/chats/${chatId}/messages`, { method: 'POST', body })
}

export function forwardMessage(chatId, fromChatId, messageId) {
    return authorized(`/chats/${chatId}/messages`, { method: 'POST', body: { forwardFrom: { chatId: fromChatId, messageId } } })
}

export function loadMessagesAround(chatId, messageId) {
    const after = loadNewerMessages(chatId, messageId)
    const before = loadMessages(chatId, (BigInt(messageId) + 1n).toString())
    return Promise.all([before, after])
}

export function loadPinned(chatId) {
    return authorized(`/chats/${chatId}/pinned`)
}

export function pinMessage(chatId, messageId) {
    return authorized(`/chats/${chatId}/pinned`, { method: 'PUT', body: { messageId } })
}

export function unpinMessage(chatId) {
    return authorized(`/chats/${chatId}/pinned`, { method: 'DELETE' })
}

export function loadPlace(chatId) {
    return authorized(`/chats/${chatId}/place`)
}

export function deleteMessage(chatId, messageId) {
    return authorized(`/chats/${chatId}/messages/${messageId}`, { method: 'DELETE' })
}

export function markRead(chatId, messageId) {
    return authorized(`/chats/${chatId}/read`, { method: 'POST', body: { messageId } })
}
