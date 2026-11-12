import { useState } from 'react'
import styles from './RoomCreate.module.css'
import BackIcon from '../Icons/BackIcon'
import Spinner from '../Spinner/Spinner'
import RoomForm from '../RoomForm/RoomForm'
import MemberPicker from '../MemberPicker/MemberPicker'
import { createRoom } from '../../api/rooms'

const blank = { title: '', about: '', isPublic: false, username: '', photoId: '', photo: '' }

function RoomCreate({ kind, known, onCreated }) {
    const [step, setStep] = useState('details')
    const [form, setForm] = useState(blank)
    const [members, setMembers] = useState([])
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const label = kind === 'channel' ? 'Channel' : 'Group'

    const goNext = () => {
        if (!form.title.trim()) {
            setError(`${label} name is required`)
            return
        }
        if (form.isPublic && !form.username.trim()) {
            setError('Public needs a username')
            return
        }
        setError('')
        setStep('members')
    }

    const goBack = () => {
        if (step === 'members') setStep('details')
        else window.history.back()
    }

    const create = async () => {
        if (busy) return
        setBusy(true)
        setError('')
        try {
            const room = await createRoom({
                kind,
                title: form.title,
                about: form.about,
                isPublic: form.isPublic,
                username: form.isPublic ? form.username : '',
                photoId: form.photoId,
                userIds: members.map((person) => person.id),
            })
            onCreated(room)
        } catch (failure) {
            setError(failure.message)
            setStep('details')
            setBusy(false)
        }
    }

    return (
        <>
            <div className={styles.header}>
                <button className={styles.back} type="button" aria-label="Back" onClick={goBack}>
                    <BackIcon />
                </button>
                <span className={styles.title}>{step === 'details' ? `New ${label}` : 'Add Members'}</span>
            </div>
            {step === 'details' ? (
                <div className={styles.body}>
                    <RoomForm kind={kind} value={form} onChange={setForm} />
                </div>
            ) : (
                <MemberPicker known={known} selected={members} onChange={setMembers} />
            )}
            <div className={styles.footer}>
                {error && <p className={styles.error}>{error}</p>}
                <button className={styles.next} type="button" disabled={busy} onClick={step === 'details' ? goNext : create}>
                    {busy ? <Spinner size={20} /> : step === 'details' ? 'Next' : `Create ${label}`}
                </button>
            </div>
        </>
    )
}

export default RoomCreate
