import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './PostDialog.module.css'
import Spinner from '../Spinner/Spinner'

const switchRows = [
    { key: 'allowComments', label: 'Comments', on: 'People can comment on this post', off: 'Nobody can comment on this post' },
    { key: 'allowReactions', label: 'Reactions', on: 'People can react to this post', off: 'Nobody can react to this post' },
]

function PostDialog({ title, text, switches, actionLabel, onSubmit, onClose }) {
    const [draft, setDraft] = useState(text)
    const [values, setValues] = useState(switches)
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            e.preventDefault()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    const submit = async (e) => {
        e.preventDefault()
        if (busy || !draft.trim()) return
        setBusy(true)
        const done = await onSubmit(draft.trim(), values)
        if (done === false) setBusy(false)
        else onClose()
    }

    return createPortal(
        <div className={styles.backdrop} onClick={busy ? undefined : onClose}>
            <form className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="post-dialog-title" onClick={(e) => e.stopPropagation()} onSubmit={submit}>
                <h2 id="post-dialog-title" className={styles.title}>{title}</h2>
                <textarea
                    className={styles.text}
                    value={draft}
                    rows={4}
                    maxLength={4000}
                    autoFocus
                    onChange={(e) => setDraft(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) submit(e)
                    }}
                />
                <div className={styles.switches}>
                    {switchRows.map(({ key, label, on, off }) => (
                        <label key={key} className={styles.switchRow}>
                            <span className={styles.switchText}>
                                <span className={styles.switchLabel}>{label} {values[key] ? 'on' : 'off'}</span>
                                <span className={styles.switchHint}>{values[key] ? on : off}</span>
                            </span>
                            <input
                                className={styles.switch}
                                type="checkbox"
                                checked={values[key]}
                                onChange={(e) => setValues({ ...values, [key]: e.target.checked })}
                            />
                        </label>
                    ))}
                </div>
                <div className={styles.buttons}>
                    <button className={styles.cancel} type="button" onClick={onClose} disabled={busy}>Cancel</button>
                    <button className={styles.confirm} type="submit" disabled={busy || !draft.trim()}>
                        {busy ? <Spinner size={18} /> : actionLabel}
                    </button>
                </div>
            </form>
        </div>,
        document.body,
    )
}

export default PostDialog
