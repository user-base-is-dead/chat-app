import { useState } from 'react'
import styles from './SettingsPage.module.css'
import Avatar from '../../components/Avatar/Avatar'
import BackIcon from '../../components/Icons/BackIcon'
import Input from '../../components/Input/Input'
import Spinner from '../../components/Spinner/Spinner'
import { updateProfile } from '../../api/users'

const BIO_LIMIT = 140

function ProfileSettings({ user, onBack, onSaved }) {
    const [displayName, setDisplayName] = useState(user.displayName)
    const [username, setUsername] = useState(user.username)
    const [bio, setBio] = useState(user.bio ?? '')
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [saved, setSaved] = useState(false)

    const [copied, setCopied] = useState(false)

    const copyId = async () => {
        try {
            await navigator.clipboard.writeText(user.id)
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        } catch {
            setCopied(false)
        }
    }

    const changed = displayName.trim() !== user.displayName || username.trim() !== user.username || bio.trim() !== (user.bio ?? '')

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (!changed || saving) return
        if (!displayName.trim()) {
            setError('Name cannot be empty')
            return
        }
        if (!/^[A-Za-z0-9_]{3,32}$/.test(username.trim())) {
            setError('Username must be 3-32 characters: letters, numbers or _')
            return
        }
        setError('')
        setSaving(true)
        try {
            const updated = await updateProfile(displayName.trim(), username.trim(), bio.trim())
            onSaved(updated)
            setDisplayName(updated.displayName)
            setUsername(updated.username)
            setBio(updated.bio)
            setSaved(true)
        } catch (err) {
            setError(err.message)
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className={styles.page}>
            <div className={styles.subHeader}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onBack}>
                    <BackIcon />
                </button>
                <span className={styles.subTitle}>Profile</span>
            </div>

            <div className={styles.profileHero}>
                <Avatar name={displayName.trim() || user.username} size={88} />
                <span className={styles.profileHint}>@{user.username}</span>
                <button className={styles.idChip} type="button" onClick={copyId}>
                    {copied ? 'Copied' : `ID: ${user.id}`}
                </button>
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
                <Input label="Name" value={displayName} maxLength={64} onChange={(e) => { setDisplayName(e.target.value); setSaved(false) }} />
                <Input label="Username" value={username} maxLength={32} onChange={(e) => { setUsername(e.target.value); setSaved(false) }} />
                <Input label="Bio" value={bio} maxLength={BIO_LIMIT} onChange={(e) => { setBio(e.target.value); setSaved(false) }} />
                <span className={styles.counter}>{[...bio].length}/{BIO_LIMIT}</span>
                {error && <p className={styles.error}>{error}</p>}
                {saved && !changed && <p className={styles.success}>Profile saved</p>}
                <button className={styles.primary} type="submit" disabled={!changed || saving}>
                    {saving ? <Spinner size={20} /> : 'Save'}
                </button>
            </form>
        </div>
    )
}

export default ProfileSettings
