const palette = ['#D02670', '#C2410C', '#6D4AFF', '#0E7490', '#1F6FEB', '#A21CAF', '#2E8540']
const neutral = '#7C7A84'

export const appColors = [
    { id: 'default', label: 'Default', value: '#3A8FE6' },
]

export function chromaFor(name = '') {
    let sum = 0
    for (const char of name) sum += char.charCodeAt(0)
    return palette[sum % palette.length]
}

export function chatStyle(chat) {
    return chat.peerDisabled || chat.peerDeleted ? { '--chroma': neutral } : undefined
}
