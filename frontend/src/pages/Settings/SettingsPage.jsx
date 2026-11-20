import { useEffect, useState } from 'react'
import styles from './SettingsPage.module.css'
import Avatar from '../../components/Avatar/Avatar'
import ChevronIcon from '../../components/Icons/ChevronIcon'
import AtIcon from '../../components/Icons/AtIcon'
import HashIcon from '../../components/Icons/HashIcon'
import InfoIcon from '../../components/Icons/InfoIcon'
import EditIcon from '../../components/Icons/EditIcon'
import LockIcon from '../../components/Icons/LockIcon'
import DisabledUserIcon from '../../components/Icons/DisabledUserIcon'
import LogoutIcon from '../../components/Icons/LogoutIcon'
import { getThemeChoice, setThemeChoice } from '../../utils/theme'
import { revealChange } from '../../utils/motion'
import { getPrivacy, setPrivacy } from '../../api/users'
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
    const [showLastSeen, setShowLastSeen] = useState(null)

    useEffect(() => {
        let active = true
        getPrivacy()
            .then((privacy) => active && setShowLastSeen(privacy.showLastSeen))
            .catch(() => active && setShowLastSeen(true))
        return () => {
            active = false
        }
    }, [])

    const changeLastSeen = async (next) => {
        const before = showLastSeen
        setShowLastSeen(next)
        try {
            const saved = await setPrivacy(next)
            setShowLastSeen(saved.showLastSeen)
        } catch {
            setShowLastSeen(before)
        }
    }

    const chooseTheme = (choice, e) => {
        if (choice === theme) return
        revealChange(() => {
            setThemeChoice(choice)
            setTheme(choice)
        }, e.clientX, e.clientY)
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
            <div className={styles.hero}>
                <span className={styles.cover} />
                <button className={styles.heroEdit} type="button" onClick={() => setView('profile')}>
                    <EditIcon />
                    Edit
                </button>
                <Avatar name={user.displayName || user.username} size={92} />
                <span className={styles.heroName}>{user.displayName}</span>
                <span className={styles.heroStatus}>online</span>
            </div>

            <div className={styles.group}>
                <button className={styles.row} type="button" onClick={() => copy('username', `@${user.username}`)}>
                    <span className={`${styles.icon} ${styles.iconBlue}`}><AtIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>{copied === 'username' ? 'Copied' : `@${user.username}`}</span>
                        <span className={styles.rowHint}>Username</span>
                    </span>
                </button>
                {user.bio && (
                    <button className={styles.row} type="button" onClick={() => copy('bio', user.bio)}>
                        <span className={`${styles.icon} ${styles.iconOrange}`}><InfoIcon /></span>
                        <span className={styles.rowText}>
                            <span className={`${styles.rowLabel} ${styles.rowWrap}`}>{copied === 'bio' ? 'Copied' : user.bio}</span>
                            <span className={styles.rowHint}>Bio</span>
                        </span>
                    </button>
                )}
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
                <div className={styles.themes} style={{ '--slot': themes.findIndex((item) => item.id === theme) }}>
                    <span className={styles.themeSlider} />
                    {themes.map((item) => (
                        <button
                            key={item.id}
                            className={item.id === theme ? `${styles.themeOption} ${styles.themeActive}` : styles.themeOption}
                            type="button"
                            onClick={(e) => chooseTheme(item.id, e)}
                        >
                            {item.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className={styles.group}>
                <span className={styles.groupTitle}>Privacy</span>
                <label className={styles.switchRow}>
                    Show my last seen and online
                    <input
                        className={styles.switch}
                        type="checkbox"
                        checked={showLastSeen ?? true}
                        disabled={showLastSeen === null}
                        onChange={(e) => changeLastSeen(e.target.checked)}
                    />
                </label>
            </div>
            <div className={styles.group}>
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
