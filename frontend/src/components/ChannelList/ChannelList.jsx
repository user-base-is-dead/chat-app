import { useState } from 'react'
import styles from './ChannelList.module.css'
import ContextMenu from '../ContextMenu/ContextMenu'
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog'
import NameDialog from '../NameDialog/NameDialog'
import Spinner from '../Spinner/Spinner'
import HashIcon from '../Icons/HashIcon'
import PlusIcon from '../Icons/PlusIcon'
import ChevronIcon from '../Icons/ChevronIcon'
import EditIcon from '../Icons/EditIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import ArrowDownIcon from '../Icons/ArrowDownIcon'
import ForwardIcon from '../Icons/ForwardIcon'
import LockIcon from '../Icons/LockIcon'
import ChannelPermissions from '../ChannelPermissions/ChannelPermissions'
import { createCategory, createChannel, deleteCategory, deleteChannel, updateCategory, updateChannel } from '../../api/channels'
import { cleanChannelName, readCollapsed, saveCollapsed } from '../../utils/channelName'
import { formatCount } from '../../utils/formatCount'

function ArrowUpIcon() {
    return <span className={styles.flip}><ArrowDownIcon /></span>
}

function ChannelList({ room, list, openId, canManage, onOpen, onOpenInfo, onChanged, onError }) {
    const [collapsed, setCollapsed] = useState(() => readCollapsed(room.id))
    const [menu, setMenu] = useState(null)
    const [dialog, setDialog] = useState(null)
    const [confirm, setConfirm] = useState(null)
    const [permissionsFor, setPermissionsFor] = useState(null)

    if (!list) return <div className={styles.loading}><Spinner size={22} /></div>

    const run = async (action) => {
        try {
            await action()
            onChanged()
        } catch (error) {
            onError(error)
        }
    }

    const save = async (action) => {
        await action()
        onChanged()
    }

    const toggle = (categoryId) => {
        const next = collapsed.includes(categoryId) ? collapsed.filter((id) => id !== categoryId) : [...collapsed, categoryId]
        setCollapsed(next)
        saveCollapsed(room.id, next)
    }

    const newChannel = (categoryId) => setDialog({
        title: 'New channel',
        label: 'Channel name',
        clean: cleanChannelName,
        submit: (name) => save(() => createChannel(room.id, name, categoryId)),
    })

    const newCategory = () => setDialog({
        title: 'New category',
        label: 'Category name',
        submit: (name) => save(() => createCategory(room.id, name)),
    })

    const openAt = (e, build) => {
        e.preventDefault()
        e.stopPropagation()
        const rect = e.currentTarget.getBoundingClientRect()
        const fromButton = e.type === 'click'
        const at = { x: fromButton ? rect.left : e.clientX, y: fromButton ? rect.bottom + 4 : e.clientY }
        setMenu({ ...at, items: build(at) })
    }

    const channelItems = (channel, at) => [
        ...(channel.permissions?.includes('manageChannel') ? manageItems(channel, at) : []),
        ...(channel.permissions?.includes('managePermissions')
            ? [{ label: 'Permissions', Icon: LockIcon, onClick: () => setPermissionsFor({ kind: 'channel', id: channel.id, name: channel.name }) }]
            : []),
    ]

    const manageItems = (channel, at) => [
        {
            label: 'Rename',
            Icon: EditIcon,
            onClick: () => setDialog({
                title: 'Rename channel',
                label: 'Channel name',
                clean: cleanChannelName,
                initial: channel.name,
                submit: (name) => save(() => updateChannel(room.id, channel.id, { name })),
            }),
        },
        { label: 'Move up', Icon: ArrowUpIcon, onClick: () => run(() => updateChannel(room.id, channel.id, { move: 'up' })) },
        { label: 'Move down', Icon: ArrowDownIcon, onClick: () => run(() => updateChannel(room.id, channel.id, { move: 'down' })) },
        ...(list.categories.length > 0 ? [{
            label: 'Move to category',
            Icon: ForwardIcon,
            onClick: () => setMenu({
                ...at,
                items: [
                    { label: 'No category', Icon: HashIcon, onClick: () => run(() => updateChannel(room.id, channel.id, { categoryId: '' })) },
                    ...list.categories.map((category) => ({
                        label: category.name,
                        Icon: ChevronIcon,
                        onClick: () => run(() => updateChannel(room.id, channel.id, { categoryId: category.id })),
                    })),
                ],
            }),
        }] : []),
        ...(channel.id === room.id ? [] : [{
            label: 'Delete',
            Icon: DeleteIcon,
            danger: true,
            onClick: () => setConfirm({
                title: 'Delete channel?',
                text: `All messages in #${channel.name} will be deleted for everyone.`,
                action: () => run(() => deleteChannel(room.id, channel.id)),
            }),
        }]),
    ]

    const categoryItems = (category) => [
        { label: 'New channel', Icon: PlusIcon, onClick: () => newChannel(category.id) },
        ...(category.permissions?.includes('managePermissions')
            ? [{ label: 'Permissions', Icon: LockIcon, onClick: () => setPermissionsFor({ kind: 'category', id: category.id, name: category.name }) }]
            : []),
        {
            label: 'Rename',
            Icon: EditIcon,
            onClick: () => setDialog({
                title: 'Rename category',
                label: 'Category name',
                initial: category.name,
                submit: (name) => save(() => updateCategory(room.id, category.id, { name })),
            }),
        },
        { label: 'Move up', Icon: ArrowUpIcon, onClick: () => run(() => updateCategory(room.id, category.id, { move: 'up' })) },
        { label: 'Move down', Icon: ArrowDownIcon, onClick: () => run(() => updateCategory(room.id, category.id, { move: 'down' })) },
        {
            label: 'Delete',
            Icon: DeleteIcon,
            danger: true,
            onClick: () => setConfirm({
                title: 'Delete category?',
                text: `${category.name} will be removed. Its channels stay in the group.`,
                action: () => run(() => deleteCategory(room.id, category.id)),
            }),
        },
    ]

    const groupItems = [
        { label: 'New channel', Icon: HashIcon, onClick: () => newChannel(null) },
        { label: 'New category', Icon: PlusIcon, onClick: newCategory },
    ]

    const row = (channel) => {
        const classes = [styles.channel, channel.id === openId && styles.open, channel.unreadCount > 0 && styles.unread].filter(Boolean).join(' ')
        return (
            <button
                key={channel.id}
                className={classes}
                type="button"
                aria-current={channel.id === openId}
                onClick={() => onOpen(channel)}
                onContextMenu={channelItems(channel, {}).length > 0 ? (e) => openAt(e, (at) => channelItems(channel, at)) : undefined}
            >
                <span className={styles.hash} aria-label={channel.private ? 'Private channel' : undefined}>{channel.private ? <LockIcon /> : <HashIcon />}</span>
                <span className={styles.name}>{channel.name}</span>
                {channel.unreadCount > 0 && channel.id !== openId && <span className={styles.count}>{formatCount(channel.unreadCount)}</span>}
            </button>
        )
    }

    const loose = list.channels.filter((channel) => !channel.categoryId)

    return (
        <div className={styles.column}>
            <div className={styles.head}>
                <button className={styles.groupName} type="button" onClick={onOpenInfo}>{room.name}</button>
                {canManage && (
                    <button className={styles.plus} type="button" aria-label="Add to group" onClick={(e) => openAt(e, () => groupItems)}>
                        <PlusIcon />
                    </button>
                )}
            </div>
            <div className={styles.list}>
                {loose.map(row)}
                {list.categories.map((category) => {
                    const closed = collapsed.includes(category.id)
                    const channels = list.channels.filter((channel) => channel.categoryId === category.id)
                    const shown = closed ? channels.filter((channel) => channel.unreadCount > 0 || channel.id === openId) : channels
                    return (
                        <div key={category.id} className={styles.category}>
                            <div className={styles.categoryRow} onContextMenu={canManage ? (e) => openAt(e, () => categoryItems(category)) : undefined}>
                                <button className={styles.categoryName} type="button" aria-expanded={!closed} onClick={() => toggle(category.id)}>
                                    <span className={closed ? styles.chevron : `${styles.chevron} ${styles.chevronOpen}`}><ChevronIcon /></span>
                                    {category.name}
                                </button>
                                {canManage && (
                                    <button className={styles.plus} type="button" aria-label={`New channel in ${category.name}`} onClick={() => newChannel(category.id)}>
                                        <PlusIcon />
                                    </button>
                                )}
                            </div>
                            {shown.map(row)}
                        </div>
                    )
                })}
            </div>
            {menu && <ContextMenu key={menu.items.length} x={menu.x} y={menu.y} items={menu.items} onClose={() => setMenu((current) => (current === menu ? null : current))} />}
            {permissionsFor && (
                <ChannelPermissions room={room} target={permissionsFor} onClose={() => setPermissionsFor(null)} onChanged={onChanged} />
            )}
            {dialog && (
                <NameDialog
                    title={dialog.title}
                    label={dialog.label}
                    clean={dialog.clean}
                    initial={dialog.initial}
                    onSubmit={dialog.submit}
                    onClose={() => setDialog(null)}
                />
            )}
            {confirm && (
                <ConfirmDialog title={confirm.title} text={confirm.text} confirmLabel="Yes" onConfirm={confirm.action} onClose={() => setConfirm(null)} />
            )}
        </div>
    )
}

export default ChannelList
