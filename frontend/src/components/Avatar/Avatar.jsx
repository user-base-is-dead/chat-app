import styles from './Avatar.module.css'
import DisabledUserIcon from '../Icons/DisabledUserIcon'
import DeletedUserIcon from '../Icons/DeletedUserIcon'
import PersonIcon from '../Icons/PersonIcon'

function Avatar({ name, photo, size = 54, disabled, deleted, morphTarget }) {
    const marks = { 'data-avatar': '', 'data-morph-target': morphTarget ? '' : undefined }

    if (deleted) {
        return (
            <div className={`${styles.avatar} ${styles.disabled}`} style={{ width: size, height: size }} aria-label="Deleted account" {...marks}>
                <DeletedUserIcon />
            </div>
        )
    }

    if (disabled) {
        return (
            <div className={`${styles.avatar} ${styles.disabled}`} style={{ width: size, height: size }} aria-label="Disabled account" {...marks}>
                <DisabledUserIcon />
            </div>
        )
    }

    return (
        <div className={`${styles.avatar} ${styles.blank}`} style={{ width: size, height: size }} aria-label={name} {...marks}>
            {photo ? <img src={photo} alt="" draggable="false" /> : <PersonIcon />}
        </div>
    )
}

export default Avatar
