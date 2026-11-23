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
                <span className={styles.label}>
                    <LockIcon />
                    Private by design
                </span>
                <div className={styles.facts}>
                    <p>Only you and the person you're talking to can read your messages. End-to-end encrypted, always.</p>
                    <p>Nothing you send is shared with any government or third party.</p>
                </div>
            </div>
        </div>
    )
}

export default ChatWelcome
