import { useEffect, useState } from 'react'
import { loadMessages, messageText, MESSAGE_PAGE_SIZE } from '../../api/chats'
import styles from './ChatPage.module.css'

export default function ConversationSearch({ chatId, onClose, onPick }) {
    const [query, setQuery] = useState('')
    const [hits, setHits] = useState([])
    const [status, setStatus] = useState('')
    useEffect(() => {
        let active = true
        const needle = query.trim().toLowerCase()
        setHits([])
        setStatus(needle ? 'Searching…' : '')
        const timer = setTimeout(async () => {
            if (!needle) return
            try {
                let before
                let found = false
                while (active) {
                    const page = await loadMessages(chatId, before)
                    if (!active) return
                    const matches = page.map(m => ({ id: m.id, text: messageText(m) })).filter(m => m.text.toLowerCase().includes(needle))
                    if (matches.length) { found = true; setHits(prev => [...prev, ...matches]) }
                    if (page.length < MESSAGE_PAGE_SIZE) break
                    const oldest = page.reduce((a, b) => BigInt(a.id) < BigInt(b.id) ? a : b).id
                    if (oldest === before) break
                    before = oldest
                }
                if (active) setStatus(found ? '' : 'No matching messages')
            } catch { if (active) setStatus('Could not search messages') }
        }, 250)
        return () => { active = false; clearTimeout(timer) }
    }, [chatId, query])
    return <section className={styles.conversationSearch}>
        <div><input autoFocus type="search" aria-label="Search this conversation" placeholder="Search this conversation" value={query} onChange={e => setQuery(e.target.value)} />
        <button type="button" onClick={onClose}>Close</button></div>
        {hits.map(m => <button type="button" key={m.id} onClick={() => onPick(m.id)}>{m.text}</button>)}
        {status && <small role="status">{status}</small>}
    </section>
}
