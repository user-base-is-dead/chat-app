import { APP_PLATFORM, APP_VERSION, markUpdateRequired } from '../utils/appUpdate'

const ACCESS_KEY = 'civenth.accessToken'
const REFRESH_KEY = 'civenth.refreshToken'

export class ApiError extends Error {
    constructor(status, code, message) {
        super(message)
        this.status = status
        this.code = code
    }
}

function readItem(key) {
    try {
        return localStorage.getItem(key)
    } catch {
        return null
    }
}

function writeItem(key, value) {
    try {
        if (value === null) localStorage.removeItem(key)
        else localStorage.setItem(key, value)
    } catch {
        return
    }
}

export function saveSession(session) {
    writeItem(ACCESS_KEY, session.accessToken)
    writeItem(REFRESH_KEY, session.refreshToken)
}

export function clearSession() {
    writeItem(ACCESS_KEY, null)
    writeItem(REFRESH_KEY, null)
}

export function hasSession() {
    return Boolean(readItem(REFRESH_KEY))
}

export function watchSession(onChange) {
    const handleStorage = (e) => {
        if (e.key === REFRESH_KEY || e.key === null) onChange()
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
}

export function accessToken() {
    return readItem(ACCESS_KEY)
}

export async function send(path, { method = 'GET', body, raw, token } = {}) {
    const headers = { 'X-App-Platform': APP_PLATFORM, 'X-App-Version': APP_VERSION }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    if (raw) headers['Content-Type'] = raw.type || 'application/octet-stream'
    if (token) headers.Authorization = `Bearer ${token}`

    let response
    try {
        response = await fetch(`/v1${path}`, {
            method,
            headers,
            body: raw ?? (body === undefined ? undefined : JSON.stringify(body)),
        })
    } catch {
        throw new ApiError(0, 'network', 'Cannot reach the server. Check your connection.')
    }

    if (response.status === 426) markUpdateRequired()
    if (response.status === 204) return null
    const data = await response.json().catch(() => null)
    if (!response.ok) {
        throw new ApiError(response.status, data?.error?.code ?? 'unknown', data?.error?.message ?? 'Something went wrong')
    }
    return data
}

let refreshing = null

export function refreshSession() {
    const refreshToken = readItem(REFRESH_KEY)
    if (!refreshToken) return Promise.reject(new ApiError(401, 'unauthorized', 'Please log in again'))

    refreshing ??= send('/auth/refresh', { method: 'POST', body: { refreshToken } })
        .then((session) => saveSession(session))
        .catch((error) => {
            if (error.status === 401) clearSession()
            throw error
        })
        .finally(() => {
            refreshing = null
        })
    return refreshing
}

export async function authorized(path, options = {}) {
    try {
        return await send(path, { ...options, token: readItem(ACCESS_KEY) })
    } catch (error) {
        if (error.status !== 401) throw error
        await refreshSession()
        return send(path, { ...options, token: readItem(ACCESS_KEY) })
    }
}
