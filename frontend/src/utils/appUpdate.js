import { useEffect, useState } from 'react'

export const APP_PLATFORM = 'web'
export const APP_VERSION = '1'

const CHECK_EVERY = 10 * 60 * 1000
const SEEN_KEY = 'alertsSeenAt'
const NOTICES_KEY = 'appNotices'
const NOTICE_LIMIT = 30
const listeners = new Set()

function readNumber(key) {
    try {
        return Number(localStorage.getItem(key)) || 0
    } catch {
        return 0
    }
}

let state = { required: false, latest: null, notice: '', flags: {}, alerts: [], seenAt: readNumber(SEEN_KEY) }

function update(changes) {
    state = { ...state, ...changes }
    listeners.forEach((listener) => listener(state))
}

function storedNotices(current) {
    let list = []
    try {
        list = JSON.parse(localStorage.getItem(NOTICES_KEY) || '[]')
    } catch {
        list = []
    }
    if (current && !list.some((item) => item.text === current)) {
        list = [...list, { text: current, at: Date.now() }].slice(-NOTICE_LIMIT)
        try {
            localStorage.setItem(NOTICES_KEY, JSON.stringify(list))
        } catch {
            return list
        }
    }
    return list
}

export function isNewAlert(alert, since) {
    return alert.at > since && (alert.kind === 'notice' || alert.versionCode > Number(APP_VERSION))
}

export function markAlertsSeen() {
    const seenAt = Math.max(Date.now(), ...state.alerts.map((alert) => alert.at))
    try {
        localStorage.setItem(SEEN_KEY, String(seenAt))
    } catch {
        return update({ seenAt })
    }
    update({ seenAt })
}

export function markUpdateRequired() {
    if (!state.required) update({ required: true })
}

export async function checkVersion() {
    try {
        const response = await fetch(`/v1/app/version?platform=${APP_PLATFORM}`)
        if (!response.ok) return
        const info = await response.json()
        const code = Number(APP_VERSION)
        const releases = (info.releases ?? []).map((release) => ({
            id: `release-${release.versionCode}`,
            kind: 'update',
            title: `Miron Chat ${release.versionName}`,
            body: release.notes || 'A new version of Miron Chat is ready.',
            at: release.createdAt,
            versionCode: release.versionCode,
        }))
        const notices = storedNotices(info.flags?.notice ?? '').map((item) => ({
            id: `notice-${item.at}`,
            kind: 'notice',
            title: 'Message from Miron Chat',
            body: item.text,
            at: item.at,
            versionCode: 0,
        }))
        update({
            required: code < (info.minVersion ?? 0) || (info.blocked ?? []).includes(code),
            latest: info.latest,
            flags: info.flags ?? {},
            notice: info.flags?.notice ?? '',
            alerts: [...releases, ...notices].sort((a, b) => b.at - a.at),
        })
    } catch {
        return
    }
}

export function featureEnabled(name) {
    return state.flags[name] !== false
}

export function useAppUpdate() {
    const [current, setCurrent] = useState(state)

    useEffect(() => {
        listeners.add(setCurrent)
        checkVersion()
        const timer = setInterval(checkVersion, CHECK_EVERY)
        const handleFocus = () => checkVersion()
        window.addEventListener('focus', handleFocus)
        return () => {
            listeners.delete(setCurrent)
            clearInterval(timer)
            window.removeEventListener('focus', handleFocus)
        }
    }, [])

    return {
        ...current,
        available: (current.latest?.versionCode ?? 0) > Number(APP_VERSION),
        unread: current.alerts.filter((alert) => isNewAlert(alert, current.seenAt)).length,
    }
}
