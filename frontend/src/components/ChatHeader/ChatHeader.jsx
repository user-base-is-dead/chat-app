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
import LogoutIcon from '../Icons/LogoutIcon'
import PeerStatus from '../PeerStatus/PeerStatus'
import GroupsIcon from '../Icons/GroupsIcon'
import { roomLabel } from '../../utils/roomLink'

function ChatHeader({ chat, channelName, typing, typingName, onBack, onOpenInfo, onCall, onSearch, onToggleMute, onLeave, onDelete, onMembers, membersOpen }) {
    const [menu, setMenu] = useState(null)

    const items = chat.kind ? [
        { label: `${roomLabel(chat.kind)} Info`, Icon: ProfileIcon, onClick: onOpenInfo },
        chat.muted ? { label: 'Unmute', Icon: UnmuteIcon, onClick: onToggleMute } : { label: 'Mute', Icon: MuteIcon, onClick: onToggleMute },
        chat.role === 'owner'
            ? { label: `Delete ${roomLabel(chat.kind)}`, Icon: DeleteIcon, danger: true, onClick: onDelete }
            : { label: `Leave ${roomLabel(chat.kind)}`, Icon: LogoutIcon, danger: true, onClick: onLeave },
    ] : [
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
                <Avatar name={chat.name} photo={chat.photo} size={44} disabled={chat.peerDisabled} deleted={chat.peerDeleted} morphTarget />
                <div className={styles.peerText}>
                    <div className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>{channelName ? `# ${channelName}` : chat.name}</div>
                    <div className={styles.status}>
                        <PeerStatus key={chat.id} chat={chat} prefix={channelName ? chat.name : null} typing={typing} typingName={typingName} />
                    </div>
                </div>
            </button>
            <div className={styles.actions}>
                {!chat.kind && (
                    <button className={styles.action} type="button" aria-label="Call" onClick={onCall}>
                        <CallIcon />
                    </button>
                )}
                {onMembers && (
                    <button
                        className={membersOpen ? `${styles.action} ${styles.actionOn}` : styles.action}
                        type="button"
                        aria-label="Members"
                        aria-pressed={Boolean(membersOpen)}
                        onClick={onMembers}
                    >
                        <GroupsIcon />
                    </button>
                )}
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
