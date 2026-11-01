import styles from './Skeleton.module.css'

const bubbles = [
    ['left', 180], ['left', 120], ['right', 210], ['left', 150],
    ['right', 140], ['right', 230], ['left', 190], ['right', 120],
]

function MessageSkeleton() {
    return (
        <div className={styles.bubbles} aria-hidden="true">
            {bubbles.map(([side, width], i) => (
                <div key={i} className={`${styles.bubble} ${styles.shimmer} ${styles[side]}`} style={{ width }} />
            ))}
        </div>
    )
}

export default MessageSkeleton
