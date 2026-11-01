import styles from './BottomTabs.module.css'
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

function BottomTabs({ activeTab, onChange }) {
    return (
        <nav className={styles.bar}>
            {tabs.map(({ id, label, Icon }) => (
                <button
                    key={id}
                    className={id === activeTab ? `${styles.tab} ${styles.tabActive}` : styles.tab}
                    type="button"
                    aria-current={id === activeTab ? 'page' : undefined}
                    onClick={() => onChange(id)}
                >
                    <span className={styles.circle}><Icon /></span>
                    <span className={styles.label}>{label}</span>
                </button>
            ))}
        </nav>
    )
}

export default BottomTabs
