import { useLayoutEffect, useRef, useState } from 'react'
import styles from './MessageBubble.module.css'
import TickIcon from '../Icons/TickIcon'
import DoubleTickIcon from '../Icons/DoubleTickIcon'
import ClockIcon from '../Icons/ClockIcon'
import FailedIcon from '../Icons/FailedIcon'
import ReplyIcon from '../Icons/ReplyIcon'
import EditIcon from '../Icons/EditIcon'
import CopyIcon from '../Icons/CopyIcon'
import TranslateIcon from '../Icons/TranslateIcon'
import LinkIcon from '../Icons/LinkIcon'
import PinIcon from '../Icons/PinIcon'
import ForwardIcon from '../Icons/ForwardIcon'
import SelectIcon from '../Icons/SelectIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import ResendIcon from '../Icons/ResendIcon'
import CancelIcon from '../Icons/CancelIcon'
import ContextMenu from '../ContextMenu/ContextMenu'

const statusIcons = {
    sending: ClockIcon,
    sent: TickIcon,
    read: DoubleTickIcon,
    failed: FailedIcon,
}

function StatusIcon({ status }) {
    const Icon = statusIcons[status] || TickIcon
    return (
        <span className={status === 'failed' ? `${styles.tick} ${styles.failed}` : status === 'sending' ? `${styles.tick} ${styles.sending}` : styles.tick}>
            <Icon />
        </span>
    )
}

function MessageBubble({ message, focused, groupStart, groupEnd, onResend, onDelete }) {
    const [menu, setMenu] = useState(null)
    const bubbleRef = useRef(null)

    useLayoutEffect(() => {
        if (message.leaving) bubbleRef.current.style.setProperty('--height', `${bubbleRef.current.offsetHeight}px`)
    }, [message.leaving])

    const classes = [
        message.mine ? styles.bubbleMine : styles.bubble,
        groupStart && styles.groupStart,
        groupEnd && styles.withTail,
        message.leaving && styles.leaving,
        focused && styles.focused,
    ].filter(Boolean).join(' ')

    const deleteItem = { label: 'Delete', Icon: DeleteIcon, danger: true, onClick: onDelete }

    const menuItems = {
        failed: [
            { label: 'Resend', Icon: ResendIcon, onClick: onResend },
            deleteItem,
        ],
        sending: [
            { label: 'Cancel Sending', Icon: CancelIcon, onClick: onDelete },
            deleteItem,
        ],
        sent: [
            { label: 'Reply', Icon: ReplyIcon },
            { label: 'Edit', Icon: EditIcon },
            { label: 'Copy', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(message.text) },
            { label: 'Translate', Icon: TranslateIcon },
            { label: 'Copy Message Link', Icon: LinkIcon },
            { label: 'Pin', Icon: PinIcon },
            { label: 'Forward', Icon: ForwardIcon },
            { label: 'Select', Icon: SelectIcon },
            deleteItem,
        ],
    }

    const receivedItems = menuItems.sent.filter((item) => item.label !== 'Edit')

    const items = message.mine ? menuItems[message.status] || menuItems.sent : receivedItems

    const openMenu = (e) => {
        e.preventDefault()
        setMenu({ x: e.clientX, y: e.clientY })
    }

    return (
        <div className={classes} ref={bubbleRef} data-message-id={message.id} onContextMenu={openMenu}>
            <span className={styles.text}>{message.text}</span>
            <span className={styles.meta}>
                <span className={styles.time}>{message.time}</span>
                {message.mine && <StatusIcon status={message.status} />}
            </span>
            {menu && (
                <ContextMenu
                    x={menu.x}
                    y={menu.y}
                    title={message.status === 'failed' ? 'Message not sent' : null}
                    items={items}
                    onClose={() => setMenu(null)}
                />
            )}
        </div>
    )
}

export default MessageBubble
