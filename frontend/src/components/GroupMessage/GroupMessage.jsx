import { useState } from 'react'
import styles from './GroupMessage.module.css'
import Avatar from '../Avatar/Avatar'
import ContextMenu from '../ContextMenu/ContextMenu'
import TickIcon from '../Icons/TickIcon'
import DoubleTickIcon from '../Icons/DoubleTickIcon'
import ClockIcon from '../Icons/ClockIcon'
import FailedIcon from '../Icons/FailedIcon'
import { messageMenuItems } from '../MessageBubble/messageMenu'
import { roleColor } from '../../utils/roles'
import { dateTime } from '../../utils/dayLabel'

const statusIcons = { sending: ClockIcon, sent: TickIcon, read: DoubleTickIcon, failed: FailedIcon }

function Tick({ status }) {
    const Icon = statusIcons[status] || TickIcon
    return (
        <span className={status === 'failed' ? `${styles.tick} ${styles.failed}` : styles.tick}>
            <Icon />
        </span>
    )
}

function GroupMessage({ message, start, focused, selfName, peerName, peerPhoto, nameColor, canDeleteOthers, selecting, selected, onSelect, onResend, onDelete }) {
    const [menu, setMenu] = useState(null)
    const name = message.mine ? selfName : message.senderName || peerName || 'Deleted Account'
    const pending = message.mine && (message.status === 'sending' || message.status === 'failed')
    const tick = pending ? <Tick status={message.status} /> : null
    const classes = [styles.row, start && styles.start, focused && styles.focused, selecting && styles.selecting, selected && styles.selected].filter(Boolean).join(' ')

    const openMenu = (e) => {
        e.preventDefault()
        if (selecting) return
        setMenu({ x: e.clientX, y: e.clientY })
    }

    return (
        <div className={classes} data-message-id={message.id} onContextMenu={openMenu}>
            <span className={styles.check} aria-hidden="true" />
            <span className={styles.side}>
                {start ? (
                    <Avatar name={name} photo={message.mine ? undefined : peerPhoto} size={40} />
                ) : (
                    <span className={styles.sideTime}>{message.time}</span>
                )}
            </span>
            <span className={styles.body}>
                {start && (
                    <span className={styles.head}>
                        <span className={styles.name} style={nameColor ? { color: roleColor(nameColor) } : undefined}>{name}</span>
                        <span className={styles.time}>{message.createdAt ? dateTime(message.createdAt) : message.time}</span>
                        {tick}
                    </span>
                )}
                <span className={styles.text}>
                    {message.text}
                    {!start && tick && <span className={styles.inlineTick}>{tick}</span>}
                </span>
            </span>
            {menu && (
                <ContextMenu
                    x={menu.x}
                    y={menu.y}
                    title={message.status === 'failed' ? 'Message not sent' : null}
                    items={messageMenuItems({ message, canDeleteOthers, onResend, onDelete, onSelect })}
                    onClose={() => setMenu(null)}
                />
            )}
        </div>
    )
}

export default GroupMessage
