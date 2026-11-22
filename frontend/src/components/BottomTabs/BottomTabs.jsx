import { useId, useRef } from 'react'
import CloseIcon from '../Icons/CloseIcon'
import styles from './BottomTabs.module.css'
import MoreIcon from '../Icons/MoreIcon'
import Avatar from '../Avatar/Avatar'
import ChatsIcon from '../Icons/ChatsIcon'
import GroupsIcon from '../Icons/GroupsIcon'
import ChannelsIcon from '../Icons/ChannelsIcon'
import SettingsIcon from '../Icons/SettingsIcon'

const tabs = [
    { id: 'chats', label: 'Chats', Icon: ChatsIcon },
    { id: 'groups', label: 'Groups', Icon: GroupsIcon },
    { id: 'channels', label: 'Channels', Icon: ChannelsIcon },
    { id: 'settings', label: 'Settings', Icon: SettingsIcon },
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
                <h1 id={titleId}>Coming soon</h1>
                <p>New options will show up here.</p>
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
