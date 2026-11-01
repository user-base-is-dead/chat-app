import { useEffect, useRef, useState } from 'react'
import styles from './MessageInput.module.css'
import SendIcon from '../Icons/SendIcon'
import AttachIcon from '../Icons/AttachIcon'
import EmojiIcon from '../Icons/EmojiIcon'
import EmojiPanel from '../EmojiPanel/EmojiPanel'

function MessageInput({ onSend, onTyping }) {
    const [text, setText] = useState('')
    const [emojiOpen, setEmojiOpen] = useState(false)
    const [emojiPinned, setEmojiPinned] = useState(false)
    const inputRef = useRef(null)
    const emojiButtonRef = useRef(null)
    const panelRef = useRef(null)
    const openTimer = useRef(null)
    const closeTimer = useRef(null)

    const closeEmoji = () => {
        clearTimeout(openTimer.current)
        clearTimeout(closeTimer.current)
        setEmojiOpen(false)
        setEmojiPinned(false)
    }

    useEffect(() => {
        if (!emojiOpen) return
        const handlePointerDown = (e) => {
            if (emojiButtonRef.current.contains(e.target)) return
            if (panelRef.current && panelRef.current.contains(e.target)) return
            closeEmoji()
        }
        document.addEventListener('pointerdown', handlePointerDown)
        return () => document.removeEventListener('pointerdown', handlePointerDown)
    }, [emojiOpen])

    useEffect(() => {
        const handleKeyDown = (e) => {
            if (e.ctrlKey || e.metaKey || e.altKey || e.isComposing) return
            if (e.key.length !== 1 || e.key === ' ') return
            const target = e.target
            if (target.closest('input, textarea, select, [contenteditable="true"]')) return
            const input = inputRef.current
            if (!input || !input.offsetParent) return
            input.focus()
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [])

    useEffect(() => () => {
        clearTimeout(openTimer.current)
        clearTimeout(closeTimer.current)
    }, [])

    const handleHoverEnter = (e) => {
        if (e.pointerType !== 'mouse') return
        clearTimeout(closeTimer.current)
        if (!emojiOpen) openTimer.current = setTimeout(() => setEmojiOpen(true), 150)
    }

    const handleHoverLeave = (e) => {
        if (e.pointerType !== 'mouse') return
        clearTimeout(openTimer.current)
        if (!emojiPinned) closeTimer.current = setTimeout(() => setEmojiOpen(false), 300)
    }

    const handleEmojiClick = () => {
        if (emojiPinned) {
            closeEmoji()
            return
        }
        clearTimeout(openTimer.current)
        clearTimeout(closeTimer.current)
        setEmojiOpen(true)
        setEmojiPinned(true)
    }

    const insertEmoji = (emoji) => {
        const input = inputRef.current
        const start = input.selectionStart ?? text.length
        const end = input.selectionEnd ?? text.length
        setText(text.slice(0, start) + emoji + text.slice(end))
        requestAnimationFrame(() => input.setSelectionRange(start + emoji.length, start + emoji.length))
    }

    const deleteBefore = () => {
        const input = inputRef.current
        const start = input.selectionStart ?? text.length
        const end = input.selectionEnd ?? text.length
        if (start !== end) {
            setText(text.slice(0, start) + text.slice(end))
            requestAnimationFrame(() => input.setSelectionRange(start, start))
            return
        }
        if (start === 0) return
        const parts = [...new Intl.Segmenter().segment(text.slice(0, start))]
        const cut = start - parts[parts.length - 1].segment.length
        setText(text.slice(0, cut) + text.slice(start))
        requestAnimationFrame(() => input.setSelectionRange(cut, cut))
    }

    const handleSubmit = (e) => {
        e.preventDefault()
        if (!text.trim()) return
        onSend(text.trim())
        setText('')
    }

    return (
        <div className={styles.wrap}>
            {emojiOpen && (
                <EmojiPanel
                    panelRef={panelRef}
                    onPick={insertEmoji}
                    onBackspace={deleteBefore}
                    onSearchFocus={() => setEmojiPinned(true)}
                    onPointerEnter={handleHoverEnter}
                    onPointerLeave={handleHoverLeave}
                />
            )}
            <form className={styles.bar} onSubmit={handleSubmit}>
                <button className={styles.iconButton} type="button" aria-label="Attach">
                    <AttachIcon />
                </button>
                <input ref={inputRef} className={styles.input} type="text" placeholder="Message" value={text} onChange={(e) => {
                    setText(e.target.value)
                    if (e.target.value.trim()) onTyping?.()
                }} />
                <button
                    ref={emojiButtonRef}
                    className={emojiOpen ? `${styles.emojiButton} ${styles.emojiButtonActive}` : styles.emojiButton}
                    type="button"
                    aria-label="Emoji"
                    aria-expanded={emojiOpen}
                    onClick={handleEmojiClick}
                    onPointerEnter={handleHoverEnter}
                    onPointerLeave={handleHoverLeave}
                >
                    <EmojiIcon />
                </button>
                <button className={styles.sendButton} type="submit" aria-label="Send">
                    <SendIcon />
                </button>
            </form>
        </div>
    )
}

export default MessageInput
