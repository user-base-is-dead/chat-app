import styles from './MessageExtras.module.css'
import ForwardIcon from '../Icons/ForwardIcon'
import ReplyIcon from '../Icons/ReplyIcon'
import EditIcon from '../Icons/EditIcon'
import CloseIcon from '../Icons/CloseIcon'

export function ReplyQuote({ reply, nameColor, onOpen }) {
    return (
        <button className={styles.quote} type="button" onClick={(e) => { e.stopPropagation(); onOpen?.(reply.id) }}>
            {reply.deleted ? (
                <span className={styles.gone}>Original message was deleted</span>
            ) : (
                <>
                    <span className={styles.quoteName} style={nameColor ? { color: nameColor } : undefined}>{reply.senderName || 'Deleted Account'}</span>
                    <span className={styles.quoteText}>{reply.text}</span>
                </>
            )}
        </button>
    )
}

export function Forwarded({ name }) {
    return (
        <span className={styles.forwarded}>
            <ForwardIcon />
            Forwarded from {name || 'Deleted Account'}
        </span>
    )
}

export function Edited() {
    return <span className={styles.edited}>(edited)</span>
}

export function InputStrip({ mode, name, onClose }) {
    const editing = mode.kind === 'edit'
    return (
        <div className={styles.strip}>
            <span className={styles.stripIcon}>{editing ? <EditIcon /> : <ReplyIcon />}</span>
            <span className={styles.stripBody}>
                <span className={styles.stripTitle}>{editing ? 'Edit message' : `Reply to ${name || 'Deleted Account'}`}</span>
                <span className={styles.stripText}>{mode.message.text}</span>
            </span>
            <button className={styles.stripClose} type="button" aria-label="Cancel" onClick={onClose}>
                <CloseIcon />
            </button>
        </div>
    )
}
