import styles from './Spinner.module.css'

function Spinner({ size = 20 }) {
    return <span className={styles.spinner} style={{ width: size, height: size }} role="status" aria-label="Loading" />
}

export default Spinner
