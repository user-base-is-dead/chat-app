import { useLayoutEffect, useRef } from 'react'
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
    const line = useRef(null)
    const scroll = status.text?.toLowerCase().startsWith('last seen')
    useLayoutEffect(() => {
        if (!scroll) return
        const node = line.current
        const frame = node.parentElement
        let animation
        const update = () => {
            animation?.cancel()
            const distance = node.offsetWidth - frame.clientWidth
            if (distance <= 0) return
            const travel = distance / 30 * 1000
            const duration = travel + 2000
            animation = node.animate([
                { transform: 'translateX(0)', offset: 0 },
                { transform: 'translateX(0)', offset: 1000 / duration },
                { transform: `translateX(-${distance}px)`, offset: (1000 + travel) / duration },
                { transform: `translateX(-${distance}px)`, offset: 1 },
            ], { duration, iterations: Infinity })
        }
        const observer = new ResizeObserver(update)
        observer.observe(frame)
        observer.observe(node)
        update()
        return () => { observer.disconnect(); animation?.cancel() }
    }, [scroll, status.text])
    return (
        <span ref={line} className={[styles.line, scroll && styles.scrollable, status.active && styles.active, status.typing && styles.typing].filter(Boolean).join(' ')}>
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
