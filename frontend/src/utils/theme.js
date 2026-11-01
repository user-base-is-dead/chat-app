const KEY = 'theme'
const DEFAULT_CHOICE = 'light'
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
    document.documentElement.dataset.theme = dark ? 'dark' : 'light'
}

export function setThemeChoice(choice) {
    applyTheme(choice)
    try {
        localStorage.setItem(KEY, choice)
    } catch {
        return
    }
}

export function initTheme() {
    applyTheme(getThemeChoice())
    darkQuery.addEventListener('change', () => {
        if (getThemeChoice() === 'system') applyTheme('system')
    })
}
