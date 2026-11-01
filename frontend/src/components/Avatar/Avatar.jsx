import styles from './Avatar.module.css'
import DisabledUserIcon from '../Icons/DisabledUserIcon'
import DeletedUserIcon from '../Icons/DeletedUserIcon'

const colors = ['#ff5252', '#ff9800', '#7c4dff', '#26a69a', '#2196f3', '#ec407a', '#43a047']

function getColor(name) {
    let sum = 0
    for (const char of name) sum += char.charCodeAt(0)
    return colors[sum % colors.length]
}

function Avatar({ name, size = 54, disabled, deleted }) {
    const color = getColor(name)

    if (deleted) {
        return (
            <div className={`${styles.avatar} ${styles.disabled}`} style={{ width: size, height: size }} aria-label="Deleted account">
                <DeletedUserIcon />
            </div>
        )
    }

    if (disabled) {
        return (
            <div className={`${styles.avatar} ${styles.disabled}`} style={{ width: size, height: size }} aria-label="Disabled account">
                <DisabledUserIcon />
            </div>
        )
    }

    return (
        <div
            className={styles.avatar}
            style={{ width: size, height: size, fontSize: size * 0.4, background: color }}
        >
            {name[0]}
        </div>
    )
}

export default Avatar
