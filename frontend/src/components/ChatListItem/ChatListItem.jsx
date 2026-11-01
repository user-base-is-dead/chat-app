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
import { formatCount } from '../../utils/formatCount'

function ChatListItem({ chat, lastMessage, active, leaving, query, onClick, onPin, onMute, onRestrict, onUnrestrict, onDelete }) {
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
        { label: 'Copy ID', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(chat.peerId).catch(() => {}) },
        { label: 'Delete Chat', Icon: DeleteIcon, danger: true, onClick: onDelete },
    ].filter(Boolean)

    const openMenu = (e) => {
        e.preventDefault()
        setMenu({ x: e.clientX, y: e.clientY })
    }

    return (
        <div
            className={[active ? styles.itemActive : styles.item, leaving && styles.leaving].filter(Boolean).join(' ')}
            ref={itemRef}
            onClick={onClick}
            onContextMenu={onDelete ? openMenu : undefined}
        >
            <Avatar name={chat.name} disabled={chat.peerDisabled} deleted={chat.peerDeleted} />
            <div className={styles.info}>
                <div className={styles.top}>
                    <span className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>
                        <HighlightText text={chat.name} query={query} />
                        {chat.muted && <span className={styles.mutedMark}><MuteIcon /></span>}
                    </span>
                    <span className={styles.time}>{lastMessage ? lastMessage.time : ''}</span>
                </div>
                <div className={styles.bottom}>
                    <div className={styles.lastMessage}>{lastMessage ? <HighlightText text={lastMessage.text} query={query} /> : ''}</div>
                    {chat.unreadCount > 0 && <span className={chat.muted ? `${styles.unread} ${styles.unreadMuted}` : styles.unread}>{formatCount(chat.unreadCount)}</span>}
                    {chat.unreadCount === 0 && chat.pinned && <span className={styles.pinnedMark}><PinIcon /></span>}
                </div>
            </div>
            {menu && <ContextMenu x={menu.x} y={menu.y} items={items} onClose={() => setMenu(null)} />}
        </div>
    )
}

export default ChatListItem
