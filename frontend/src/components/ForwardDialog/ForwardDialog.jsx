import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './ForwardDialog.module.css'
import Avatar from '../Avatar/Avatar'
import CloseIcon from '../Icons/CloseIcon'
import SearchIcon from '../Icons/SearchIcon'
import ChevronIcon from '../Icons/ChevronIcon'
import HashIcon from '../Icons/HashIcon'

function ForwardDialog({ count, people, groups, channels, channelsByRoom, loadChannels, onPick, onClose }) {
    const [query, setQuery] = useState('')
    const [open, setOpen] = useState(() => new Set())

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    const needle = query.trim().replace(/^@/, '').toLowerCase()
    const matches = (chat) => chat.name.toLowerCase().includes(needle) || (chat.username ?? '').toLowerCase().includes(needle)
    const shownPeople = people.filter(matches)
    const shownGroups = groups.filter(matches)
    const shownChannels = channels.filter(matches)

    const toggleGroup = (group) => {
        const next = new Set(open)
        if (next.has(group.id)) next.delete(group.id)
        else {
            next.add(group.id)
            if (!channelsByRoom[group.id]) loadChannels(group.id)
        }
        setOpen(next)
    }

    const row = (chat, subtitle, onClick, chevron) => (
        <button key={chat.id} className={styles.row} type="button" onClick={onClick}>
            <Avatar name={chat.name} photo={chat.photo} size={42} />
            <span className={styles.rowText}>
                <span className={styles.rowName}>{chat.name}</span>
                {subtitle && <span className={styles.rowHint}>{subtitle}</span>}
            </span>
            {chevron && <span className={open.has(chat.id) ? `${styles.chevron} ${styles.chevronOpen}` : styles.chevron}><ChevronIcon /></span>}
        </button>
    )

    const empty = shownPeople.length === 0 && shownGroups.length === 0 && shownChannels.length === 0

    return createPortal(
        <div className={styles.backdrop} onClick={onClose}>
            <div className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="forward-title" onClick={(e) => e.stopPropagation()}>
                <div className={styles.head}>
                    <h2 id="forward-title" className={styles.title}>{count === 1 ? 'Forward message' : `Forward ${count} messages`}</h2>
                    <button className={styles.close} type="button" aria-label="Close" onClick={onClose}>
                        <CloseIcon />
                    </button>
                </div>
                <label className={styles.search}>
                    <SearchIcon />
                    <input autoFocus value={query} placeholder="Search" onChange={(e) => setQuery(e.target.value)} />
                </label>
                <div className={styles.list}>
                    {empty && <p className={styles.empty}>Nothing found</p>}
                    {shownPeople.length > 0 && <p className={styles.section}>Chats</p>}
                    {shownPeople.map((chat) => row(chat, chat.username ? `@${chat.username}` : '', () => onPick({ id: chat.id, name: chat.name })))}
                    {shownGroups.length > 0 && <p className={styles.section}>Groups</p>}
                    {shownGroups.map((group) => {
                        const list = channelsByRoom[group.id]
                        const writable = list?.channels.filter((channel) => channel.permissions?.includes('sendMessages')) ?? []
                        return (
                            <div key={group.id}>
                                {row(group, open.has(group.id) ? 'Pick a channel' : 'Group', () => toggleGroup(group), true)}
                                {open.has(group.id) && !list && <p className={styles.note}>Loading channels</p>}
                                {open.has(group.id) && list && writable.length === 0 && <p className={styles.note}>You cannot send messages here</p>}
                                {open.has(group.id) && writable.map((channel) => (
                                    <button key={channel.id} className={styles.channel} type="button" onClick={() => onPick({ id: channel.id, name: `${group.name} #${channel.name}` })}>
                                        <HashIcon />
                                        <span>{channel.name}</span>
                                    </button>
                                ))}
                            </div>
                        )
                    })}
                    {shownChannels.length > 0 && <p className={styles.section}>Channels</p>}
                    {shownChannels.map((chat) => row(chat, 'Channel', () => onPick({ id: chat.id, name: chat.name })))}
                </div>
            </div>
        </div>,
        document.body,
    )
}

export default ForwardDialog
