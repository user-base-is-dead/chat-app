import styles from './Skeleton.module.css'

const widths = [[46, 78], [38, 64], [52, 70], [34, 58], [44, 82], [40, 66], [48, 74], [36, 60]]

function ChatListSkeleton() {
    return (
        <div aria-hidden="true">
            {widths.map(([name, text], i) => (
                <div key={i} className={styles.row}>
                    <div className={`${styles.avatar} ${styles.shimmer}`} />
                    <div className={styles.lines}>
                        <div className={`${styles.line} ${styles.shimmer}`} style={{ width: `${name}%` }} />
                        <div className={`${styles.line} ${styles.shimmer}`} style={{ width: `${text}%` }} />
                    </div>
                </div>
            ))}
        </div>
    )
}

export default ChatListSkeleton
