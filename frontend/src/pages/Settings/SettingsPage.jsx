import { useEffect, useState } from 'react'
import styles from './SettingsPage.module.css'
import Avatar from '../../components/Avatar/Avatar'
import ChevronIcon from '../../components/Icons/ChevronIcon'
import AtIcon from '../../components/Icons/AtIcon'
import HashIcon from '../../components/Icons/HashIcon'
import InfoIcon from '../../components/Icons/InfoIcon'
import EditIcon from '../../components/Icons/EditIcon'
import LockIcon from '../../components/Icons/LockIcon'
import EyeIcon from '../../components/Icons/EyeIcon'
import BackIcon from '../../components/Icons/BackIcon'
import DisabledUserIcon from '../../components/Icons/DisabledUserIcon'
import LogoutIcon from '../../components/Icons/LogoutIcon'
import LogoutDialog from '../../components/LogoutDialog/LogoutDialog'
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
    const [confirmLogout, setConfirmLogout] = useState(false)

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
        return <PasswordSettings onBack={() => setView('security')} />
    }

    if (view === 'account') {
        return <AccountSettings onBack={() => setView('security')} onDone={onAccountGone} />
    }

    const subPage = (title, content) => (
        <div className={styles.page}>
            <div className={styles.subHeader}>
                <button className={styles.back} type="button" aria-label="Back" onClick={() => setView('main')}>
                    <BackIcon />
                </button>
                <span className={styles.subTitle}>{title}</span>
            </div>
            {content}
        </div>
    )

    if (view === 'privacy') {
        return subPage('Privacy', (
            <div className={styles.group}>
                <span className={styles.groupTitle}>Last seen and online</span>
                <label className={styles.switchRow}>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Show my last seen and online</span>
                        <span className={styles.rowHint}>When on, people can see when you are online and when you were last active</span>
                    </span>
                    <input
                        className={styles.switch}
                        type="checkbox"
                        checked={showLastSeen ?? true}
                        disabled={showLastSeen === null}
                        onChange={(e) => changeLastSeen(e.target.checked)}
                    />
                </label>
            </div>
        ))
    }

    if (view === 'security') {
        return subPage('Security', (
            <div className={styles.group}>
                <button className={styles.row} type="button" onClick={() => setView('password')}>
                    <span className={`${styles.icon} ${styles.iconGrey}`}><LockIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Change Password</span>
                        <span className={styles.rowHint}>Set a new password for signing in</span>
                    </span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
                <button className={styles.row} type="button" onClick={() => setView('account')}>
                    <span className={`${styles.icon} ${styles.iconOrange}`}><DisabledUserIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Disable or Delete Account</span>
                        <span className={styles.rowHint}>Pause your account for a while or remove it</span>
                    </span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
            </div>
        ))
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
                        <span className={styles.rowHint}>Your username. Click to copy</span>
                    </span>
                </button>
                {user.bio && (
                    <button className={styles.row} type="button" onClick={() => copy('bio', user.bio)}>
                        <span className={`${styles.icon} ${styles.iconOrange}`}><InfoIcon /></span>
                        <span className={styles.rowText}>
                            <span className={`${styles.rowLabel} ${styles.rowWrap}`}>{copied === 'bio' ? 'Copied' : user.bio}</span>
                            <span className={styles.rowHint}>Your bio. Click to copy</span>
                        </span>
                    </button>
                )}
                <button className={styles.row} type="button" onClick={() => copy('id', user.id)}>
                    <span className={`${styles.icon} ${styles.iconPurple}`}><HashIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>{copied === 'id' ? 'Copied' : user.id}</span>
                        <span className={styles.rowHint}>Your account ID. Click to copy</span>
                    </span>
                </button>
            </div>

            <div className={styles.group}>
                <span className={styles.groupTitle}>Appearance</span>
                <span className={styles.groupHint}>Pick light or dark, or follow your device</span>
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
                <button className={styles.row} type="button" onClick={() => setView('privacy')}>
                    <span className={`${styles.icon} ${styles.iconBlue}`}><EyeIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Privacy</span>
                    </span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
                <button className={styles.row} type="button" onClick={() => setView('security')}>
                    <span className={`${styles.icon} ${styles.iconGrey}`}><LockIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Security</span>
                    </span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
            </div>

            <div className={styles.group}>
                <button className={`${styles.row} ${styles.logout}`} type="button" onClick={() => setConfirmLogout(true)}>
                    <span className={`${styles.icon} ${styles.iconRed}`}><LogoutIcon /></span>
                    <span className={styles.rowText}>
                        <span className={styles.rowLabel}>Log Out</span>
                        <span className={styles.rowHint}>Sign out of Civenth on this device</span>
                    </span>
                </button>
            </div>
            {confirmLogout && <LogoutDialog user={user} onConfirm={onLogout} onClose={() => setConfirmLogout(false)} />}
        </div>
    )
}

export default SettingsPage
