import { useEffect, useState } from 'react'
import styles from './MemberPicker.module.css'
import Avatar from '../Avatar/Avatar'
import CloseIcon from '../Icons/CloseIcon'
import TickIcon from '../Icons/TickIcon'
import Spinner from '../Spinner/Spinner'
import { searchUsers } from '../../api/chats'

const MIN_QUERY = 2

function MemberPicker({ known, selected, excludeIds = [], onChange }) {
    const [query, setQuery] = useState('')
    const [results, setResults] = useState({ query: '', list: [] })
    const needle = query.trim().replace(/^@/, '')
    const searching = needle.length >= MIN_QUERY

    useEffect(() => {
        if (!searching) return
        const timer = setTimeout(() => {
            searchUsers(needle)
                .then((list) => setResults({ query: needle, list }))
                .catch(() => setResults({ query: needle, list: [] }))
        }, 250)
        return () => clearTimeout(timer)
    }, [needle, searching])

    const loading = searching && results.query !== needle
    const people = (searching ? (loading ? [] : results.list) : known).filter((person) => !excludeIds.includes(person.id))
    const isPicked = (person) => selected.some((item) => item.id === person.id)

    const toggle = (person) => {
        onChange(isPicked(person) ? selected.filter((item) => item.id !== person.id) : [...selected, person])
    }

    return (
        <div className={styles.picker}>
            {selected.length > 0 && (
                <div className={styles.chips}>
                    {selected.map((person) => (
                        <button key={person.id} className={styles.chip} type="button" aria-label={`Remove ${person.displayName}`} onClick={() => toggle(person)}>
                            {person.displayName}
                            <CloseIcon />
                        </button>
                    ))}
                </div>
            )}
            <div className={styles.searchWrap}>
                <input className={styles.search} type="text" placeholder="Search by username" value={query} onChange={(e) => setQuery(e.target.value)} />
            </div>
            <div className={styles.list}>
                {loading && <div className={styles.loading}><Spinner size={22} /></div>}
                {!loading && people.length === 0 && <div className={styles.note}>{searching ? 'No users found' : 'Search a username to add people'}</div>}
                {people.map((person) => (
                    <button key={person.id} className={styles.person} type="button" onClick={() => toggle(person)}>
                        <Avatar name={person.displayName} size={44} />
                        <span className={styles.text}>
                            <span className={styles.name}>{person.displayName}</span>
                            <span className={styles.username}>@{person.username}</span>
                        </span>
                        <span className={isPicked(person) ? `${styles.check} ${styles.checked}` : styles.check}>{isPicked(person) && <TickIcon />}</span>
                    </button>
                ))}
            </div>
        </div>
    )
}

export default MemberPicker
