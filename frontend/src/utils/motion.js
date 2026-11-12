import { flushSync } from 'react-dom'

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
const phoneScreen = window.matchMedia('(max-width: 768px)')
let current = 0

export function isPhone() {
    return phoneScreen.matches
}

function canAnimate() {
    return Boolean(document.startViewTransition) && !reducedMotion.matches
}

export function animateChange(update, kind, afterUpdate) {
    if (!canAnimate()) {
        update()
        return null
    }
    const id = ++current
    const root = document.documentElement
    root.dataset.motion = kind
    const transition = document.startViewTransition(() => {
        flushSync(update)
        afterUpdate?.()
    })
    transition.finished.finally(() => {
        if (id === current) delete root.dataset.motion
    })
    return transition
}

function clearMorph() {
    document.querySelectorAll('[data-morph-target]').forEach((el) => {
        el.style.viewTransitionName = ''
    })
}

export function morphChange(source, update, kind) {
    if (!source || !canAnimate()) {
        animateChange(update, kind)
        return
    }
    clearMorph()
    source.style.viewTransitionName = 'peer-avatar'
    const transition = animateChange(update, kind, () => {
        source.style.viewTransitionName = ''
        const target = document.querySelector('[data-morph-target]')
        if (target) target.style.viewTransitionName = 'peer-avatar'
    })
    transition?.finished.finally(() => {
        source.style.viewTransitionName = ''
        clearMorph()
    })
}

export function revealChange(update, x, y) {
    const transition = animateChange(update, 'theme')
    if (!transition) return
    transition.ready.then(() => {
        const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
        document.documentElement.animate(
            { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
            { duration: 700, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', pseudoElement: '::view-transition-new(root)' },
        )
    }).catch(() => {})
}
