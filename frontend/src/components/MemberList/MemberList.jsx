import styles from './MemberList.module.css'
import Avatar from '../Avatar/Avatar'
import BackIcon from '../Icons/BackIcon'
import CloseIcon from '../Icons/CloseIcon'
import Spinner from '../Spinner/Spinner'
import { MEMBER_LIMIT } from '../../pages/Chat/useGroupMembers'
import { colorOf, rankOf, roleColor, separateRoleOf } from '../../utils/roles'

const nameOf = (member) => (member.deleted ? 'Deleted Account' : member.displayName)

function liveState(member, presence, loadedAt) {
    if (member.disabled || member.deleted) return 'offline'
    const live = presence[member.id]
    const fresh = live && live.at > loadedAt
    const hidden = fresh ? live.hidden : member.lastSeenHidden
    const online = fresh ? live.online : member.online
    if (hidden) return 'hidden'
    return online ? 'online' : 'offline'
}

function MemberList({ group, data, userId, presence, screen, onClose }) {
    const roles = data?.roles ?? []
    const members = data?.members ?? []
    const rankKey = (member) => (member.role === 'owner' ? -1 : rankOf(member.roleIds, roles) ?? Number.MAX_SAFE_INTEGER)
    const byRankThenName = (a, b) => rankKey(a) - rankKey(b) || nameOf(a).localeCompare(nameOf(b))

    const roleSections = new Map()
    const sections = { online: [], offline: [], hidden: [] }
    for (const member of members) {
        const state = liveState(member, presence, data?.loadedAt ?? 0)
        const separate = state === 'online' ? separateRoleOf(member.roleIds, roles) : null
        if (separate) {
            if (!roleSections.has(separate.id)) roleSections.set(separate.id, [])
            roleSections.get(separate.id).push(member)
        } else {
            sections[state].push(member)
        }
    }

    const shown = [
        ...roles.filter((role) => roleSections.has(role.id)).map((role) => [role.id, role.name.toUpperCase(), roleSections.get(role.id), false]),
        ['online', 'ONLINE', sections.online, false],
        ['offline', 'OFFLINE', sections.offline, true],
        ['hidden', 'MEMBERS', sections.hidden, true],
    ].filter(([, , list]) => list.length > 0)

    return (
        <aside className={screen ? `${styles.panel} ${styles.screen}` : styles.panel}>
            <div className={styles.head}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onClose}>
                    <BackIcon />
                </button>
                <span className={styles.title}>Members</span>
                <button className={styles.close} type="button" aria-label="Close members" onClick={onClose}>
                    <CloseIcon />
                </button>
            </div>
            <div className={styles.list}>
                {!data && <div className={styles.loading}><Spinner size={20} /></div>}
                {data && shown.map(([key, title, list, muted]) => (
                    <section key={key} className={styles.section}>
                        <div className={styles.sectionTitle}>{`${title} — ${list.length}`}</div>
                        {[...list].sort(byRankThenName).map((member) => {
                            const color = member.disabled || member.deleted ? null : colorOf(member.roleIds, roles)
                            return (
                                <div key={member.id} className={muted ? `${styles.row} ${styles.muted}` : styles.row}>
                                    <Avatar name={nameOf(member)} size={32} disabled={member.disabled} deleted={member.deleted} />
                                    <span
                                        className={member.disabled ? `${styles.name} ${styles.disabled}` : styles.name}
                                        style={color ? { color: roleColor(color) } : undefined}
                                    >
                                        {nameOf(member)}{member.id === userId ? ' (You)' : ''}
                                    </span>
                                    {member.role === 'owner' && <span className={styles.role}>Owner</span>}
                                </div>
                            )
                        })}
                    </section>
                ))}
                {data && members.length >= MEMBER_LIMIT && group.memberCount > MEMBER_LIMIT && (
                    <div className={styles.more}>{`Showing ${MEMBER_LIMIT} of ${group.memberCount} members`}</div>
                )}
            </div>
        </aside>
    )
}

export default MemberList
