import { useState } from 'react'
import styles from './ChatHeader.module.css'
import Avatar from '../Avatar/Avatar'
import ContextMenu from '../ContextMenu/ContextMenu'
import BackIcon from '../Icons/BackIcon'
import CallIcon from '../Icons/CallIcon'
import ChatSearchIcon from '../Icons/ChatSearchIcon'
import MoreIcon from '../Icons/MoreIcon'
import ProfileIcon from '../Icons/ProfileIcon'
import MuteIcon from '../Icons/MuteIcon'
import UnmuteIcon from '../Icons/UnmuteIcon'
import CopyIcon from '../Icons/CopyIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import { peerStatus } from '../../utils/lastSeen'

function ChatHeader({ chat, typing, onBack, onOpenInfo, onCall, onSearch, onToggleMute, onDelete }) {
    const [menu, setMenu] = useState(null)
    const status = peerStatus(chat, typing)

    const items = [
        { label: 'View Profile', Icon: ProfileIcon, onClick: onOpenInfo },
        chat.muted ? { label: 'Unmute', Icon: UnmuteIcon, onClick: onToggleMute } : { label: 'Mute', Icon: MuteIcon, onClick: onToggleMute },
        { label: 'Copy ID', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(chat.peerId).catch(() => {}) },
        { label: 'Delete Chat', Icon: DeleteIcon, danger: true, onClick: onDelete },
    ]

    const openMenu = (e) => {
        const rect = e.currentTarget.getBoundingClientRect()
        setMenu({ x: rect.right, y: rect.bottom + 6 })
    }

    return (
        <div className={styles.header}>
            <button className={styles.backButton} onClick={onBack}>
                <BackIcon />
            </button>
            <button className={styles.peer} type="button" onClick={onOpenInfo}>
                <Avatar name={chat.name} size={40} disabled={chat.peerDisabled} deleted={chat.peerDeleted} />
                <div className={styles.peerText}>
                    <div className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>{chat.name}</div>
                    <div className={typing || chat.peerOnline ? `${styles.status} ${styles.statusActive}` : styles.status}>{status}</div>
                </div>
            </button>
            <div className={styles.actions}>
                <button className={styles.action} type="button" aria-label="Call" onClick={onCall}>
                    <CallIcon />
                </button>
                <button className={styles.action} type="button" aria-label="Search" onClick={onSearch}>
                    <ChatSearchIcon />
                </button>
                <button className={styles.action} type="button" aria-label="More" onClick={openMenu}>
                    <MoreIcon />
                </button>
            </div>
            {menu && <ContextMenu x={menu.x} y={menu.y} items={items} onClose={() => setMenu(null)} />}
        </div>
    )
}

export default ChatHeader
