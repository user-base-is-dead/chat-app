import styles from './Roles.module.css'
import Input from '../Input/Input'
import DeleteIcon from '../Icons/DeleteIcon'
import { PERMISSIONS, ROLE_COLORS, ROLE_NAME_LIMIT, roleColor } from '../../utils/roles'

function RoleEditor({ value, onChange, readOnly, canMoveUp, canMoveDown, onMove, onDelete }) {
    const set = (changes) => onChange({ ...value, ...changes })
    const togglePermission = (key, on) => {
        const next = on ? [...value.permissions, key] : value.permissions.filter((item) => item !== key)
        set({ permissions: PERMISSIONS.map((item) => item.key).filter((item) => next.includes(item)) })
    }

    return (
        <div className={styles.editor}>
            {readOnly && <p className={styles.note}>This role is at or above your highest role, so you can only view it.</p>}
            <fieldset className={styles.fields} disabled={readOnly}>
                <Input label="Role name" value={value.name} maxLength={ROLE_NAME_LIMIT} onChange={(e) => set({ name: e.target.value })} />

                <div className={styles.block}>
                    <span className={styles.blockTitle}>Colour</span>
                    <div className={styles.swatches}>
                        <button
                            className={value.color ? styles.swatchNone : `${styles.swatchNone} ${styles.swatchOn}`}
                            type="button"
                            aria-label="No colour"
                            aria-pressed={!value.color}
                            onClick={() => set({ color: null })}
                        />
                        {ROLE_COLORS.map((color) => (
                            <button
                                key={color}
                                className={value.color === color ? `${styles.swatch} ${styles.swatchOn}` : styles.swatch}
                                style={{ background: roleColor(color) }}
                                type="button"
                                aria-label={color}
                                aria-pressed={value.color === color}
                                onClick={() => set({ color })}
                            />
                        ))}
                    </div>
                </div>

                <label className={styles.switchRow}>
                    <span className={styles.switchText}>
                        Show members separately
                        <span className={styles.hint}>Online members with this role get their own section in the member list</span>
                    </span>
                    <input className={styles.switch} type="checkbox" checked={value.separate} onChange={(e) => set({ separate: e.target.checked })} />
                </label>

                <div className={styles.block}>
                    <span className={styles.blockTitle}>Permissions</span>
                    {PERMISSIONS.map((permission) => (
                        <label key={permission.key} className={styles.switchRow}>
                            <span className={styles.switchText}>
                                {permission.label}
                                <span className={styles.hint}>{permission.hint}</span>
                            </span>
                            <input
                                className={styles.switch}
                                type="checkbox"
                                checked={value.permissions.includes(permission.key)}
                                onChange={(e) => togglePermission(permission.key, e.target.checked)}
                            />
                        </label>
                    ))}
                </div>
            </fieldset>

            {onMove && !readOnly && (
                <div className={styles.moves}>
                    <button className={styles.move} type="button" disabled={!canMoveUp} onClick={() => onMove('up')}>Move up</button>
                    <button className={styles.move} type="button" disabled={!canMoveDown} onClick={() => onMove('down')}>Move down</button>
                </div>
            )}
            {onDelete && !readOnly && (
                <button className={styles.delete} type="button" onClick={onDelete}>
                    <DeleteIcon />
                    Delete Role
                </button>
            )}
        </div>
    )
}

export default RoleEditor
