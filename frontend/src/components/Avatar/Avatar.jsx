import styles from './Avatar.module.css'
import DisabledUserIcon from '../Icons/DisabledUserIcon'
import DeletedUserIcon from '../Icons/DeletedUserIcon'
import { chromaFor } from '../../utils/chroma'

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
        <div
            className={styles.avatar}
            style={{ width: size, height: size, fontSize: size * 0.42, background: chromaFor(name) }}
            {...marks}
        >
            {photo ? <img src={photo} alt="" draggable="false" /> : name[0]}
        </div>
    )
}

export default Avatar
