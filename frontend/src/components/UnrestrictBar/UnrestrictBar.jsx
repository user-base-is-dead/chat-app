import { useState } from 'react'
import styles from './UnrestrictBar.module.css'
import Spinner from '../Spinner/Spinner'

function UnrestrictBar({ onUnrestrict }) {
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
                {busy ? <Spinner size={20} /> : 'Unrestrict to chat'}
            </button>
        </div>
    )
}

export default UnrestrictBar
