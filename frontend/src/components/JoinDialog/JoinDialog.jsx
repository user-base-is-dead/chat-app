import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './JoinDialog.module.css'
import Avatar from '../Avatar/Avatar'
import Spinner from '../Spinner/Spinner'
import { roomLabel } from '../../utils/roomLink'

function JoinDialog({ room, error, onJoin, onClose }) {
    const [busy, setBusy] = useState(false)
    const [failure, setFailure] = useState('')

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    const join = async () => {
        if (busy) return
        setBusy(true)
        setFailure('')
        try {
            await onJoin()
        } catch (joinError) {
            setFailure(joinError.message)
            setBusy(false)
        }
    }

    return createPortal(
        <div className={styles.backdrop} onClick={busy ? undefined : onClose}>
            <div className={styles.dialog} role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
                {room ? (
                    <>
                        <Avatar name={room.title} photo={room.photo} size={88} />
                        <h2 className={styles.title}>{room.title}</h2>
                        <p className={styles.count}>{roomLabel(room.kind)}, {room.memberCount} {room.memberCount === 1 ? 'member' : 'members'}</p>
                        {room.about && <p className={styles.about}>{room.about}</p>}
                        {failure && <p className={styles.error}>{failure}</p>}
                        <button className={styles.join} type="button" disabled={busy} onClick={join}>
                            {busy ? <Spinner size={20} /> : 'Join'}
                        </button>
                    </>
                ) : (
                    <p className={styles.about}>{error}</p>
                )}
                <button className={styles.cancel} type="button" disabled={busy} onClick={onClose}>Close</button>
            </div>
        </div>,
        document.body,
    )
}

export default JoinDialog
