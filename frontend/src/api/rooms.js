import { authorized } from './client'

export function listRooms(onResponse) {
    return authorized('/rooms', { onResponse })
}

export function createRoom(room) {
    return authorized('/rooms', { method: 'POST', body: room })
}

export function getRoom(roomId) {
    return authorized(`/rooms/${roomId}`)
}

export function updateRoom(roomId, changes) {
    return authorized(`/rooms/${roomId}`, { method: 'PATCH', body: changes })
}

export function deleteRoom(roomId) {
    return authorized(`/rooms/${roomId}`, { method: 'DELETE' })
}

export function acceptInvite(roomId) {
    return authorized(`/rooms/${roomId}/accept`, { method: 'POST' })
}

export function declineInvite(roomId) {
    return authorized(`/rooms/${roomId}/decline`, { method: 'POST' })
}

export function leaveRoom(roomId) {
    return authorized(`/rooms/${roomId}/leave`, { method: 'POST' })
}

export function muteRoom(roomId) {
    return authorized(`/rooms/${roomId}/mute`, { method: 'POST' })
}

export function unmuteRoom(roomId) {
    return authorized(`/rooms/${roomId}/unmute`, { method: 'POST' })
}

export function pinRoom(roomId) {
    return authorized(`/rooms/${roomId}/pin`, { method: 'POST' })
}

export function unpinRoom(roomId) {
    return authorized(`/rooms/${roomId}/unpin`, { method: 'POST' })
}

export function resetInvite(roomId) {
    return authorized(`/rooms/${roomId}/invite`, { method: 'POST' })
}

export function transferRoom(roomId, userId) {
    return authorized(`/rooms/${roomId}/transfer`, { method: 'POST', body: { userId } })
}

export const MEMBER_PAGE_SIZE = 100

export function listMembers(roomId, offset = 0, channelId = null) {
    return authorized(`/rooms/${roomId}/members?offset=${offset}${channelId ? `&channel=${channelId}` : ''}`)
}

export function addMembers(roomId, userIds) {
    return authorized(`/rooms/${roomId}/members`, { method: 'POST', body: { userIds } })
}

export function setMemberRole(roomId, userId, role) {
    return authorized(`/rooms/${roomId}/members/${userId}`, { method: 'PATCH', body: { role } })
}

export function removeMember(roomId, userId) {
    return authorized(`/rooms/${roomId}/members/${userId}`, { method: 'DELETE' })
}

export function findRooms(query, kind) {
    return authorized(`/find/rooms?q=${encodeURIComponent(query)}&kind=${kind}`)
}

export function previewJoin(key) {
    return authorized(`/join/${encodeURIComponent(key)}`)
}

export function joinRoom(key) {
    return authorized(`/join/${encodeURIComponent(key)}`, { method: 'POST' })
}

export function uploadPhoto(file) {
    return authorized('/media/photo', { method: 'POST', raw: file })
}
