import { useEffect, useRef, useState } from 'react'
import styles from './FindPanel.module.css'
import BackIcon from '../Icons/BackIcon'
import Avatar from '../Avatar/Avatar'
import Spinner from '../Spinner/Spinner'
import HighlightText from '../HighlightText/HighlightText'
import { searchUsers } from '../../api/chats'
import { findRooms } from '../../api/rooms'
import { joinKey } from '../../utils/roomLink'

const kinds = {
    contacts: { title: 'Search User', placeholder: 'Search by username', hint: 'Type a username to find people', empty: 'No users found' },
    groups: { title: 'Search Group', placeholder: 'Search groups or paste a link', hint: 'Type a group name or @username, or paste an invite link', empty: 'No groups found', roomKind: 'group' },
    channels: { title: 'Search Channel', placeholder: 'Search channels or paste a link', hint: 'Type a channel name or @username, or paste an invite link', empty: 'No channels found', roomKind: 'channel' },
}

const MIN_QUERY = 2

function FindPanel({ kind, onStartChat, onOpenRoom, onOpenKey }) {
    const [query, setQuery] = useState('')
    const [results, setResults] = useState({ query: '', list: [] })
    const inputRef = useRef(null)
    const text = kinds[kind]
    const needle = query.trim().replace(/^@/, '')
    const pastedKey = text.roomKind ? joinKey(query) : null

    useEffect(() => {
        inputRef.current?.focus()
    }, [kind])

    useEffect(() => {
        if (needle.length < MIN_QUERY || pastedKey) return
        const timer = setTimeout(() => {
            (text.roomKind ? findRooms(needle, text.roomKind) : searchUsers(needle))
                .then((list) => setResults({ query: needle, list }))
                .catch(() => setResults({ query: needle, list: [] }))
        }, 250)
        return () => clearTimeout(timer)
    }, [kind, needle, pastedKey, text.roomKind])

    const searching = needle.length >= MIN_QUERY && !pastedKey
    const loading = searching && results.query !== needle
    const list = searching && !loading ? results.list : []
    const exact = text.roomKind ? [] : list.filter((person) => person.username.toLowerCase() === needle.toLowerCase())
    const similar = text.roomKind ? [] : list.filter((person) => !exact.includes(person))
    const rooms = text.roomKind ? list : []

    const renderRoom = (room) => (
        <button key={room.id} className={styles.person} type="button" onClick={() => onOpenRoom(room)}>
            <Avatar name={room.title} photo={room.photo} />
            <span className={styles.personText}>
                <span className={styles.personName}><HighlightText text={room.title} query={needle} /></span>
                <span className={styles.personUsername}>
                    @<HighlightText text={room.username} query={needle} />, {room.memberCount} {room.memberCount === 1 ? 'member' : 'members'}
                </span>
            </span>
        </button>
    )

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
                {!searching && !pastedKey && <div className={styles.note}>{text.hint}</div>}
                {pastedKey && (
                    <button className={`${styles.person} ${styles.personExact}`} type="button" onClick={() => onOpenKey(pastedKey)}>
                        <span className={styles.personText}>
                            <span className={styles.personName}>Open invite link</span>
                            <span className={styles.personUsername}>{pastedKey}</span>
                        </span>
                    </button>
                )}
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
                {rooms.map(renderRoom)}
            </div>
        </>
    )
}

export default FindPanel
