import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './NameDialog.module.css'
import Input from '../Input/Input'
import Spinner from '../Spinner/Spinner'
import { NAME_LIMIT } from '../../utils/channelName'

function NameDialog({ title, label, initial = '', clean, onSubmit, onClose }) {
    const [value, setValue] = useState(initial)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const name = clean ? clean(value) : value.trim()
    const ready = name.length > 0 && [...name].length <= NAME_LIMIT

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    const submit = async (e) => {
        e.preventDefault()
        if (!ready || busy) return
        setBusy(true)
        setError('')
        try {
            await onSubmit(name)
            onClose()
        } catch (failure) {
            setError(failure.message)
            setBusy(false)
        }
    }

    return createPortal(
        <div className={styles.backdrop} onClick={busy ? undefined : onClose}>
            <form className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="name-title" onClick={(e) => e.stopPropagation()} onSubmit={submit}>
                <h2 id="name-title" className={styles.title}>{title}</h2>
                <Input label={label} value={value} onChange={(e) => setValue(e.target.value)} maxLength={64} />
                <p className={error ? `${styles.hint} ${styles.error}` : styles.hint}>
                    {error || (clean && name ? `Will be saved as #${name}` : `Up to ${NAME_LIMIT} characters`)}
                </p>
                <div className={styles.buttons}>
                    <button className={styles.cancel} type="button" onClick={onClose} disabled={busy}>Cancel</button>
                    <button className={styles.save} type="submit" disabled={!ready || busy}>
                        {busy ? <Spinner size={18} /> : 'Save'}
                    </button>
                </div>
            </form>
        </div>,
        document.body,
    )
}

export default NameDialog
