import { authorized } from './client'

export function listChannels(roomId) {
    return authorized(`/rooms/${roomId}/channels`)
}

export function createChannel(roomId, name, categoryId) {
    return authorized(`/rooms/${roomId}/channels`, { method: 'POST', body: { name, categoryId: categoryId ?? '' } })
}

export function updateChannel(roomId, channelId, changes) {
    return authorized(`/rooms/${roomId}/channels/${channelId}`, { method: 'PATCH', body: changes })
}

export function deleteChannel(roomId, channelId) {
    return authorized(`/rooms/${roomId}/channels/${channelId}`, { method: 'DELETE' })
}

export function createCategory(roomId, name) {
    return authorized(`/rooms/${roomId}/categories`, { method: 'POST', body: { name } })
}

export function updateCategory(roomId, categoryId, changes) {
    return authorized(`/rooms/${roomId}/categories/${categoryId}`, { method: 'PATCH', body: changes })
}

export function deleteCategory(roomId, categoryId) {
    return authorized(`/rooms/${roomId}/categories/${categoryId}`, { method: 'DELETE' })
}

export function getChannelPermissions(roomId, channelId) {
    return authorized(`/rooms/${roomId}/channels/${channelId}/permissions`)
}

export function setChannelOverride(roomId, channelId, targetType, targetId, allow, deny) {
    return authorized(`/rooms/${roomId}/channels/${channelId}/permissions/${targetType}/${targetId}`, { method: 'PUT', body: { allow, deny } })
}

export function syncChannel(roomId, channelId) {
    return authorized(`/rooms/${roomId}/channels/${channelId}/sync`, { method: 'POST' })
}

export function getCategoryPermissions(roomId, categoryId) {
    return authorized(`/rooms/${roomId}/categories/${categoryId}/permissions`)
}

export function setCategoryOverride(roomId, categoryId, targetType, targetId, allow, deny) {
    return authorized(`/rooms/${roomId}/categories/${categoryId}/permissions/${targetType}/${targetId}`, { method: 'PUT', body: { allow, deny } })
}
