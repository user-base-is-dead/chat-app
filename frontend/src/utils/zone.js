let last = null

const mark = (e) => {
    last = e.target
}

document.addEventListener('pointerdown', mark, true)
document.addEventListener('focusin', mark, true)

export function inSidebar() {
    if (!last?.isConnected) return false
    return last.closest('[data-zone]')?.dataset.zone === 'sidebar'
}
