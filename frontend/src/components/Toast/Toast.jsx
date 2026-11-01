import styles from './Toast.module.css'

function Toast({ text }) {
    return <div className={styles.toast} role="status">{text}</div>
}

export default Toast
