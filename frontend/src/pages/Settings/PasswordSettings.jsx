import { useState } from 'react'
import styles from './SettingsPage.module.css'
import BackIcon from '../../components/Icons/BackIcon'
import Input from '../../components/Input/Input'
import Spinner from '../../components/Spinner/Spinner'
import { changePassword } from '../../api/users'

function PasswordSettings({ onBack }) {
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [saved, setSaved] = useState(false)

    const edit = (setter) => (e) => {
        setter(e.target.value)
        setSaved(false)
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        if (saving) return
        if (!currentPassword || !newPassword) {
            setError('Fill in all fields')
            return
        }
        if (newPassword.length < 6) {
            setError('New password must be at least 6 characters')
            return
        }
        if (newPassword !== confirmPassword) {
            setError('Passwords do not match')
            return
        }
        setError('')
        setSaving(true)
        try {
            await changePassword(currentPassword, newPassword)
            setCurrentPassword('')
            setNewPassword('')
            setConfirmPassword('')
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
                <span className={styles.subTitle}>Change password</span>
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
                <Input type="password" label="Current password" value={currentPassword} maxLength={128} onChange={edit(setCurrentPassword)} />
                <Input type="password" label="New password" value={newPassword} maxLength={128} onChange={edit(setNewPassword)} />
                <Input type="password" label="Confirm new password" value={confirmPassword} maxLength={128} onChange={edit(setConfirmPassword)} />
                {error && <p className={styles.error}>{error}</p>}
                {saved && <p className={styles.success}>Password changed. Other devices are logged out.</p>}
                <button className={styles.primary} type="submit" disabled={saving}>
                    {saving ? <Spinner size={20} /> : 'Change password'}
                </button>
            </form>
        </div>
    )
}

export default PasswordSettings
