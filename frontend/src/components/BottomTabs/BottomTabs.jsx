import styles from './BottomTabs.module.css'
import logo from '../../assets/logo.png'
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

function BottomTabs({ user, activeTab, onChange }) {
    return (
        <nav className={styles.bar} style={{ '--slot': tabs.findIndex((tab) => tab.id === activeTab) }}>
            <img className={styles.logo} src={logo} alt="Civenth" />
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
