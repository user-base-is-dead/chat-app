import { authorized, clearSession, saveSession, send } from './client'

function deviceName() {
    const agent = navigator.userAgent
    const browser = /Edg\//.test(agent) ? 'Edge'
        : /Chrome\//.test(agent) ? 'Chrome'
        : /Firefox\//.test(agent) ? 'Firefox'
        : /Safari\//.test(agent) ? 'Safari'
        : 'Browser'
    const system = /Android/.test(agent) ? 'Android'
        : /iPhone|iPad/.test(agent) ? 'iOS'
        : /Windows/.test(agent) ? 'Windows'
        : /Mac OS/.test(agent) ? 'macOS'
        : /Linux/.test(agent) ? 'Linux'
        : 'Unknown'
    return `${browser} on ${system}`
}

async function openSession(path, username, password) {
    const session = await send(path, {
        method: 'POST',
        body: { username, password, platform: 'web', deviceName: deviceName() },
    })
    saveSession(session)
    return session.user
}

export function login(username, password) {
    return openSession('/auth/login', username, password)
}

export async function register(displayName, username, password) {
    const { user } = await send('/auth/register', { method: 'POST', body: { displayName, username, password } })
    return user
}

export function currentUser() {
    return authorized('/auth/me')
}

export async function logout() {
    try {
        await authorized('/auth/logout', { method: 'POST' })
    } catch {
        return
    } finally {
        clearSession()
    }
}
