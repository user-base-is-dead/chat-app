import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './ConfirmDialog.module.css'
import Spinner from '../Spinner/Spinner'

function ConfirmDialog({ title, text, confirmLabel, onConfirm, onClose }) {
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    const confirm = async () => {
        if (busy) return
        setBusy(true)
        await onConfirm()
        onClose()
    }

    return createPortal(
        <div className={styles.backdrop} onClick={busy ? undefined : onClose}>
            <div className={styles.dialog} role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" onClick={(e) => e.stopPropagation()}>
                <h2 id="confirm-title" className={styles.title}>{title}</h2>
                <p className={styles.text}>{text}</p>
                <div className={styles.buttons}>
                    <button className={styles.cancel} type="button" onClick={onClose} disabled={busy}>No</button>
                    <button className={styles.confirm} type="button" onClick={confirm} disabled={busy} autoFocus>
                        {busy ? <Spinner size={18} /> : confirmLabel}
                    </button>
                </div>
            </div>
        </div>,
        document.body,
    )
}

export default ConfirmDialog
