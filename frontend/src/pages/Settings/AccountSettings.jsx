import { useState } from 'react'
import styles from './SettingsPage.module.css'
import BackIcon from '../../components/Icons/BackIcon'
import Input from '../../components/Input/Input'
import Spinner from '../../components/Spinner/Spinner'
import { deleteAccount, disableAccount } from '../../api/users'

const options = {
    disable: {
        title: 'Disable account',
        text: 'Your account is hidden from search and you are logged out on every device. Log in again any time to turn it back on.',
        confirm: 'Disable account',
        action: disableAccount,
    },
    delete: {
        title: 'Delete account',
        text: 'Your account and profile are deleted forever and you are logged out on every device. People you talked to keep their chats, shown as Deleted Account. This cannot be undone.',
        confirm: 'Delete account forever',
        action: deleteAccount,
    },
}

function AccountSettings({ onBack, onDone }) {
    const [choice, setChoice] = useState(null)
    const [password, setPassword] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    const choose = (next) => {
        setChoice(next)
        setPassword('')
        setError('')
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!choice || busy) return
        if (!password) {
            setError('Enter your password to continue')
            return
        }
        setError('')
        setBusy(true)
        try {
            await options[choice].action(password)
            onDone()
        } catch (err) {
            setError(err.message)
            setBusy(false)
        }
    }

    return (
        <div className={styles.page}>
            <div className={styles.subHeader}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onBack}>
                    <BackIcon />
                </button>
                <span className={styles.subTitle}>Disable or delete account</span>
            </div>

            {Object.entries(options).map(([key, option]) => (
                <button
                    key={key}
                    type="button"
                    className={choice === key ? `${styles.option} ${styles.optionActive}` : styles.option}
                    onClick={() => choose(key)}
                >
                    <span className={key === 'delete' ? `${styles.optionTitle} ${styles.dangerText}` : styles.optionTitle}>{option.title}</span>
                    <span className={styles.optionText}>{option.text}</span>
                </button>
            ))}

            {choice && (
                <form className={styles.form} onSubmit={handleSubmit}>
                    <Input type="password" label="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
                    {error && <p className={styles.error}>{error}</p>}
                    <button className={styles.dangerButton} type="submit" disabled={busy}>
                        {busy ? <Spinner size={20} /> : options[choice].confirm}
                    </button>
                    <button className={styles.cancel} type="button" onClick={() => choose(null)}>Cancel</button>
                </form>
            )}
        </div>
    )
}

export default AccountSettings
