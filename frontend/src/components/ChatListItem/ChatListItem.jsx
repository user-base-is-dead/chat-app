import { useLayoutEffect, useRef, useState } from 'react'
import styles from './ChatListItem.module.css'
import Avatar from '../Avatar/Avatar'
import ContextMenu from '../ContextMenu/ContextMenu'
import PinIcon from '../Icons/PinIcon'
import MuteIcon from '../Icons/MuteIcon'
import SelectIcon from '../Icons/SelectIcon'
import TickIcon from '../Icons/TickIcon'
import HighlightText from '../HighlightText/HighlightText'
import { formatCount } from '../../utils/formatCount'
import { chatStyle } from '../../utils/chroma'
import { chatActions } from '../../utils/chatActions'

const HOLD_MS = 450
const HOLD_SLOP = 10

const pad = (value) => String(value).padStart(2, '0')

function dateTime(ms) {
    const day = new Date(ms)
    return `${pad(day.getDate())}/${pad(day.getMonth() + 1)}/${String(day.getFullYear()).slice(2)} ${pad(day.getHours())}:${pad(day.getMinutes())}`
}

function ChatListItem({ chat, index = 0, lastMessage, active, leaving, query, selected, onSelect, onClick, onPin, onMute, onRestrict, onUnrestrict, onLeave, onDelete }) {
    const [menu, setMenu] = useState(null)
    const itemRef = useRef(null)
    const hold = useRef(null)

    useLayoutEffect(() => {
        if (leaving) itemRef.current.style.setProperty('--height', `${itemRef.current.offsetHeight}px`)
    }, [leaving])

    const items = [
        onSelect && { label: 'Select', Icon: SelectIcon, onClick: onSelect },
        ...chatActions(chat, { onPin, onMute, onRestrict, onUnrestrict, onLeave, onDelete }),
    ].filter(Boolean)

    const senderPrefix = chat.kind === 'group' && lastMessage?.senderId
        ? `${lastMessage.mine ? 'You' : lastMessage.senderName || 'Deleted Account'}: `
        : ''

    const openMenu = (e) => {
        e.preventDefault()
        if (hold.current?.touch) return
        setMenu({ x: e.clientX, y: e.clientY })
    }

    const endHold = () => {
        if (hold.current) clearTimeout(hold.current.timer)
    }

    const startHold = (e) => {
        endHold()
        hold.current = null
        if (!onSelect || e.button > 0) return
        const current = { x: e.clientX, y: e.clientY, touch: e.pointerType !== 'mouse', fired: false }
        current.timer = setTimeout(() => {
            current.fired = true
            onSelect()
        }, HOLD_MS)
        hold.current = current
    }

    const moveHold = (e) => {
        const current = hold.current
        if (current && Math.hypot(e.clientX - current.x, e.clientY - current.y) > HOLD_SLOP) endHold()
    }

    const handleClick = (e) => {
        if (hold.current?.fired) {
            hold.current = null
            return
        }
        onClick?.(e)
    }

    const itemClass = [
        active ? styles.itemActive : styles.item,
        selected && styles.itemSelected,
        onSelect && styles.holdable,
        leaving && styles.leaving,
    ].filter(Boolean).join(' ')

    return (
        <div
            className={itemClass}
            ref={itemRef}
            data-zone="chat"
            data-chat-id={chat.id}
            style={{ ...chatStyle(chat), '--i': Math.min(index, 12) }}
            onClick={handleClick}
            onContextMenu={onDelete || onLeave ? openMenu : undefined}
            onPointerDown={startHold}
            onPointerMove={moveHold}
            onPointerUp={endHold}
            onPointerCancel={endHold}
            onPointerLeave={endHold}
        >
            <span className={styles.avatar}>
                <Avatar name={chat.name} photo={chat.photo} size={52} disabled={chat.peerDisabled} deleted={chat.peerDeleted} />
                {!selected && !chat.kind && chat.peerOnline && !chat.peerDisabled && !chat.peerDeleted && <span className={styles.onlineDot} role="img" aria-label="Online" />}
                {selected && <span className={styles.check}><TickIcon /></span>}
            </span>
            <div className={styles.info}>
                <div className={styles.top}>
                    <span className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>
                        <HighlightText text={chat.name} query={query} />
                        {chat.muted && <span className={styles.mutedMark}><MuteIcon /></span>}
                    </span>
                    <span className={styles.time}>{lastMessage ? (lastMessage.createdAt ? dateTime(lastMessage.createdAt) : lastMessage.time) : ''}</span>
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
