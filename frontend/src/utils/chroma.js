const palette = ['#D02670', '#C2410C', '#6D4AFF', '#0E7490', '#1F6FEB', '#A21CAF', '#2E8540']
const neutral = '#7C7A84'

export const appColors = [
    { id: 'default', label: 'Default', value: '#3A8FE6' },
    { id: 'violet', label: 'Violet', value: '#6D4AFF' },
    { id: 'blue', label: 'Blue', value: '#1F6FEB' },
    { id: 'cyan', label: 'Cyan', value: '#0E7490' },
    { id: 'green', label: 'Green', value: '#2E8540' },
    { id: 'orange', label: 'Orange', value: '#C2410C' },
    { id: 'rose', label: 'Rose', value: '#D02670' },
    { id: 'fuchsia', label: 'Fuchsia', value: '#A21CAF' },
]

export function chromaFor(name = '') {
    let sum = 0
    for (const char of name) sum += char.charCodeAt(0)
    return palette[sum % palette.length]
}

export function chatStyle(chat) {
    return chat.peerDisabled || chat.peerDeleted ? { '--chroma': neutral } : undefined
}
