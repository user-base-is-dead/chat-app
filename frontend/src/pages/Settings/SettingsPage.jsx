import { useState } from 'react'
import styles from './SettingsPage.module.css'
import Avatar from '../../components/Avatar/Avatar'
import ChevronIcon from '../../components/Icons/ChevronIcon'
import AtIcon from '../../components/Icons/AtIcon'
import HashIcon from '../../components/Icons/HashIcon'
import EditIcon from '../../components/Icons/EditIcon'
import LockIcon from '../../components/Icons/LockIcon'
import DisabledUserIcon from '../../components/Icons/DisabledUserIcon'
import LogoutIcon from '../../components/Icons/LogoutIcon'
import { getThemeChoice, setThemeChoice } from '../../utils/theme'
import ProfileSettings from './ProfileSettings'
import AccountSettings from './AccountSettings'
import PasswordSettings from './PasswordSettings'

const themes = [
    { id: 'system', label: 'System' },
    { id: 'dark', label: 'Dark' },
    { id: 'light', label: 'Light' },
]

function SettingsPage({ user, onLogout, onUserChange, onAccountGone }) {
    const [view, setView] = useState('main')
    const [copied, setCopied] = useState(null)
    const [theme, setTheme] = useState(getThemeChoice)

    const chooseTheme = (choice) => {
        setThemeChoice(choice)
        setTheme(choice)
    }

    const copy = async (key, value) => {
        try {
            await navigator.clipboard.writeText(value)
            setCopied(key)
            setTimeout(() => setCopied(null), 1500)
        } catch {
            setCopied(null)
        }
    }

    if (view === 'profile') {
        return <ProfileSettings user={user} onBack={() => setView('main')} onSaved={onUserChange} />
    }

    if (view === 'password') {
        return <PasswordSettings onBack={() => setView('main')} />
    }

    if (view === 'account') {
        return <AccountSettings onBack={() => setView('main')} onDone={onAccountGone} />
    }

    return (
        <div className={styles.page}>
            <button className={styles.hero} type="button" onClick={() => setView('profile')}>
                <Avatar name={user.displayName || user.username} size={100} />
                <span className={styles.heroName}>{user.displayName}</span>
                <span className={styles.heroStatus}>online</span>
            </button>

            <div className={styles.group}>
                <button className={styles.row} type="button" onClick={() => copy('username', `@${user.username}`)}>
                    <span className={`${styles.icon} ${styles.iconBlue}`}><AtIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>{copied === 'username' ? 'Copied' : `@${user.username}`}</span>
                        <span className={styles.rowHint}>Username</span>
                    </span>
                </button>
                <button className={styles.row} type="button" onClick={() => copy('id', user.id)}>
                    <span className={`${styles.icon} ${styles.iconPurple}`}><HashIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>{copied === 'id' ? 'Copied' : user.id}</span>
                        <span className={styles.rowHint}>ID</span>
                    </span>
                </button>
            </div>

            <div className={styles.group}>
                <span className={styles.groupTitle}>Appearance</span>
                <div className={styles.themes}>
                    {themes.map((item) => (
                        <button
                            key={item.id}
                            className={item.id === theme ? `${styles.themeOption} ${styles.themeActive}` : styles.themeOption}
                            type="button"
                            onClick={() => chooseTheme(item.id)}
                        >
                            {item.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className={styles.group}>
                <button className={styles.row} type="button" onClick={() => setView('profile')}>
                    <span className={`${styles.icon} ${styles.iconPurple}`}><EditIcon /></span>
                    <span className={styles.rowLabel}>Edit Profile</span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
                <button className={styles.row} type="button" onClick={() => setView('password')}>
                    <span className={`${styles.icon} ${styles.iconGrey}`}><LockIcon /></span>
                    <span className={styles.rowLabel}>Change Password</span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
                <button className={styles.row} type="button" onClick={() => setView('account')}>
                    <span className={`${styles.icon} ${styles.iconOrange}`}><DisabledUserIcon /></span>
                    <span className={styles.rowLabel}>Disable or Delete Account</span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
            </div>

            <div className={styles.group}>
                <button className={`${styles.row} ${styles.logout}`} type="button" onClick={onLogout}>
                    <span className={`${styles.icon} ${styles.iconRed}`}><LogoutIcon /></span>
                    <span className={styles.rowLabel}>Log Out</span>
                </button>
            </div>
        </div>
    )
}

export default SettingsPage
