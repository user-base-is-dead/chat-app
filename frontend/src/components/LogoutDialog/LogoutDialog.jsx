import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import styles from './LogoutDialog.module.css'
import Avatar from '../Avatar/Avatar'
import LogoutIcon from '../Icons/LogoutIcon'

function LogoutDialog({ user, onConfirm, onClose }) {
    const name = user.displayName || user.username

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        window.addEventListener('keydown', handleKeyDown, true)
        return () => window.removeEventListener('keydown', handleKeyDown, true)
    }, [onClose])

    return createPortal(
        <div className={styles.backdrop} onClick={onClose}>
            <div className={styles.dialog} role="alertdialog" aria-modal="true" aria-labelledby="logout-title" onClick={(e) => e.stopPropagation()}>
                <span className={styles.handle} />
                <span className={styles.avatar}>
                    <Avatar name={name} size={76} />
                    <span className={styles.badge}><LogoutIcon /></span>
                </span>
                <span className={styles.name}>{name}</span>
                <span className={styles.username}>@{user.username}</span>
                <h2 id="logout-title" className={styles.title}>Log out of Civenth?</h2>
                <p className={styles.text}>Your chats stay saved. Sign in with your username and password to get back in.</p>
                <button className={styles.confirm} type="button" onClick={onConfirm} autoFocus>
                    <LogoutIcon />
                    Log Out
                </button>
                <button className={styles.cancel} type="button" onClick={onClose}>Cancel</button>
            </div>
        </div>,
        document.body,
    )
}

export default LogoutDialog
