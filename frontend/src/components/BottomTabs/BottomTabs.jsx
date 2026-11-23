import { useId, useRef } from 'react'
import CloseIcon from '../Icons/CloseIcon'
import styles from './BottomTabs.module.css'
import MoreIcon from '../Icons/MoreIcon'
import Avatar from '../Avatar/Avatar'
import ChatsIcon from '../Icons/ChatsIcon'
import GroupsIcon from '../Icons/GroupsIcon'
import ChannelsIcon from '../Icons/ChannelsIcon'
import SettingsIcon from '../Icons/SettingsIcon'
import ChevronIcon from '../Icons/ChevronIcon'
import logo from '../../assets/logo.png'

const tabs = [
    { id: 'chats', label: 'Chats', Icon: ChatsIcon },
    { id: 'groups', label: 'Groups', Icon: GroupsIcon },
    { id: 'channels', label: 'Channels', Icon: ChannelsIcon },
    { id: 'settings', label: 'Settings', Icon: SettingsIcon },
]

const menuOptions = [
    { name: 'Chat', ready: true },
    { name: 'Crypto Cold Wallet', ready: false },
    { name: 'Buy & Sell', ready: false },
]

export function MoreMenu({ className = styles.more }) {
    const titleId = useId()
    const menu = useRef(null)
    const openMenu = (event) => {
        const bounds = event.currentTarget.getBoundingClientRect()
        const dialog = menu.current
        dialog.style.setProperty('--trigger-x', bounds.left + 'px')
        dialog.style.setProperty('--trigger-y', bounds.top + 'px')
        dialog.style.setProperty('--trigger-width', bounds.width + 'px')
        dialog.style.setProperty('--trigger-height', bounds.height + 'px')
        dialog.showModal()
    }
    return (<>
            <button type="button" className={className} aria-label="More" aria-haspopup="dialog" onClick={openMenu}>
                <MoreIcon />
            </button>
            <dialog ref={menu} className={styles.morePanel} aria-labelledby={titleId}>
                <button type="button" className={styles.closeMenu} aria-label="Close" onClick={() => menu.current.close()}><CloseIcon /></button>
                <h1 id={titleId} className={styles.brand}><img src={logo} alt="" />Civenth</h1>
                <div className={styles.menuOptions}>
                    {menuOptions.map(({ name, ready }, index) => (
                        <button
                            key={name}
                            type="button"
                            className={[styles.menuOption, !ready && styles.optionSoon].filter(Boolean).join(' ')}
                            aria-current={ready ? 'page' : undefined}
                            aria-disabled={ready ? undefined : 'true'}
                            onClick={ready ? () => menu.current.close() : undefined}
                        >
                            <span className={styles.optionNumber}>{String(index + 1).padStart(2, '0')}</span>
                            <span className={styles.optionName}>{name}</span>
                            {ready ? <span className={styles.optionEnd}><ChevronIcon /></span> : <span className={styles.soonTag}>Soon</span>}
                        </button>
                    ))}
                </div>
            </dialog>
    </>)
}

function BottomTabs({ user, activeTab, onChange }) {
    return (
        <nav className={styles.bar} style={{ '--slot': tabs.findIndex((tab) => tab.id === activeTab) }}>
            <MoreMenu />
            <span className={styles.slider} />
            {tabs.map(({ id, label, Icon }) => (
                <button
                    key={id}
                    className={[styles.tab, id === activeTab && styles.tabActive, id === 'settings' && styles.settingsTab].filter(Boolean).join(' ')}
                    type="button"
                    aria-current={id === activeTab ? 'page' : undefined}
                    onClick={() => onChange(id)}
                >
                    <span className={styles.circle}><Icon /></span>
                    <span className={styles.label}>{label}</span>
                </button>
            ))}
            <button
                className={styles.me}
                type="button"
                aria-label="Settings"
                onClick={() => onChange('settings')}
            >
                <Avatar name={user.displayName || user.username} size={42} />
            </button>
        </nav>
    )
}

export default BottomTabs
