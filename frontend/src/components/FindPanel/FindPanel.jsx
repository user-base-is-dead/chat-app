import { useEffect, useRef, useState } from 'react'
import styles from './FindPanel.module.css'
import BackIcon from '../Icons/BackIcon'
import Avatar from '../Avatar/Avatar'
import Spinner from '../Spinner/Spinner'
import HighlightText from '../HighlightText/HighlightText'
import { searchUsers } from '../../api/chats'

const kinds = {
    contacts: { title: 'Add Contacts', placeholder: 'Search by username', hint: 'Type a username to find people', empty: 'No users found' },
    groups: { title: 'Search Group', placeholder: 'Search groups', hint: 'Type a group name or @username', empty: 'No groups found' },
    channels: { title: 'Search Channel', placeholder: 'Search channels', hint: 'Type a channel name or @username', empty: 'No channels found' },
}

const MIN_QUERY = 2

function FindPanel({ kind, onStartChat }) {
    const [query, setQuery] = useState('')
    const [results, setResults] = useState({ query: '', list: [] })
    const inputRef = useRef(null)
    const text = kinds[kind]
    const needle = query.trim().replace(/^@/, '')

    useEffect(() => {
        inputRef.current?.focus()
    }, [kind])

    useEffect(() => {
        if (kind !== 'contacts' || needle.length < MIN_QUERY) return
        const timer = setTimeout(() => {
            searchUsers(needle)
                .then((list) => setResults({ query: needle, list }))
                .catch(() => setResults({ query: needle, list: [] }))
        }, 250)
        return () => clearTimeout(timer)
    }, [kind, needle])

    const searching = needle.length >= MIN_QUERY
    const loading = kind === 'contacts' && searching && results.query !== needle
    const list = kind === 'contacts' && searching && !loading ? results.list : []
    const exact = list.filter((person) => person.username.toLowerCase() === needle.toLowerCase())
    const similar = list.filter((person) => !exact.includes(person))

    const renderPerson = (person, isExact) => (
        <button
            key={person.id}
            className={isExact ? `${styles.person} ${styles.personExact}` : styles.person}
            type="button"
            onClick={() => onStartChat(person)}
        >
            <Avatar name={person.displayName} />
            <span className={styles.personText}>
                <span className={styles.personName}>{person.displayName}</span>
                <span className={styles.personUsername}>
                    @<HighlightText text={person.username} query={needle} />
                </span>
            </span>
        </button>
    )

    return (
        <>
            <div className={styles.header}>
                <button className={styles.back} type="button" aria-label="Back" onClick={() => window.history.back()}>
                    <BackIcon />
                </button>
                <span className={styles.title}>{text.title}</span>
            </div>
            <div className={styles.searchWrap}>
                <input
                    ref={inputRef}
                    className={styles.search}
                    type="text"
                    placeholder={text.placeholder}
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                />
            </div>
            <div className={styles.list}>
                {!searching && <div className={styles.note}>{text.hint}</div>}
                {loading && (
                    <div className={styles.loading}>
                        <Spinner size={22} />
                    </div>
                )}
                {searching && !loading && list.length === 0 && <div className={styles.note}>{text.empty}</div>}
                {exact.length > 0 && <div className={styles.sectionTitle}>Exact match</div>}
                {exact.map((person) => renderPerson(person, true))}
                {similar.length > 0 && <div className={styles.sectionTitle}>{exact.length > 0 ? 'Similar usernames' : 'Users'}</div>}
                {similar.map((person) => renderPerson(person, false))}
            </div>
        </>
    )
}

export default FindPanel
