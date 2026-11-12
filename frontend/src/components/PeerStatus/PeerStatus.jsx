import { useState } from 'react'
import styles from './PeerStatus.module.css'
import { peerStatus } from '../../utils/lastSeen'
import { roomStatus } from '../../utils/roomLink'

function readStatus(chat, typing, typingName) {
    if (chat.kind) {
        const typingText = typing ? `${typingName} is typing` : null
        return { text: typingText ?? roomStatus(chat), active: typing, typing, typingText }
    }
    const hidden = chat.peerDeleted || chat.peerDisabled
    return {
        text: peerStatus(chat, typing),
        active: !hidden && (typing || chat.peerOnline),
        typing: !hidden && typing,
    }
}

function StatusLine({ status, motion, onAnimationEnd }) {
    return (
        <span className={[styles.line, status.active && styles.active, status.typing && styles.typing, motion].filter(Boolean).join(' ')} onAnimationEnd={onAnimationEnd}>
            {status.typing ? (
                <>
                    <span className={styles.dots} aria-hidden="true"><i /><i /><i /></span>
                    {status.typingText ?? 'typing'}
                </>
            ) : status.text}
        </span>
    )
}

function PeerStatus({ chat, typing, typingName, centered }) {
    const next = readStatus(chat, typing, typingName)
    const [shown, setShown] = useState(next)
    const [leaving, setLeaving] = useState(null)
    const [changes, setChanges] = useState(0)

    if (next.text !== shown.text) {
        setLeaving(shown)
        setShown(next)
        setChanges(changes + 1)
    }

    const finishLeaving = (e) => {
        if (e.target === e.currentTarget) setLeaving(null)
    }

    return (
        <span className={centered ? `${styles.status} ${styles.centered}` : styles.status}>
            {leaving && <StatusLine key={`out-${changes}`} status={leaving} motion={styles.leave} onAnimationEnd={finishLeaving} />}
            <StatusLine key={changes} status={next} motion={changes > 0 ? styles.enter : undefined} />
        </span>
    )
}

export default PeerStatus
