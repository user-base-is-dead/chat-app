import { useState } from 'react'
import styles from './UserInfo.module.css'
import Avatar from '../Avatar/Avatar'
import CloseIcon from '../Icons/CloseIcon'
import BackIcon from '../Icons/BackIcon'
import MuteIcon from '../Icons/MuteIcon'
import UnmuteIcon from '../Icons/UnmuteIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import { peerStatus } from '../../utils/lastSeen'

function UserInfo({ chat, typing, onClose, onToggleMute, onDelete }) {
    const [copied, setCopied] = useState(null)
    const hidden = chat.peerDisabled || chat.peerDeleted

    const copy = async (key, value) => {
        try {
            await navigator.clipboard.writeText(value)
            setCopied(key)
            setTimeout(() => setCopied(null), 1500)
        } catch {
            setCopied(null)
        }
    }

    return (
        <aside className={styles.panel}>
            <div className={styles.top}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onClose}>
                    <BackIcon />
                </button>
                <span className={styles.title}>User Info</span>
                <button className={styles.close} type="button" aria-label="Close" onClick={onClose}>
                    <CloseIcon />
                </button>
            </div>
            <div className={styles.body}>
                <div className={styles.hero}>
                    <Avatar name={chat.name} size={110} disabled={chat.peerDisabled} deleted={chat.peerDeleted} />
                    <div className={chat.peerDisabled ? `${styles.name} ${styles.nameDisabled}` : chat.peerDeleted ? `${styles.name} ${styles.nameDeleted}` : styles.name}>{chat.name}</div>
                    {peerStatus(chat, typing) && (
                        <div className={typing || chat.peerOnline ? `${styles.status} ${styles.statusActive}` : styles.status}>{peerStatus(chat, typing)}</div>
                    )}
                </div>
                <div className={styles.card}>
                    {hidden ? (
                        <div className={styles.row}>
                            <span className={`${styles.value} ${styles.valueMuted}`}>{chat.peerDeleted ? 'Deleted user' : 'Disabled user'}</span>
                            <span className={styles.label}>Username</span>
                        </div>
                    ) : (
                        <button className={styles.row} type="button" onClick={() => copy('username', `@${chat.username}`)}>
                            <span className={styles.value}>{copied === 'username' ? 'Copied' : `@${chat.username}`}</span>
                            <span className={styles.label}>Username</span>
                        </button>
                    )}
                    {!hidden && chat.peerBio && (
                        <div className={styles.row}>
                            <span className={styles.bio}>{chat.peerBio}</span>
                            <span className={styles.label}>Bio</span>
                        </div>
                    )}
                    <button className={styles.row} type="button" onClick={() => copy('id', chat.peerId)}>
                        <span className={styles.value}>{copied === 'id' ? 'Copied' : chat.peerId}</span>
                        <span className={styles.label}>ID</span>
                    </button>
                </div>
                <div className={styles.card}>
                    <button className={styles.action} type="button" onClick={onToggleMute}>
                        {chat.muted ? <UnmuteIcon /> : <MuteIcon />}
                        {chat.muted ? 'Unmute' : 'Mute'}
                    </button>
                    <button className={`${styles.action} ${styles.danger}`} type="button" onClick={onDelete}>
                        <DeleteIcon />
                        Delete Chat
                    </button>
                </div>
            </div>
        </aside>
    )
}

export default UserInfo
