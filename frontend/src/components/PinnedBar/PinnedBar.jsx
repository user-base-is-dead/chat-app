import styles from './PinnedBar.module.css'
import PinIcon from '../Icons/PinIcon'
import CloseIcon from '../Icons/CloseIcon'

function PinnedBar({ message, canUnpin, onOpen, onUnpin }) {
    return (
        <div className={styles.bar}>
            <button className={styles.open} type="button" onClick={onOpen}>
                <span className={styles.icon}><PinIcon /></span>
                <span className={styles.texts}>
                    <span className={styles.label}>Pinned message</span>
                    <span className={styles.text}>{message.text}</span>
                </span>
            </button>
            {canUnpin && (
                <button className={styles.close} type="button" aria-label="Unpin" title="Unpin" onClick={onUnpin}>
                    <CloseIcon />
                </button>
            )}
        </div>
    )
}

export default PinnedBar
