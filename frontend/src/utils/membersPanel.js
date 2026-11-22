const KEY = 'civenth.membersPanel'

export function readMembersPanel() {
    try {
        return localStorage.getItem(KEY) !== 'closed'
    } catch {
        return true
    }
}

export function saveMembersPanel(open) {
    try {
        localStorage.setItem(KEY, open ? 'open' : 'closed')
    } catch {
        return
    }
}
