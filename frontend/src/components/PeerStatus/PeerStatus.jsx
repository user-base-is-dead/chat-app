import styles from './PeerStatus.module.css'
import { peerStatus } from '../../utils/lastSeen'
import { roomStatus } from '../../utils/roomLink'

function readStatus(chat, typing, typingName, prefix) {
    if (chat.kind) {
        const typingText = typing ? `${typingName} is typing` : null
        const idle = prefix ? `${prefix} · ${roomStatus(chat)}` : roomStatus(chat)
        return { text: typingText ?? idle, active: typing, typing, typingText }
    }
    const hidden = chat.peerDeleted || chat.peerDisabled
    return {
        text: peerStatus(chat, typing),
        active: !hidden && (typing || chat.peerOnline),
        typing: !hidden && typing,
    }
}

function StatusLine({ status }) {
    return (
        <span className={[styles.line, status.active && styles.active, status.typing && styles.typing].filter(Boolean).join(' ')}>
            {status.typing ? (
                <>
                    <span className={styles.dots} aria-hidden="true"><i /><i /><i /></span>
                    {status.typingText ?? 'typing'}
                </>
            ) : status.text}
        </span>
    )
}

function PeerStatus({ chat, typing, typingName, centered, prefix }) {
    const next = readStatus(chat, typing, typingName, prefix)

    return (
        <span className={centered ? `${styles.status} ${styles.centered}` : styles.status}>
            <StatusLine status={next} />
        </span>
    )
}

export default PeerStatus
