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

export function listChats() {
    return authorized('/chats')
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

export function sendMessage(chatId, text) {
    return authorized(`/chats/${chatId}/messages`, { method: 'POST', body: { ciphertext: encodeContent(text) } })
}

export function deleteMessage(chatId, messageId) {
    return authorized(`/chats/${chatId}/messages/${messageId}`, { method: 'DELETE' })
}

export function markRead(chatId, messageId) {
    return authorized(`/chats/${chatId}/read`, { method: 'POST', body: { messageId } })
}
