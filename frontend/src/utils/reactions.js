export const REACTIONS = ['👍', '❤️', '🔥', '😂', '😮', '😢']

const DEFAULT_SWITCHES = { allowReactions: true, allowComments: true }

export function readSwitches(roomId) {
    try {
        const saved = JSON.parse(localStorage.getItem(`postSwitches:${roomId}`))
        return { ...DEFAULT_SWITCHES, ...saved }
    } catch {
        return DEFAULT_SWITCHES
    }
}

export function saveSwitches(roomId, switches) {
    try {
        localStorage.setItem(`postSwitches:${roomId}`, JSON.stringify(switches))
    } catch {
        return
    }
}
