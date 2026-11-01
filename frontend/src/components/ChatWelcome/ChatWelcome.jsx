import styles from './ChatWelcome.module.css'
import LockIcon from '../Icons/LockIcon'

function ChatWelcome() {
    return (
        <div className={styles.welcome}>
            <div className={styles.card}>
                <div className={styles.info}>
                    <div className={styles.badge}>
                        <LockIcon />
                    </div>
                    <h2 className={styles.title}>Miron Chat</h2>
                    <ul className={styles.points}>
                        <li>Every message is end-to-end encrypted. Only you and the person you're talking to can read it.</li>
                        <li>We never share your data with any government or third party.</li>
                    </ul>
                </div>
                <p className={styles.hint}>Select a chat to start messaging</p>
            </div>
        </div>
    )
}

export default ChatWelcome
