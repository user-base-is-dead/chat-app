export function dayKey(ms) {
    return new Date(ms).toDateString()
}

export function dayLabel(ms) {
    const day = new Date(ms)
    const today = new Date()
    if (day.toDateString() === today.toDateString()) return 'Today'
    const yesterday = new Date(today)
    yesterday.setDate(today.getDate() - 1)
    if (day.toDateString() === yesterday.toDateString()) return 'Yesterday'
    const sameYear = day.getFullYear() === today.getFullYear()
    return day.toLocaleDateString([], sameYear ? { day: 'numeric', month: 'short' } : { day: 'numeric', month: 'short', year: 'numeric' })
}
