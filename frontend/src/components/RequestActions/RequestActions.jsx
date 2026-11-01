import styles from './RequestActions.module.css'

function RequestActions({ name, onAccept, onDelete }) {
    return (
        <div className={styles.wrap}>
            <p className={styles.text}>{name} wants to send you a message</p>
            <div className={styles.buttons}>
                <button className={styles.delete} type="button" onClick={onDelete}>Delete</button>
                <button className={styles.accept} type="button" onClick={onAccept}>Accept</button>
            </div>
        </div>
    )
}

export default RequestActions
