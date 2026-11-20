import styles from './Roles.module.css'
import ChevronIcon from '../Icons/ChevronIcon'
import PlusIcon from '../Icons/PlusIcon'
import { roleColor } from '../../utils/roles'

function RoleList({ roles, canCreate, onOpen, onNew }) {
    return (
        <div className={styles.list}>
            {canCreate && (
                <button className={styles.newRole} type="button" onClick={onNew}>
                    <PlusIcon />
                    New Role
                </button>
            )}
            {roles.length === 0 && <p className={styles.empty}>No roles yet</p>}
            {roles.map((role) => (
                <button key={role.id} className={styles.roleRow} type="button" onClick={() => onOpen(role)}>
                    <span className={styles.dot} style={{ background: roleColor(role.color) ?? 'var(--surface-strong)' }} />
                    <span className={styles.roleName}>{role.name}</span>
                    <span className={styles.count}>{role.memberCount === 1 ? '1 member' : `${role.memberCount} members`}</span>
                    <span className={styles.chevron}><ChevronIcon /></span>
                </button>
            ))}
        </div>
    )
}

export default RoleList
