import styles from './UpdateGate.module.css'
import { useAppUpdate } from '../../utils/appUpdate'

const reload = () => window.location.reload()

function UpdateGate({ children }) {
    const { required, latest } = useAppUpdate()

    if (required) {
        return (
            <div className={styles.required}>
                <div className={styles.card}>
                    <img className={styles.logo} src="/apple-touch-icon.png" alt="Civenth" />
                    <h1 className={styles.title}>Update required</h1>
                    <p className={styles.text}>This version of Civenth is no longer supported. Reload to get the latest version.</p>
                    {latest?.notes && <p className={styles.notes}>{latest.notes}</p>}
                    <button className={styles.primary} type="button" onClick={reload}>Reload now</button>
                </div>
            </div>
        )
    }

    return children
}

export default UpdateGate
