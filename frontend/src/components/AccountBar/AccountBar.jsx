import styles from './AccountBar.module.css'

function AccountBar({ text }) {
    return (
        <div className={styles.wrap}>
            <div className={styles.bar}>{text}</div>
        </div>
    )
}

export default AccountBar
