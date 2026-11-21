import { useLayoutEffect, useRef, useState } from 'react'
import styles from './ChatListItem.module.css'
import Avatar from '../Avatar/Avatar'
import ContextMenu from '../ContextMenu/ContextMenu'
import PinIcon from '../Icons/PinIcon'
import MuteIcon from '../Icons/MuteIcon'
import UnmuteIcon from '../Icons/UnmuteIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import HighlightText from '../HighlightText/HighlightText'
import RestrictIcon from '../Icons/RestrictIcon'
import CopyIcon from '../Icons/CopyIcon'
import LogoutIcon from '../Icons/LogoutIcon'
import { formatCount } from '../../utils/formatCount'
import { chatStyle } from '../../utils/chroma'
import { roomLabel } from '../../utils/roomLink'

const pad = (value) => String(value).padStart(2, '0')

function dateTime(ms) {
    const day = new Date(ms)
    return `${pad(day.getDate())}/${pad(day.getMonth() + 1)}/${String(day.getFullYear()).slice(2)} ${pad(day.getHours())}:${pad(day.getMinutes())}`
}

function ChatListItem({ chat, index = 0, lastMessage, active, leaving, query, withDate, onClick, onPin, onMute, onRestrict, onUnrestrict, onLeave, onDelete }) {
    const [menu, setMenu] = useState(null)
    const itemRef = useRef(null)

    useLayoutEffect(() => {
        if (leaving) itemRef.current.style.setProperty('--height', `${itemRef.current.offsetHeight}px`)
    }, [leaving])

    const items = [
        onPin && { label: chat.pinned ? 'Unpin' : 'Pin', Icon: PinIcon, onClick: onPin },
        onMute && (chat.muted ? { label: 'Unmute', Icon: UnmuteIcon, onClick: onMute } : { label: 'Mute', Icon: MuteIcon, onClick: onMute }),
        onRestrict && { label: 'Restrict', Icon: RestrictIcon, onClick: onRestrict },
        onUnrestrict && { label: 'Unrestrict', Icon: RestrictIcon, onClick: onUnrestrict },
        !chat.kind && { label: 'Copy ID', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(chat.peerId).catch(() => {}) },
        onLeave && { label: `Leave ${roomLabel(chat.kind)}`, Icon: LogoutIcon, danger: true, onClick: onLeave },
        onDelete && { label: chat.kind ? `Delete ${roomLabel(chat.kind)}` : 'Delete Chat', Icon: DeleteIcon, danger: true, onClick: onDelete },
    ].filter(Boolean)

    const senderPrefix = chat.kind === 'group' && lastMessage?.senderId
        ? `${lastMessage.mine ? 'You' : lastMessage.senderName || 'Deleted Account'}: `
        : ''

    const openMenu = (e) => {
        e.preventDefault()
        setMenu({ x: e.clientX, y: e.clientY })
    }

    return (
        <div
            className={[active ? styles.itemActive : styles.item, leaving && styles.leaving].filter(Boolean).join(' ')}
            ref={itemRef}
            data-chat-id={chat.id}
            style={{ ...chatStyle(chat), '--i': Math.min(index, 12) }}
            onClick={onClick}
            onContextMenu={onDelete || onLeave ? openMenu : undefined}
        >
            <Avatar name={chat.name} photo={chat.photo} size={52} disabled={chat.peerDisabled} deleted={chat.peerDeleted} />
            <div className={styles.info}>
                <div className={styles.top}>
                    <span className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>
                        <HighlightText text={chat.name} query={query} />
                        {chat.muted && <span className={styles.mutedMark}><MuteIcon /></span>}
                    </span>
                    <span className={styles.time}>{lastMessage ? (withDate && lastMessage.createdAt ? dateTime(lastMessage.createdAt) : lastMessage.time) : ''}</span>
                </div>
                <div className={styles.bottom}>
                    <div className={styles.lastMessage}>{lastMessage ? <HighlightText text={`${senderPrefix}${lastMessage.text}`} query={query} /> : ''}</div>
                    {chat.unreadCount > 0 && <span className={chat.muted ? `${styles.unread} ${styles.unreadMuted}` : styles.unread}>{formatCount(chat.unreadCount)}</span>}
                    {chat.unreadCount === 0 && chat.pinned && <span className={styles.pinnedMark}><PinIcon /></span>}
                </div>
            </div>
            {menu && <ContextMenu x={menu.x} y={menu.y} items={items} onClose={() => setMenu(null)} />}
        </div>
    )
}

export default ChatListItem
