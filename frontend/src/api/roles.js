import { authorized } from './client'

export function listRoles(roomId) {
    return authorized(`/rooms/${roomId}/roles`)
}

export function createRole(roomId, role) {
    return authorized(`/rooms/${roomId}/roles`, { method: 'POST', body: role })
}

export function updateRole(roomId, roleId, changes) {
    return authorized(`/rooms/${roomId}/roles/${roleId}`, { method: 'PATCH', body: changes })
}

export function deleteRole(roomId, roleId) {
    return authorized(`/rooms/${roomId}/roles/${roleId}`, { method: 'DELETE' })
}

export function setMemberRoles(roomId, userId, roleIds) {
    return authorized(`/rooms/${roomId}/members/${userId}/roles`, { method: 'PUT', body: { roleIds } })
}
