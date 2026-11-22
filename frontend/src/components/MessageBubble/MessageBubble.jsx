import { useId, useLayoutEffect, useRef, useState } from 'react'
import styles from './MessageBubble.module.css'
import TickIcon from '../Icons/TickIcon'
import DoubleTickIcon from '../Icons/DoubleTickIcon'
import ClockIcon from '../Icons/ClockIcon'
import FailedIcon from '../Icons/FailedIcon'
import ContextMenu from '../ContextMenu/ContextMenu'
import Avatar from '../Avatar/Avatar'
import PostFooter from '../PostFooter/PostFooter'
import EmojiIcon from '../Icons/EmojiIcon'
import CommentIcon from '../Icons/CommentIcon'
import EditIcon from '../Icons/EditIcon'
import { REACTIONS } from '../../utils/reactions'
import { messageMenuItems } from './messageMenu'
import { dateTime } from '../../utils/dayLabel'

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

function MessageBubble({ message, focused, groupStart, groupEnd, showSender, firstOfSender, hideTicks, canDeleteOthers = true, channel, stats, canManagePosts, selecting, selected, onSelect, onReact, onOpenComments, onToggleSwitch, onEditPost, onResend, onDelete }) {
    const [menu, setMenu] = useState(null)
    const tailClip = useId()
    const bubbleRef = useRef(null)

    useLayoutEffect(() => {
        if (message.leaving) bubbleRef.current.style.setProperty('--height', `${bubbleRef.current.offsetHeight}px`)
    }, [message.leaving])

    const post = Boolean(channel) && message.status !== 'sending' && message.status !== 'failed'

    const classes = [
        message.mine && !channel ? styles.bubbleMine : styles.bubble,
        post && styles.post,
        groupStart && styles.groupStart,
        groupEnd && styles.groupEnd,
        message.leaving && styles.leaving,
        focused && styles.focused,
        showSender && styles.indented,
        selected && styles.selected,
    ].filter(Boolean).join(' ')

    const senderName = channel ? channel.name : message.senderName || 'Deleted Account'

    const manage = post && canManagePosts && stats
    const manageItems = manage ? [
        { label: 'Edit post', Icon: EditIcon, onClick: () => onEditPost() },
        { label: stats.allowComments ? 'Turn off comments' : 'Turn on comments', Icon: CommentIcon, onClick: () => onToggleSwitch('allowComments') },
        { label: stats.allowReactions ? 'Turn off reactions' : 'Turn on reactions', Icon: EmojiIcon, onClick: () => onToggleSwitch('allowReactions') },
    ] : []

    const items = menu?.manage ? manageItems : messageMenuItems({ message, canDeleteOthers, onResend, onDelete, onSelect })

    const openManage = (e) => {
        if (selecting) return
        const box = e.currentTarget.getBoundingClientRect()
        setMenu({ x: box.right - 8, y: box.bottom + 4, manage: true })
    }

    const reactionRow = post && stats?.allowReactions ? (close) => (
        <div className={styles.reactions}>
            {REACTIONS.map((emoji) => (
                <button
                    key={emoji}
                    className={stats.myReaction === emoji ? `${styles.reaction} ${styles.reactionMine}` : styles.reaction}
                    type="button"
                    aria-label={`React ${emoji}`}
                    aria-pressed={stats.myReaction === emoji}
                    onClick={() => { close(); onReact(emoji) }}
                >
                    {emoji}
                </button>
            ))}
        </div>
    ) : null

    const openMenu = (e) => {
        e.preventDefault()
        if (selecting) return
        setMenu({ x: e.clientX, y: e.clientY })
    }

    return (
        <div className={classes} ref={bubbleRef} data-message-id={message.id} onContextMenu={openMenu}>
            {showSender && firstOfSender && <span className={styles.sender}>{senderName}</span>}
            {showSender && groupEnd && <span className={styles.senderAvatar}><Avatar name={senderName} photo={channel?.photo} size={32} /></span>}
            <span className={styles.text}>{message.text}</span>
            {post ? (
                <PostFooter stats={stats} time={message.createdAt ? dateTime(message.createdAt) : message.time} onReact={onReact} onOpenComments={onOpenComments} onManage={manage ? openManage : undefined} />
            ) : (
                <span className={styles.meta}>
                    <span className={styles.time}>{message.time}</span>
                    {message.mine && (!hideTicks || message.status === 'sending' || message.status === 'failed') && <StatusIcon status={message.status} />}
                </span>
            )}
            {groupEnd && (
                <svg className={styles.tail} width="12" height="16" viewBox="0 0 12 16" aria-hidden="true">
                    <clipPath id={tailClip}>
                        <rect x="-4" y="-4" width="14" height="24" />
                    </clipPath>
                    <path className={styles.tailRing} clipPath={`url(#${tailClip})`} d="M11 0C11 6 8 10.6 2.4 13.4C0.8 14.2 1.1 16 2.7 16H11" />
                    <path className={styles.tailFill} d="M11 0C11 6 8 10.6 2.4 13.4C0.8 14.2 1.1 16 2.7 16H12V0Z" />
                </svg>
            )}
            {menu && (
                <ContextMenu
                    x={menu.x}
                    y={menu.y}
                    title={message.status === 'failed' ? 'Message not sent' : null}
                    header={menu.manage ? null : reactionRow}
                    items={items}
                    onClose={() => setMenu(null)}
                />
            )}
        </div>
    )
}

export default MessageBubble
