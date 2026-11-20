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
import RoleList from '../Roles/RoleList'
import RoleEditor from '../Roles/RoleEditor'
import MemberRoles from '../Roles/MemberRoles'
import { addMembers, listMembers, MEMBER_PAGE_SIZE, removeMember, resetInvite, setMemberRole, transferRoom, updateRoom } from '../../api/rooms'
import { createRole, deleteRole, listRoles, setMemberRoles, updateRole } from '../../api/roles'
import { roomLabel, roomLink } from '../../utils/roomLink'
import { above, can, colorOf, outranks, rankOf, roleColor } from '../../utils/roles'

const roleNames = { owner: 'Owner', admin: 'Admin' }
const emptyRole = { name: '', color: null, separate: false, permissions: [] }

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
    const [roles, setRoles] = useState([])
    const [roleDraft, setRoleDraft] = useState(null)
    const [memberDraft, setMemberDraft] = useState(null)
    const label = roomLabel(chat.kind)
    const isGroup = chat.kind === 'group'
    const isOwner = chat.role === 'owner'
    const isAdmin = can(chat, 'manageGroup')
    const canManageRoles = isGroup && can(chat, 'manageRoles')
    const me = { owner: isOwner, rank: chat.rank ?? null }
    const link = roomLink(chat)
    const rankOfMember = (member) => ({ owner: member.role === 'owner', rank: rankOf(member.roleIds, roles) })

    useEffect(() => {
        if (!open || !isGroup) return
        let active = true
        listRoles(chat.id)
            .then((list) => active && setRoles(list))
            .catch(() => active && setRoles([]))
        return () => {
            active = false
        }
    }, [open, isGroup, chat.id, refreshKey])

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

    const openRole = (role) => {
        setRoleDraft(role ? { id: role.id, position: role.position, value: { name: role.name, color: role.color, separate: role.separate, permissions: role.permissions } } : { id: null, value: emptyRole })
        setError('')
        setView('role')
    }

    const saveRole = async () => {
        if (busy) return
        setBusy(true)
        setError('')
        try {
            const body = { ...roleDraft.value, name: roleDraft.value.name.trim() }
            if (roleDraft.id) await updateRole(chat.id, roleDraft.id, body)
            else await createRole(chat.id, body)
            setRoles(await listRoles(chat.id))
            setView('roles')
        } catch (failure) {
            setError(failure.message)
        }
        setBusy(false)
    }

    const moveRole = async (direction) => {
        try {
            const moved = await updateRole(chat.id, roleDraft.id, { move: direction })
            setRoleDraft((draft) => ({ ...draft, position: moved.position }))
            setRoles(await listRoles(chat.id))
        } catch (failure) {
            onToast(failure.message)
        }
    }

    const removeRole = () => onConfirm({
        title: `Delete ${roleDraft.value.name}?`,
        text: 'Everyone with this role will lose it.',
        action: async () => {
            try {
                await deleteRole(chat.id, roleDraft.id)
                setRoles(await listRoles(chat.id))
                setView('roles')
            } catch (failure) {
                onToast(failure.message)
            }
        },
    })

    const openMemberRoles = (member) => {
        setMemberDraft({ member, selected: member.roleIds ?? [] })
        setError('')
        setView('memberRoles')
    }

    const saveMemberRoles = async () => {
        if (busy) return
        setBusy(true)
        setError('')
        try {
            await setMemberRoles(chat.id, memberDraft.member.id, memberDraft.selected)
            setRoles(await listRoles(chat.id))
            setView('main')
        } catch (failure) {
            setError(failure.message)
        }
        setBusy(false)
    }

    const groupMemberItems = (member) => {
        const items = []
        const target = rankOfMember(member)
        const reachable = isOwner || outranks(me, target)
        if (canManageRoles && member.status === 'active' && reachable) {
            items.push({ label: 'Roles', Icon: ProfileIcon, onClick: () => openMemberRoles(member) })
        }
        if (member.id === userId || member.role === 'owner') return items
        if (isOwner && member.status === 'active') {
            items.push({
                label: 'Transfer Ownership',
                Icon: ProfileIcon,
                onClick: () => onConfirm({
                    title: 'Transfer ownership?',
                    text: `${memberName(member)} will become the owner. You will get the Admin role.`,
                    action: () => run(() => transferRoom(chat.id, member.id)),
                }),
            })
        }
        if (can(chat, 'removeMembers') && reachable) {
            items.push({
                label: member.status === 'request' ? 'Cancel Invite' : 'Remove',
                Icon: DeleteIcon,
                danger: true,
                onClick: () => onConfirm({
                    title: `Remove ${memberName(member)}?`,
                    text: 'They cannot join again until someone adds them.',
                    action: () => run(() => removeMember(chat.id, member.id)),
                }),
            })
        }
        return items
    }

    const memberItems = (member) => {
        if (isGroup) return groupMemberItems(member)
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
        const roleReadOnly = view === 'role' && roleDraft.id !== null && !above(me, roleDraft.position)
        const titles = {
            edit: `Edit ${label}`,
            add: 'Add Members',
            roles: 'Roles',
            role: roleDraft?.id ? 'Edit Role' : 'New Role',
            memberRoles: 'Roles',
        }
        const actions = {
            edit: { text: 'Save', run: saveEdit },
            add: { text: 'Add', run: saveMembers },
            role: roleReadOnly ? null : { text: 'Save', run: saveRole },
            memberRoles: { text: 'Save', run: saveMemberRoles },
        }
        const action = actions[view]
        const backTo = view === 'role' ? 'roles' : 'main'
        return (
            <aside className={`${styles.panel} ${own.subPanel}`}>
                <div className={own.subHeader}>
                    <button className={own.subBack} type="button" aria-label="Back" onClick={() => setView(backTo)}>
                        <BackIcon />
                    </button>
                    <span className={own.subTitle}>{titles[view]}</span>
                </div>
                {view === 'edit' && <div className={own.subBody}><RoomForm kind={chat.kind} value={form} onChange={setForm} /></div>}
                {view === 'add' && <MemberPicker known={known} selected={picked} excludeIds={members.map((member) => member.id)} onChange={setPicked} />}
                {view === 'roles' && (
                    <div className={own.subBody}>
                        <RoleList roles={roles} canCreate={canManageRoles} onOpen={openRole} onNew={() => openRole(null)} />
                    </div>
                )}
                {view === 'role' && (
                    <div className={own.subBody}>
                        <RoleEditor
                            value={roleDraft.value}
                            onChange={(value) => setRoleDraft((draft) => ({ ...draft, value }))}
                            readOnly={roleReadOnly}
                            canMoveUp={roleDraft.id !== null && above(me, roleDraft.position - 1)}
                            canMoveDown={roleDraft.id !== null && roleDraft.position < roles.length - 1}
                            onMove={roleDraft.id ? moveRole : null}
                            onDelete={roleDraft.id ? removeRole : null}
                        />
                    </div>
                )}
                {view === 'memberRoles' && (
                    <div className={own.subBody}>
                        <MemberRoles
                            name={memberName(memberDraft.member)}
                            roles={roles}
                            selected={memberDraft.selected}
                            isEditable={(role) => above(me, role.position)}
                            onToggle={(id, on) => setMemberDraft((draft) => ({
                                ...draft,
                                selected: on ? [...draft.selected, id] : draft.selected.filter((item) => item !== id),
                            }))}
                        />
                    </div>
                )}
                {action && (
                    <div className={own.subFooter}>
                        {error && <p className={own.error}>{error}</p>}
                        <button className={own.save} type="button" disabled={busy} onClick={action.run}>
                            {busy ? <Spinner size={20} /> : action.text}
                        </button>
                    </div>
                )}
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
                    {canManageRoles && (
                        <button className={styles.action} type="button" onClick={() => setView('roles')}>
                            <ProfileIcon />
                            Roles
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
                    {members.map((member) => {
                        const color = isGroup && !member.disabled && !member.deleted ? colorOf(member.roleIds, roles) : null
                        const badge = isGroup ? (member.role === 'owner' ? 'Owner' : '') : roleNames[member.role]
                        return (
                            <button key={member.id} className={own.member} type="button" onClick={(e) => openMember(e, member)}>
                                <Avatar name={memberName(member)} size={40} disabled={member.disabled} deleted={member.deleted} />
                                <span className={own.memberText}>
                                    <span className={own.memberName} style={color ? { color: roleColor(color) } : undefined}>{memberName(member)}{member.id === userId ? ' (You)' : ''}</span>
                                    <span className={styles.label}>{member.status === 'request' ? 'Invited' : member.username ? `@${member.username}` : ''}</span>
                                </span>
                                {badge && <span className={own.role}>{badge}</span>}
                            </button>
                        )
                    })}
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
