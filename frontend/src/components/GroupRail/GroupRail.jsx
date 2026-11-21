import { useState } from 'react'
import styles from './GroupRail.module.css'
import Avatar from '../Avatar/Avatar'
import ContextMenu from '../ContextMenu/ContextMenu'
import PlusIcon from '../Icons/PlusIcon'
import PinIcon from '../Icons/PinIcon'
import MuteIcon from '../Icons/MuteIcon'
import UnmuteIcon from '../Icons/UnmuteIcon'
import LogoutIcon from '../Icons/LogoutIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import { formatCount } from '../../utils/formatCount'

const PIN_LIMIT = 5

function GroupRail({ groups, selectedId, pinnedCount, onSelect, onNew, onPin, onMute, onLeave, onDelete }) {
    const [menu, setMenu] = useState(null)
    const invites = groups.filter((room) => room.status === 'request')
    const joined = groups.filter((room) => room.status === 'active')

    const openMenu = (e, room) => {
        e.preventDefault()
        setMenu({ x: e.clientX, y: e.clientY, room })
    }

    const menuItems = (room) => [
        ...(room.pinned || pinnedCount < PIN_LIMIT ? [{ label: room.pinned ? 'Unpin' : 'Pin', Icon: PinIcon, onClick: () => onPin(room) }] : []),
        { label: room.muted ? 'Unmute' : 'Mute', Icon: room.muted ? UnmuteIcon : MuteIcon, onClick: () => onMute(room) },
        room.role === 'owner'
            ? { label: 'Delete Group', Icon: DeleteIcon, danger: true, onClick: () => onDelete(room) }
            : { label: 'Leave Group', Icon: LogoutIcon, danger: true, onClick: () => onLeave(room) },
    ]

    const item = (room, invite) => (
        <button
            key={room.id}
            className={room.id === selectedId ? `${styles.item} ${styles.selected}` : styles.item}
            type="button"
            title={room.name}
            aria-label={invite ? `Invite to ${room.name}` : room.name}
            aria-current={room.id === selectedId}
            data-chat-id={room.id}
            onClick={() => onSelect(room)}
            onContextMenu={invite ? undefined : (e) => openMenu(e, room)}
        >
            <span className={styles.marker} />
            <span className={styles.photo}><Avatar name={room.name} photo={room.photo} size={48} /></span>
            {invite && <span className={styles.inviteDot} />}
            {!invite && room.unreadCount > 0 && <span className={styles.unreadMark} />}
            {!invite && room.unreadCount > 0 && (
                room.muted ? <span className={styles.mutedDot} /> : <span className={styles.badge}>{formatCount(room.unreadCount)}</span>
            )}
        </button>
    )

    return (
        <div className={styles.rail}>
            {invites.map((room) => item(room, true))}
            {invites.length > 0 && joined.length > 0 && <span className={styles.divider} />}
            {joined.map((room) => item(room, false))}
            <span className={styles.divider} />
            <button className={styles.add} type="button" aria-label="New Group" title="New Group" onClick={onNew}>
                <PlusIcon />
            </button>
            {menu && <ContextMenu x={menu.x} y={menu.y} items={menuItems(menu.room)} onClose={() => setMenu(null)} />}
        </div>
    )
}

export default GroupRail
