import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import styles from './ContextMenu.module.css'

function ContextMenu({ x, y, title, items, onClose }) {
    const menuRef = useRef(null)
    const [position, setPosition] = useState({ left: x, top: y })

    useLayoutEffect(() => {
        const { width, height } = menuRef.current.getBoundingClientRect()
        const left = Math.max(8, Math.min(x, window.innerWidth - width - 8))
        const top = y + height > window.innerHeight - 8 ? Math.max(8, y - height) : y
        setPosition({ left, top })
    }, [x, y])

    useEffect(() => {
        const handlePointerDown = (e) => {
            if (!menuRef.current.contains(e.target)) onClose()
        }
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape') return
            e.stopPropagation()
            onClose()
        }
        document.addEventListener('pointerdown', handlePointerDown, true)
        window.addEventListener('keydown', handleKeyDown, true)
        window.addEventListener('resize', onClose)
        return () => {
            document.removeEventListener('pointerdown', handlePointerDown, true)
            window.removeEventListener('keydown', handleKeyDown, true)
            window.removeEventListener('resize', onClose)
        }
    }, [onClose])

    return createPortal(
        <div ref={menuRef} className={styles.menu} style={position} role="menu" onClick={(e) => e.stopPropagation()} onContextMenu={(e) => { e.preventDefault(); e.stopPropagation() }}>
            {title && <p className={styles.title}>{title}</p>}
            {items.map(({ label, Icon, danger, onClick }) => (
                <button
                    key={label}
                    className={danger ? `${styles.item} ${styles.danger}` : styles.item}
                    type="button"
                    role="menuitem"
                    onClick={() => { onClose(); onClick?.() }}
                >
                    <Icon />
                    {label}
                </button>
            ))}
        </div>,
        document.body,
    )
}

export default ContextMenu
