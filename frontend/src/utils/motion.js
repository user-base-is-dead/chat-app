const phoneScreen = window.matchMedia('(max-width: 768px)')

export function isPhone() {
    return phoneScreen.matches
}

export function animateChange(update, kind, afterUpdate) {
    update()
    afterUpdate?.()
    return null
}

export function morphChange(source, update) {
    update()
}

export function revealChange(update) {
    update()
}
