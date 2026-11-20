import styles from './Roles.module.css'
import { roleColor } from '../../utils/roles'

function MemberRoles({ name, roles, selected, isEditable, onToggle }) {
    return (
        <div className={styles.list}>
            <p className={styles.note}>{`Roles for ${name}`}</p>
            {roles.length === 0 && <p className={styles.empty}>No roles yet</p>}
            {roles.map((role) => {
                const editable = isEditable(role)
                return (
                    <label key={role.id} className={editable ? styles.checkRow : `${styles.checkRow} ${styles.locked}`}>
                        <input
                            className={styles.check}
                            type="checkbox"
                            checked={selected.includes(role.id)}
                            disabled={!editable}
                            onChange={(e) => onToggle(role.id, e.target.checked)}
                        />
                        <span className={styles.dot} style={{ background: roleColor(role.color) ?? 'var(--surface-strong)' }} />
                        <span className={styles.roleName}>{role.name}</span>
                    </label>
                )
            })}
        </div>
    )
}

export default MemberRoles
