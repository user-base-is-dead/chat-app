function shorten(value, unit) {
    const rounded = Math.floor(value * 10) / 10
    return `${value < 10 ? rounded : Math.floor(value)}${unit}`
}

export function formatCount(count) {
    if (count < 1000) return String(count)
    if (count < 1000000) return shorten(count / 1000, 'K')
    return shorten(count / 1000000, 'M')
}
