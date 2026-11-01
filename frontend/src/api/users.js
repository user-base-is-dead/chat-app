import { authorized } from './client'

export function updateProfile(displayName, username, bio) {
    return authorized('/users/me', { method: 'PATCH', body: { displayName, username, bio } })
}

export function changePassword(currentPassword, newPassword) {
    return authorized('/users/me/password', { method: 'POST', body: { currentPassword, newPassword } })
}

export function disableAccount(password) {
    return authorized('/users/me/disable', { method: 'POST', body: { password } })
}

export function deleteAccount(password) {
    return authorized('/users/me/delete', { method: 'POST', body: { password } })
}
