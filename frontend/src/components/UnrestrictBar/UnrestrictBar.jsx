import { useState } from 'react'
import styles from './UnrestrictBar.module.css'
import Spinner from '../Spinner/Spinner'

function UnrestrictBar({ label = 'Unrestrict to chat', onUnrestrict }) {
    const [busy, setBusy] = useState(false)

    const unrestrict = async () => {
        if (busy) return
        setBusy(true)
        await onUnrestrict()
        setBusy(false)
    }

    return (
        <div className={styles.wrap}>
            <button className={styles.button} type="button" onClick={unrestrict} disabled={busy}>
                {busy ? <Spinner size={20} /> : label}
            </button>
        </div>
    )
}

export default UnrestrictBar
