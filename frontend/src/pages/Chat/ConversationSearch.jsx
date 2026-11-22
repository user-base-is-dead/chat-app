import { useEffect, useState } from 'react'
import { loadMessages, messageText, MESSAGE_PAGE_SIZE } from '../../api/chats'
import { dateTime } from '../../utils/dayLabel'
import BackIcon from '../../components/Icons/BackIcon'
import SearchIcon from '../../components/Icons/SearchIcon'
import styles from './ConversationSearch.module.css'

function Highlight({ text, needle }) {
    if (!needle) return text
    const parts = []
    const lower = text.toLowerCase()
    let from = 0
    let at = lower.indexOf(needle)
    while (at !== -1) {
        parts.push(text.slice(from, at), <mark key={at}>{text.slice(at, at + needle.length)}</mark>)
        from = at + needle.length
        at = lower.indexOf(needle, from)
    }
    parts.push(text.slice(from))
    return parts
}

export default function ConversationSearch({ chatId, title, onClose, onPick }) {
    const [query, setQuery] = useState('')
    const [hits, setHits] = useState([])
    const [status, setStatus] = useState('')
    const needle = query.trim().toLowerCase()

    useEffect(() => {
        let active = true
        const timer = setTimeout(async () => {
            if (!needle) return
            try {
                let before
                let found = false
                while (active) {
                    const page = await loadMessages(chatId, before)
                    if (!active) return
                    const matches = page
                        .map((m) => ({ id: m.id, text: messageText(m), name: m.senderName, createdAt: m.createdAt }))
                        .filter((m) => m.text.toLowerCase().includes(needle))
                        .reverse()
                    if (matches.length) {
                        found = true
                        setHits((prev) => [...prev, ...matches])
                    }
                    if (page.length < MESSAGE_PAGE_SIZE) break
                    const oldest = page.reduce((a, b) => (BigInt(a.id) < BigInt(b.id) ? a : b)).id
                    if (oldest === before) break
                    before = oldest
                }
                if (active) setStatus(found ? '' : 'No matching messages')
            } catch {
                if (active) setStatus('Could not search messages')
            }
        }, 300)
        return () => {
            active = false
            clearTimeout(timer)
        }
    }, [chatId, needle])

    const change = (value) => {
        setQuery(value)
        setHits([])
        setStatus(value.trim() ? 'Searching…' : '')
    }

    return (
        <section className={styles.panel}>
            <div className={styles.bar}>
                <button className={styles.back} type="button" aria-label="Close search" onClick={onClose}>
                    <BackIcon />
                </button>
                <label className={styles.field}>
                    <SearchIcon />
                    <input
                        autoFocus
                        type="text"
                        aria-label="Search this conversation"
                        placeholder={title ? `Search in ${title}` : 'Search this conversation'}
                        value={query}
                        onChange={(e) => change(e.target.value)}
                    />
                </label>
            </div>
            {(hits.length > 0 || status) && (
                <div className={styles.results}>
                    {hits.map((m) => (
                        <button className={styles.hit} type="button" key={m.id} onClick={() => onPick(m.id)}>
                            <span className={styles.hitHead}>
                                <span className={styles.hitName}>{m.name || title}</span>
                                <span className={styles.hitTime}>{dateTime(m.createdAt)}</span>
                            </span>
                            <span className={styles.hitText}><Highlight text={m.text} needle={needle} /></span>
                        </button>
                    ))}
                    {status && <p className={styles.status} role="status">{status}</p>}
                </div>
            )}
        </section>
    )
}
