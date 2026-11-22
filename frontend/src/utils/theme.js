import { appColors } from './chroma'

const KEY = 'theme'
const COLOR_KEY = 'color'
const DEFAULT_CHOICE = 'light'
const DEFAULT_COLOR = 'default'
const darkQuery = window.matchMedia('(prefers-color-scheme: dark)')


export function getThemeChoice() {
    try {
        return localStorage.getItem(KEY) || DEFAULT_CHOICE
    } catch {
        return DEFAULT_CHOICE
    }
}

function applyTheme(choice) {
    const dark = choice === 'dark' || (choice === 'system' && darkQuery.matches)
    const root = document.documentElement
    const theme = dark ? 'dark' : 'light'
    if (root.dataset.theme === theme) return
    root.dataset.switchingTheme = ''
    root.dataset.theme = theme
    getComputedStyle(document.body).backgroundColor
    delete root.dataset.switchingTheme
}

export function setThemeChoice(choice) {
    applyTheme(choice)
    try {
        localStorage.setItem(KEY, choice)
    } catch {
        return
    }
}

export function getColorChoice() {
    try {
        const saved = localStorage.getItem(COLOR_KEY)
        return appColors.some((item) => item.id === saved) ? saved : DEFAULT_COLOR
    } catch {
        return DEFAULT_COLOR
    }
}

function applyColor(choice) {
    const { value } = appColors.find((item) => item.id === choice)
    document.documentElement.style.setProperty('--chroma', value)
    document.documentElement.dataset.color = choice
}

export function setColorChoice(choice) {
    applyColor(choice)
    try {
        localStorage.setItem(COLOR_KEY, choice)
    } catch {
        return
    }
}

export function initTheme() {
    applyTheme(getThemeChoice())
    applyColor(getColorChoice())
    darkQuery.addEventListener('change', () => {
        if (getThemeChoice() === 'system') applyTheme('system')
    })
}
