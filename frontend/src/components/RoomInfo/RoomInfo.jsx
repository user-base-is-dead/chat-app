import { useEffect, useState } from 'react'
import styles from '../UserInfo/UserInfo.module.css'
import own from './RoomInfo.module.css'
import Avatar from '../Avatar/Avatar'
import CloseIcon from '../Icons/CloseIcon'
import BackIcon from '../Icons/BackIcon'
import MuteIcon from '../Icons/MuteIcon'
import UnmuteIcon from '../Icons/UnmuteIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import EditIcon from '../Icons/EditIcon'
import LogoutIcon from '../Icons/LogoutIcon'
import PlusIcon from '../Icons/PlusIcon'
import LinkIcon from '../Icons/LinkIcon'
import ProfileIcon from '../Icons/ProfileIcon'
import ContextMenu from '../ContextMenu/ContextMenu'
import PeerStatus from '../PeerStatus/PeerStatus'
import RoomForm from '../RoomForm/RoomForm'
import MemberPicker from '../MemberPicker/MemberPicker'
import Spinner from '../Spinner/Spinner'
import { addMembers, listMembers, MEMBER_PAGE_SIZE, removeMember, resetInvite, setMemberRole, transferRoom, updateRoom } from '../../api/rooms'
import { roomLabel, roomLink } from '../../utils/roomLink'

const roleNames = { owner: 'Owner', admin: 'Admin' }

function memberName(member) {
    return member.deleted ? 'Deleted Account' : member.displayName
}

function RoomInfo({ chat, open, refreshKey, typingName, known, userId, onClose, onChanged, onToggleMute, onLeave, onDelete, onConfirm, onToast }) {
    const [view, setView] = useState('main')
    const [members, setMembers] = useState([])
    const [hasMore, setHasMore] = useState(false)
    const [menu, setMenu] = useState(null)
    const [form, setForm] = useState(null)
    const [picked, setPicked] = useState([])
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [copied, setCopied] = useState(false)
    const label = roomLabel(chat.kind)
    const isOwner = chat.role === 'owner'
    const isAdmin = isOwner || chat.role === 'admin'
    const link = roomLink(chat)

    useEffect(() => {
        if (!open) return
        listMembers(chat.id)
            .then((list) => {
                setMembers(list)
                setHasMore(list.length === MEMBER_PAGE_SIZE)
            })
            .catch(() => setMembers([]))
    }, [open, chat.id, refreshKey])

    const loadMore = async () => {
        const list = await listMembers(chat.id, members.length).catch(() => [])
        setMembers((prev) => [...prev, ...list])
        setHasMore(list.length === MEMBER_PAGE_SIZE)
    }

    const copyLink = async () => {
        try {
            await navigator.clipboard.writeText(link)
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        } catch {
            setCopied(false)
        }
    }

    const openEdit = () => {
        setForm({ title: chat.name, about: chat.about, isPublic: chat.isPublic, username: chat.username, photo: chat.photo, photoId: '' })
        setError('')
        setView('edit')
    }

    const saveEdit = async () => {
        if (busy) return
        setBusy(true)
        setError('')
        try {
            const changes = { title: form.title, about: form.about, isPublic: form.isPublic, username: form.isPublic ? form.username : '' }
            if (form.photoId) changes.photoId = form.photoId
            onChanged(await updateRoom(chat.id, changes))
            setView('main')
        } catch (failure) {
            setError(failure.message)
        }
        setBusy(false)
    }

    const saveMembers = async () => {
        if (busy || picked.length === 0) return
        setBusy(true)
        try {
            const result = await addMembers(chat.id, picked.map((person) => person.id))
            const parts = [result.added && `${result.added} added`, result.invited && `${result.invited} invited`, result.skipped && `${result.skipped} skipped`]
            onToast(parts.filter(Boolean).join(', '))
            setPicked([])
            setView('main')
        } catch (failure) {
            onToast(failure.message)
        }
        setBusy(false)
    }

    const toggle = async (changes) => {
        try {
            onChanged(await updateRoom(chat.id, changes))
        } catch (failure) {
            onToast(failure.message)
        }
    }

    const resetLink = () => onConfirm({
        title: 'Reset invite link?',
        text: 'The old link will stop working.',
        action: async () => {
            try {
                onChanged(await resetInvite(chat.id))
            } catch (failure) {
                onToast(failure.message)
            }
        },
    })

    const run = async (request) => {
        try {
            await request()
        } catch (failure) {
            onToast(failure.message)
        }
    }

    const memberItems = (member) => {
        if (member.id === userId || member.role === 'owner') return []
        const items = []
        if (isOwner && member.status === 'active') {
            items.push(member.role === 'admin'
                ? { label: 'Remove Admin', Icon: ProfileIcon, onClick: () => run(() => setMemberRole(chat.id, member.id, 'member')) }
                : { label: 'Make Admin', Icon: ProfileIcon, onClick: () => run(() => setMemberRole(chat.id, member.id, 'admin')) })
            items.push({
                label: 'Transfer Ownership',
                Icon: ProfileIcon,
                onClick: () => onConfirm({
                    title: 'Transfer ownership?',
                    text: `${memberName(member)} will become the owner. You will stay as an admin.`,
                    action: () => run(() => transferRoom(chat.id, member.id)),
                }),
            })
        }
        if (isOwner || (isAdmin && member.role === 'member')) {
            items.push({
                label: member.status === 'request' ? 'Cancel Invite' : 'Remove',
                Icon: DeleteIcon,
                danger: true,
                onClick: () => onConfirm({
                    title: `Remove ${memberName(member)}?`,
                    text: 'They cannot join again until an admin adds them.',
                    action: () => run(() => removeMember(chat.id, member.id)),
                }),
            })
        }
        return items
    }

    const openMember = (e, member) => {
        const items = memberItems(member)
        if (items.length > 0) setMenu({ x: e.clientX, y: e.clientY, items })
    }

    if (view !== 'main') {
        return (
            <aside className={`${styles.panel} ${own.subPanel}`}>
                <div className={own.subHeader}>
                    <button className={own.subBack} type="button" aria-label="Back" onClick={() => setView('main')}>
                        <BackIcon />
                    </button>
                    <span className={own.subTitle}>{view === 'edit' ? `Edit ${label}` : 'Add Members'}</span>
                </div>
                {view === 'edit' ? (
                    <div className={own.subBody}><RoomForm kind={chat.kind} value={form} onChange={setForm} /></div>
                ) : (
                    <MemberPicker known={known} selected={picked} excludeIds={members.map((member) => member.id)} onChange={setPicked} />
                )}
                <div className={own.subFooter}>
                    {error && <p className={own.error}>{error}</p>}
                    <button className={own.save} type="button" disabled={busy} onClick={view === 'edit' ? saveEdit : saveMembers}>
                        {busy ? <Spinner size={20} /> : view === 'edit' ? 'Save' : 'Add'}
                    </button>
                </div>
            </aside>
        )
    }

    return (
        <aside className={styles.panel}>
            <div className={styles.top}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onClose}>
                    <BackIcon />
                </button>
                <span className={styles.title}>{label} Info</span>
                <button className={styles.close} type="button" aria-label="Close" onClick={onClose}>
                    <CloseIcon />
                </button>
            </div>
            <div className={styles.body}>
                <div className={styles.hero}>
                    <Avatar name={chat.name} photo={chat.photo} size={104} />
                    <div className={styles.name}>{chat.name}</div>
                    <div className={styles.status}>
                        <PeerStatus key={chat.id} chat={chat} typing={Boolean(typingName)} typingName={typingName} centered />
                    </div>
                </div>
                {(chat.about || link) && (
                    <div className={styles.card}>
                        {chat.about && (
                            <div className={styles.row}>
                                <span className={styles.bio}>{chat.about}</span>
                                <span className={styles.label}>Description</span>
                            </div>
                        )}
                        {link && (
                            <button className={styles.row} type="button" onClick={copyLink}>
                                <span className={styles.value}>{copied ? 'Copied' : link}</span>
                                <span className={styles.label}>{chat.isPublic ? 'Link' : 'Invite Link'}</span>
                            </button>
                        )}
                    </div>
                )}
                <div className={styles.card}>
                    <button className={styles.action} type="button" onClick={onToggleMute}>
                        {chat.muted ? <UnmuteIcon /> : <MuteIcon />}
                        {chat.muted ? 'Unmute' : 'Mute'}
                    </button>
                    {isAdmin && (
                        <button className={styles.action} type="button" onClick={openEdit}>
                            <EditIcon />
                            Edit {label}
                        </button>
                    )}
                    {isAdmin && !chat.isPublic && (
                        <button className={styles.action} type="button" onClick={resetLink}>
                            <LinkIcon />
                            Reset Invite Link
                        </button>
                    )}
                    {chat.canAdd && (
                        <button className={styles.action} type="button" onClick={() => setView('add')}>
                            <PlusIcon />
                            Add Members
                        </button>
                    )}
                </div>
                {isAdmin && chat.kind === 'group' && (
                    <div className={styles.card}>
                        <div className={own.cardTitle}>Permissions</div>
                        <label className={own.switchRow}>
                            Members can send messages
                            <input className={own.switch} type="checkbox" checked={chat.membersCanSend} onChange={(e) => toggle({ membersCanSend: e.target.checked })} />
                        </label>
                        <label className={own.switchRow}>
                            Members can add members
                            <input className={own.switch} type="checkbox" checked={chat.membersCanAdd} onChange={(e) => toggle({ membersCanAdd: e.target.checked })} />
                        </label>
                    </div>
                )}
                <div className={styles.card}>
                    <div className={own.cardTitle}>Members</div>
                    {members.map((member) => (
                        <button key={member.id} className={own.member} type="button" onClick={(e) => openMember(e, member)}>
                            <Avatar name={memberName(member)} size={40} disabled={member.disabled} deleted={member.deleted} />
                            <span className={own.memberText}>
                                <span className={own.memberName}>{memberName(member)}{member.id === userId ? ' (You)' : ''}</span>
                                <span className={styles.label}>{member.status === 'request' ? 'Invited' : member.username ? `@${member.username}` : ''}</span>
                            </span>
                            {roleNames[member.role] && <span className={own.role}>{roleNames[member.role]}</span>}
                        </button>
                    ))}
                    {hasMore && <button className={styles.action} type="button" onClick={loadMore}>Show more</button>}
                </div>
                <div className={styles.card}>
                    {!isOwner && (
                        <button className={`${styles.action} ${styles.danger}`} type="button" onClick={onLeave}>
                            <LogoutIcon />
                            Leave {label}
                        </button>
                    )}
                    {isOwner && (
                        <button className={`${styles.action} ${styles.danger}`} type="button" onClick={onDelete}>
                            <DeleteIcon />
                            Delete {label}
                        </button>
                    )}
                </div>
            </div>
            {menu && <ContextMenu x={menu.x} y={menu.y} items={menu.items} onClose={() => setMenu(null)} />}
        </aside>
    )
}

export default RoomInfo
