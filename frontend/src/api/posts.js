import { authorized } from './client'

export const VIEW_BATCH = 100

export function viewPosts(roomId, ids) {
    return authorized(`/rooms/${roomId}/views`, { method: 'POST', body: { ids } })
}

export function setReaction(roomId, messageId, emoji) {
    return authorized(`/rooms/${roomId}/posts/${messageId}/reaction`, { method: 'PUT', body: { emoji } })
}

export function clearReaction(roomId, messageId) {
    return authorized(`/rooms/${roomId}/posts/${messageId}/reaction`, { method: 'DELETE' })
}

export function listComments(roomId, messageId, beforeId) {
    const before = beforeId ? `?before=${beforeId}` : ''
    return authorized(`/rooms/${roomId}/posts/${messageId}/comments${before}`)
}

export function addComment(roomId, messageId, text) {
    return authorized(`/rooms/${roomId}/posts/${messageId}/comments`, { method: 'POST', body: { text } })
}

export function deleteComment(roomId, messageId, commentId) {
    return authorized(`/rooms/${roomId}/posts/${messageId}/comments/${commentId}`, { method: 'DELETE' })
}

export function setPostSwitches(roomId, messageId, changes) {
    return authorized(`/rooms/${roomId}/posts/${messageId}`, { method: 'PATCH', body: changes })
}
