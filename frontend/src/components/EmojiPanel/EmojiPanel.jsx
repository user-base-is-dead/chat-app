import { useEffect, useRef, useState } from 'react'
import styles from './EmojiPanel.module.css'
import ClockIcon from '../Icons/ClockIcon'
import SearchIcon from '../Icons/SearchIcon'
import BackspaceIcon from '../Icons/BackspaceIcon'

const groupIcons = {
    smileys_emotion: '😀',
    people_body: '👋',
    animals_nature: '🐻',
    food_drink: '🍔',
    travel_places: '✈️',
    activities: '⚽',
    objects: '💡',
    symbols: '❤️',
    flags: '🏳️',
}

const recentKey = 'miron-recent-emoji'
const recentLimit = 24
const defaultRecent = ['😂', '❤️', '👍', '😭', '😊', '🙏', '😍', '🔥', '😁', '🥰', '😅', '👏', '🤔', '😢', '😎', '🎉']

let cachedGroups = null

function loadGroups() {
    if (cachedGroups) return Promise.resolve(cachedGroups)
    return import('unicode-emoji-json/data-by-group.json').then((module) => {
        cachedGroups = module.default.map((group) => ({
            slug: group.slug,
            name: group.name,
            emojis: group.emojis
                .filter((item) => Number(item.emoji_version) <= 14)
                .map((item) => ({ emoji: item.emoji, name: item.name })),
        }))
        return cachedGroups
    })
}

function readRecent() {
    try {
        const saved = JSON.parse(localStorage.getItem(recentKey))
        return Array.isArray(saved) && saved.length ? saved : defaultRecent
    } catch {
        return defaultRecent
    }
}

function saveRecent(list) {
    try {
        localStorage.setItem(recentKey, JSON.stringify(list))
    } catch {
        return
    }
}

function EmojiPanel({ onPick, onBackspace, onSearchFocus, onPointerEnter, onPointerLeave, panelRef }) {
    const [groups, setGroups] = useState(cachedGroups)
    const [recent, setRecent] = useState(readRecent)
    const [query, setQuery] = useState('')
    const [activeGroup, setActiveGroup] = useState('recent')
    const scrollRef = useRef(null)

    useEffect(() => {
        if (!groups) loadGroups().then(setGroups)
    }, [groups])

    const pick = (emoji) => {
        onPick(emoji)
        setRecent((list) => {
            const next = [emoji, ...list.filter((item) => item !== emoji)].slice(0, recentLimit)
            saveRecent(next)
            return next
        })
    }

    const goToGroup = (slug) => {
        setQuery('')
        requestAnimationFrame(() => {
            const section = scrollRef.current.querySelector(`[data-group="${slug}"]`)
            if (section) scrollRef.current.scrollTo({ top: section.offsetTop, behavior: 'smooth' })
        })
        setActiveGroup(slug)
    }

    const handleScroll = () => {
        const sections = scrollRef.current.querySelectorAll('[data-group]')
        const top = scrollRef.current.scrollTop + 8
        let current = activeGroup
        sections.forEach((section) => {
            if (section.offsetTop <= top) current = section.dataset.group
        })
        if (current !== activeGroup) setActiveGroup(current)
    }

    const search = query.trim().toLowerCase()
    const results = search && groups
        ? groups.flatMap((group) => group.emojis).filter((item) => item.name.includes(search))
        : null

    const renderGrid = (emojis) => (
        <div className={styles.grid}>
            {emojis.map((emoji) => (
                <button key={emoji} type="button" className={styles.emoji} onClick={() => pick(emoji)}>
                    {emoji}
                </button>
            ))}
        </div>
    )

    return (
        <div className={styles.panel} ref={panelRef} onPointerEnter={onPointerEnter} onPointerLeave={onPointerLeave}>
            <div className={styles.tabs}>
                <button
                    type="button"
                    className={activeGroup === 'recent' ? `${styles.tab} ${styles.tabActive}` : styles.tab}
                    onClick={() => goToGroup('recent')}
                    aria-label="Frequently used"
                >
                    <ClockIcon />
                </button>
                {Object.entries(groupIcons).map(([slug, icon]) => (
                    <button
                        key={slug}
                        type="button"
                        className={slug === activeGroup ? `${styles.tab} ${styles.tabEmoji} ${styles.tabActive}` : `${styles.tab} ${styles.tabEmoji}`}
                        onClick={() => goToGroup(slug)}
                        disabled={!groups}
                    >
                        {icon}
                    </button>
                ))}
            </div>
            <label className={styles.search}>
                <SearchIcon />
                <input
                    className={styles.searchInput}
                    type="text"
                    placeholder="Search Emoji"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={onSearchFocus}
                />
            </label>
            <div className={styles.scroll} ref={scrollRef} onScroll={handleScroll}>
                {results && (
                    results.length
                        ? renderGrid(results.map((item) => item.emoji))
                        : <div className={styles.empty}>No emoji found</div>
                )}
                {!results && (
                    <>
                        <section data-group="recent">
                            <div className={styles.groupName}>Frequently Used</div>
                            {renderGrid(recent)}
                        </section>
                        {!groups && <div className={styles.empty}>Loading…</div>}
                        {groups && groups.map((group) => (
                            <section key={group.slug} data-group={group.slug} className={styles.section}>
                                <div className={styles.groupName}>{group.name}</div>
                                {renderGrid(group.emojis.map((item) => item.emoji))}
                            </section>
                        ))}
                    </>
                )}
            </div>
            <div className={styles.footer}>
                <button type="button" className={styles.backspace} onClick={onBackspace} aria-label="Delete">
                    <BackspaceIcon />
                </button>
            </div>
        </div>
    )
}

export default EmojiPanel
