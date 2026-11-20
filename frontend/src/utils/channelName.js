export const NAME_LIMIT = 32

export function cleanChannelName(text) {
    return text
        .trim()
        .toLowerCase()
        .replace(/ /g, '-')
        .replace(/[^\p{L}\p{M}\p{N}_-]/gu, '')
}

function read(key, fallback) {
    try {
        const saved = JSON.parse(localStorage.getItem(key))
        return saved ?? fallback
    } catch {
        return fallback
    }
}

function write(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value))
    } catch {
        return
    }
}

export function readOpenChannel(roomId) {
    return read(`openChannel:${roomId}`, null)
}

export function saveOpenChannel(roomId, channelId) {
    write(`openChannel:${roomId}`, channelId)
}

export function readCollapsed(roomId) {
    return read(`collapsed:${roomId}`, [])
}

export function saveCollapsed(roomId, ids) {
    write(`collapsed:${roomId}`, ids)
}
