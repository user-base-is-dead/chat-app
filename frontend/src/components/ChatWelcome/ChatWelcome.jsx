import styles from './ChatWelcome.module.css'
import LockIcon from '../Icons/LockIcon'

function ChatWelcome({ name }) {
    return (
        <div className={styles.welcome}>
            <div className={styles.hello}>
                <h1 className={styles.headline}>
                    Hi, <span className={styles.name}>{name}</span>
                </h1>
                <p className={styles.hint}>Select a chat to start messaging</p>
            </div>
            <div className={styles.info}>
                <span className={styles.badge}>
                    <LockIcon />
                </span>
                <div className={styles.infoText}>
                    <h2 className={styles.title}>Miron Chat</h2>
                    <ul className={styles.points}>
                        <li>Every message is end-to-end encrypted. Only you and the person you're talking to can read it.</li>
                        <li>We never share your data with any government or third party.</li>
                    </ul>
                </div>
            </div>
        </div>
    )
}

export default ChatWelcome
