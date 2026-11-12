import { useEffect, useRef, useState } from 'react'
import styles from './ActionBubble.module.css'
import BubbleIcon from '../Icons/BubbleIcon'
import SearchIcon from '../Icons/SearchIcon'
import PlusIcon from '../Icons/PlusIcon'

const actions = [
    { id: 'new-group', label: 'New Group', Icon: PlusIcon },
    { id: 'new-channel', label: 'New Channel', Icon: PlusIcon },
    { id: 'contacts', label: 'Search User', Icon: BubbleIcon },
    { id: 'groups', label: 'Search Group', Icon: BubbleIcon },
    { id: 'channels', label: 'Search Channel', Icon: BubbleIcon },
]

function ActionBubble({ onSelect }) {
    const [open, setOpen] = useState(false)
    const wrapRef = useRef(null)

    useEffect(() => {
        if (!open) return
        const handlePointer = (e) => {
            if (!wrapRef.current.contains(e.target)) setOpen(false)
        }
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            setOpen(false)
        }
        document.addEventListener('pointerdown', handlePointer)
        window.addEventListener('keydown', handleKeyDown, true)
        return () => {
            document.removeEventListener('pointerdown', handlePointer)
            window.removeEventListener('keydown', handleKeyDown, true)
        }
    }, [open])

    const choose = (id) => {
        setOpen(false)
        onSelect(id)
    }

    return (
        <div className={styles.wrap} ref={wrapRef}>
            {open && (
                <div className={styles.menu} role="menu">
                    {actions.map(({ id, label, Icon }) => (
                        <button key={id} className={styles.item} type="button" role="menuitem" onClick={() => choose(id)}>
                            <span className={styles.itemIcon}><Icon /></span>
                            {label}
                        </button>
                    ))}
                </div>
            )}
            <button
                className={open ? `${styles.button} ${styles.buttonOpen}` : styles.button}
                type="button"
                aria-label="Search or create"
                aria-expanded={open}
                onClick={() => setOpen((value) => !value)}
            >
                <SearchIcon />
            </button>
        </div>
    )
}

export default ActionBubble
