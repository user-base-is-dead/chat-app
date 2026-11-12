import { useState } from 'react'
import styles from './RoomForm.module.css'
import Avatar from '../Avatar/Avatar'
import CameraIcon from '../Icons/CameraIcon'
import Spinner from '../Spinner/Spinner'
import { uploadPhoto } from '../../api/rooms'

const MAX_PHOTO = 5 * 1024 * 1024

function RoomForm({ kind, value, onChange }) {
    const [uploading, setUploading] = useState(false)
    const [photoError, setPhotoError] = useState('')
    const label = kind === 'channel' ? 'Channel' : 'Group'

    const change = (changes) => onChange({ ...value, ...changes })

    const pickPhoto = async (e) => {
        const file = e.target.files[0]
        e.target.value = ''
        if (!file) return
        if (file.size > MAX_PHOTO) {
            setPhotoError('Photo must be under 5 MB')
            return
        }
        setPhotoError('')
        setUploading(true)
        try {
            const photo = await uploadPhoto(file)
            change({ photoId: photo.id, photo: photo.url })
        } catch (error) {
            setPhotoError(error.message)
        }
        setUploading(false)
    }

    return (
        <div className={styles.form}>
            <label className={styles.photo} aria-label="Choose photo">
                <input className={styles.file} type="file" accept="image/jpeg,image/png,image/webp" onChange={pickPhoto} />
                {value.photo ? <Avatar name={value.title || label} photo={value.photo} size={96} /> : <span className={styles.empty}><CameraIcon /></span>}
                {uploading && <span className={styles.busy}><Spinner size={24} /></span>}
            </label>
            {photoError && <p className={styles.error}>{photoError}</p>}
            <label className={styles.field}>
                <span className={styles.label}>{label} name</span>
                <input className={styles.input} type="text" maxLength={64} value={value.title} onChange={(e) => change({ title: e.target.value })} />
            </label>
            <label className={styles.field}>
                <span className={styles.label}>Description</span>
                <textarea className={styles.area} rows={3} maxLength={255} value={value.about} onChange={(e) => change({ about: e.target.value })} />
            </label>
            <div className={styles.types}>
                <span className={value.isPublic ? `${styles.slider} ${styles.sliderRight}` : styles.slider} />
                <button className={value.isPublic ? styles.type : `${styles.type} ${styles.typeActive}`} type="button" onClick={() => change({ isPublic: false })}>Private</button>
                <button className={value.isPublic ? `${styles.type} ${styles.typeActive}` : styles.type} type="button" onClick={() => change({ isPublic: true })}>Public</button>
            </div>
            <p className={styles.hint}>
                {value.isPublic ? 'Anyone can find and join it. People you add join directly.' : 'Only people with the link can join. People you add get an invite.'}
            </p>
            {value.isPublic && (
                <label className={styles.field}>
                    <span className={styles.label}>@username</span>
                    <input className={styles.input} type="text" maxLength={32} value={value.username} onChange={(e) => change({ username: e.target.value.replace(/^@/, '') })} />
                </label>
            )}
        </div>
    )
}

export default RoomForm
