import { useState } from 'react'
import styles from './ChatSelectionBar.module.css'
import ContextMenu from '../ContextMenu/ContextMenu'
import CloseIcon from '../Icons/CloseIcon'
import MoreIcon from '../Icons/MoreIcon'
import SelectIcon from '../Icons/SelectIcon'

const SHOWN = 2

const isShared = (action) => !action.danger && action.label !== 'Copy ID'

function ChatSelectionBar({ chats, total, actionsFor, onClear, onSelectAll }) {
    const [menu, setMenu] = useState(null)
    const sets = chats.map((chat) => actionsFor(chat).filter(isShared))
    const shared = (sets[0] ?? [])
        .filter((action) => sets.every((list) => list.some((item) => item.label === action.label)))
        .map((action) => ({
            ...action,
            onClick: () => {
                sets.forEach((list) => list.find((item) => item.label === action.label).onClick())
                onClear()
            },
        }))
    const single = chats.length === 1 ? actionsFor(chats[0]).filter((action) => !isShared(action)) : []
    const menuItems = [
        chats.length < total && { label: 'Select all', Icon: SelectIcon, onClick: onSelectAll },
        ...shared.slice(SHOWN),
        ...single.map((action) => ({ ...action, onClick: () => { action.onClick(); onClear() } })),
    ].filter(Boolean)

    const openMenu = (e) => {
        const box = e.currentTarget.getBoundingClientRect()
        setMenu({ x: box.right - 224, y: box.bottom + 4 })
    }

    return (
        <div className={styles.bar}>
            <button className={styles.icon} type="button" aria-label="Clear selection" title="Clear selection" onClick={onClear}>
                <CloseIcon />
            </button>
            <span className={styles.count}>{chats.length} selected</span>
            {shared.slice(0, SHOWN).map(({ label, Icon, onClick }) => (
                <button key={label} className={styles.icon} type="button" aria-label={label} title={label} onClick={onClick}>
                    <Icon />
                </button>
            ))}
            {menuItems.length > 0 && (
                <button className={styles.icon} type="button" aria-label="More actions" title="More actions" onClick={openMenu}>
                    <MoreIcon />
                </button>
            )}
            {menu && <ContextMenu x={menu.x} y={menu.y} items={menuItems} onClose={() => setMenu(null)} />}
        </div>
    )
}

export default ChatSelectionBar
