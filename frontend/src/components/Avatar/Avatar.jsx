import styles from './Avatar.module.css'
import DisabledUserIcon from '../Icons/DisabledUserIcon'
import DeletedUserIcon from '../Icons/DeletedUserIcon'
import PersonIcon from '../Icons/PersonIcon'

const tones = [
    ['#F28B6B', '#D9573F'],
    ['#F5C26B', '#E39A3B'],
    ['#8FA8F5', '#5B6FE0'],
    ['#8FD19E', '#4FA866'],
    ['#6FD3C9', '#2FA89B'],
    ['#7CC3F0', '#3A8FD6'],
    ['#D9A0E8', '#B067CC'],
    ['#F29BB5', '#D9628A'],
]

export function initials(name) {
    const words = (name || '').trim().split(/\s+/).filter(Boolean)
    return words.slice(0, 2).map((word) => String.fromCodePoint(word.codePointAt(0))).join('').toUpperCase()
}

export function tone(name) {
    let hash = 0
    for (let i = 0; i < (name || '').length; i++) hash = (Math.imul(hash, 31) + name.charCodeAt(i)) | 0
    return tones[((hash % tones.length) + tones.length) % tones.length]
}

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

    const letters = initials(name)

    if (photo || !letters) {
        return (
            <div className={`${styles.avatar} ${styles.blank}`} style={{ width: size, height: size }} aria-label={name} {...marks}>
                {photo ? <img src={photo} alt="" draggable="false" /> : <PersonIcon />}
            </div>
        )
    }

    const [top, bottom] = tone(name.trim())
    return (
        <div
            className={styles.avatar}
            style={{ width: size, height: size, fontSize: size * (letters.length > 1 ? 0.38 : 0.44), background: `linear-gradient(${top}, ${bottom})` }}
            aria-label={name}
            {...marks}
        >
            {letters}
        </div>
    )
}

export default Avatar
