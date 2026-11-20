import { useEffect, useState } from 'react'
import styles from './ChannelPermissions.module.css'
import Avatar from '../Avatar/Avatar'
import BackIcon from '../Icons/BackIcon'
import CloseIcon from '../Icons/CloseIcon'
import PlusIcon from '../Icons/PlusIcon'
import Spinner from '../Spinner/Spinner'
import { getCategoryPermissions, getChannelPermissions, setCategoryOverride, setChannelOverride, syncChannel } from '../../api/channels'
import { listMembers, MEMBER_PAGE_SIZE } from '../../api/rooms'
import { listRoles } from '../../api/roles'
import { CHANNEL_PERMISSIONS, can, roleColor } from '../../utils/roles'

async function allMembers(roomId) {
    const all = []
    while (all.length < 1000) {
        const page = await listMembers(roomId, all.length)
        all.push(...page)
        if (page.length < MEMBER_PAGE_SIZE) break
    }
    return all.filter((member) => member.status === 'active')
}

const stateOf = (entry, key) => (entry?.allow.includes(key) ? 'allow' : entry?.deny.includes(key) ? 'deny' : 'inherit')
const nameOf = (member) => (member.deleted ? 'Deleted Account' : member.displayName)

function ChannelPermissions({ room, target, onClose, onChanged }) {
    const [data, setData] = useState(null)
    const [roles, setRoles] = useState([])
    const [members, setMembers] = useState([])
    const [view, setView] = useState('list')
    const [editing, setEditing] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const isChannel = target.kind === 'channel'
    const full = room.role === 'owner' || can(room, 'administrator')

    useEffect(() => {
        let active = true
        const load = isChannel ? getChannelPermissions(room.id, target.id) : getCategoryPermissions(room.id, target.id)
        Promise.all([load, listRoles(room.id), allMembers(room.id)])
            .then(([permissions, roleList, memberList]) => {
                if (!active) return
                setData(permissions)
                setRoles(roleList)
                setMembers(memberList)
            })
            .catch((failure) => active && setError(failure.message))
        return () => {
            active = false
        }
    }, [room.id, target.id, isChannel])

    const labelOf = (entry) => {
        if (entry.targetType === 'everyone') return { text: 'Everyone' }
        if (entry.targetType === 'role') {
            const role = roles.find((item) => item.id === entry.targetId)
            return { text: role?.name ?? 'Deleted role', color: role?.color }
        }
        const member = members.find((item) => item.id === entry.targetId)
        return { text: member ? nameOf(member) : 'Former member', member }
    }

    const overrides = data?.overrides ?? []
    const everyone = overrides.find((entry) => entry.targetType === 'everyone') ?? { targetType: 'everyone', targetId: '0', allow: [], deny: [] }
    const roleEntries = overrides
        .filter((entry) => entry.targetType === 'role')
        .sort((a, b) => (roles.find((r) => r.id === a.targetId)?.position ?? 99) - (roles.find((r) => r.id === b.targetId)?.position ?? 99))
    const memberEntries = overrides.filter((entry) => entry.targetType === 'member')
    const entries = [everyone, ...roleEntries, ...memberEntries]

    const openEditor = (entry) => {
        setEditing({ ...entry, allow: [...entry.allow], deny: [...entry.deny] })
        setError('')
        setView('edit')
    }

    const setState = (key, state) => {
        setEditing((current) => ({
            ...current,
            allow: state === 'allow' ? [...current.allow.filter((item) => item !== key), key] : current.allow.filter((item) => item !== key),
            deny: state === 'deny' ? [...current.deny.filter((item) => item !== key), key] : current.deny.filter((item) => item !== key),
        }))
    }

    const save = async (entry) => {
        if (busy) return
        setBusy(true)
        setError('')
        try {
            const write = isChannel ? setChannelOverride : setCategoryOverride
            setData(await write(room.id, target.id, entry.targetType, entry.targetId, entry.allow, entry.deny))
            onChanged()
            setView('list')
        } catch (failure) {
            setError(failure.message)
        }
        setBusy(false)
    }

    const sync = async () => {
        if (busy) return
        setBusy(true)
        try {
            setData(await syncChannel(room.id, target.id))
            onChanged()
        } catch (failure) {
            setError(failure.message)
        }
        setBusy(false)
    }

    const taken = new Set(overrides.map((entry) => `${entry.targetType}:${entry.targetId}`))
    const addable = [
        ...roles.filter((role) => !taken.has(`role:${role.id}`)).map((role) => ({ targetType: 'role', targetId: role.id, allow: [], deny: [] })),
        ...members
            .filter((member) => member.role !== 'owner' && !taken.has(`member:${member.id}`))
            .map((member) => ({ targetType: 'member', targetId: member.id, allow: [], deny: [] })),
    ]

    const title = view === 'edit' ? labelOf(editing).text : view === 'add' ? 'Add role or member' : 'Permissions'
    const subtitle = isChannel ? `# ${target.name}` : target.name

    const entryRow = (entry, onClick) => {
        const label = labelOf(entry)
        return (
            <button key={`${entry.targetType}:${entry.targetId}`} className={styles.entry} type="button" onClick={onClick}>
                {entry.targetType === 'member' ? (
                    <Avatar name={label.text} size={28} disabled={label.member?.disabled} deleted={label.member?.deleted} />
                ) : (
                    <span className={styles.dot} style={{ background: entry.targetType === 'role' ? roleColor(label.color) ?? 'var(--surface-strong)' : 'var(--accent)' }} />
                )}
                <span className={styles.entryName} style={label.color ? { color: roleColor(label.color) } : undefined}>{label.text}</span>
                <span className={styles.entryKind}>{entry.targetType === 'everyone' ? 'All members' : entry.targetType === 'role' ? 'Role' : 'Member'}</span>
            </button>
        )
    }

    return (
        <div className={styles.backdrop} onClick={onClose}>
            <div className={styles.sheet} role="dialog" aria-label={`Permissions for ${subtitle}`} onClick={(e) => e.stopPropagation()}>
                <div className={styles.head}>
                    {view !== 'list' ? (
                        <button className={styles.iconButton} type="button" aria-label="Back" onClick={() => setView('list')}>
                            <BackIcon />
                        </button>
                    ) : (
                        <span className={styles.spacer} />
                    )}
                    <span className={styles.titles}>
                        <span className={styles.title}>{title}</span>
                        <span className={styles.subtitle}>{subtitle}</span>
                    </span>
                    <button className={styles.iconButton} type="button" aria-label="Close" onClick={onClose}>
                        <CloseIcon />
                    </button>
                </div>

                <div className={styles.body}>
                    {!data && !error && <div className={styles.loading}><Spinner size={22} /></div>}
                    {error && view !== 'edit' && <p className={styles.error}>{error}</p>}

                    {data && view === 'list' && (
                        <>
                            {isChannel && data.categoryId && (
                                <div className={styles.sync}>
                                    <span>{data.synced ? 'Synced with its category' : 'Not synced with its category'}</span>
                                    {!data.synced && (
                                        <button className={styles.syncButton} type="button" disabled={busy} onClick={sync}>Sync now</button>
                                    )}
                                </div>
                            )}
                            {entries.map((entry) => entryRow(entry, () => openEditor(entry)))}
                            <button className={styles.add} type="button" onClick={() => setView('add')}>
                                <PlusIcon />
                                Add role or member
                            </button>
                        </>
                    )}

                    {data && view === 'add' && (
                        <>
                            {addable.length === 0 && <p className={styles.empty}>Everyone already has an entry</p>}
                            {addable.map((entry) => entryRow(entry, () => openEditor(entry)))}
                        </>
                    )}

                    {data && view === 'edit' && editing && (
                        <>
                            {CHANNEL_PERMISSIONS.map((permission) => {
                                const state = stateOf(editing, permission.key)
                                const locked = !full && !data.permissions.includes(permission.key)
                                return (
                                    <div key={permission.key} className={styles.permission}>
                                        <span className={styles.permissionText}>
                                            {permission.label}
                                            <span className={styles.hint}>{permission.hint}</span>
                                        </span>
                                        <span className={styles.tri} role="radiogroup" aria-label={permission.label}>
                                            {[['deny', '✕', 'Deny'], ['inherit', '/', 'Inherit'], ['allow', '✓', 'Allow']].map(([value, mark, name]) => (
                                                <button
                                                    key={value}
                                                    className={state === value ? `${styles.triButton} ${styles[`tri_${value}`]}` : styles.triButton}
                                                    type="button"
                                                    role="radio"
                                                    aria-checked={state === value}
                                                    aria-label={name}
                                                    disabled={locked}
                                                    onClick={() => setState(permission.key, value)}
                                                >
                                                    {mark}
                                                </button>
                                            ))}
                                        </span>
                                    </div>
                                )
                            })}
                            {error && <p className={styles.error}>{error}</p>}
                            <button className={styles.save} type="button" disabled={busy} onClick={() => save(editing)}>
                                {busy ? <Spinner size={18} /> : 'Save'}
                            </button>
                            {editing.targetType !== 'everyone' && taken.has(`${editing.targetType}:${editing.targetId}`) && (
                                <button className={styles.remove} type="button" disabled={busy} onClick={() => save({ ...editing, allow: [], deny: [] })}>
                                    Remove override
                                </button>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}

export default ChannelPermissions
