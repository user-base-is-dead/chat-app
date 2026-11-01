import { Fragment, useEffect, useLayoutEffect, useRef, useState } from 'react'
import styles from './MessageList.module.css'
import MessageBubble from '../MessageBubble/MessageBubble'
import MessageSkeleton from '../Skeleton/MessageSkeleton'
import Spinner from '../Spinner/Spinner'
import ArrowDownIcon from '../Icons/ArrowDownIcon'
import { formatCount } from '../../utils/formatCount'

const LOAD_OLDER_OFFSET = 120
const JUMP_SHOW_OFFSET = 200
const SEEN_EDGE = 16

function MessageList({ messages, loading, hasMore, loadingOlder, hasNewer, loadingNewer, unreadFromId, unreadCount, onLoadOlder, onLoadNewer, onJumpToLatest, onSeen, onResend, onDelete, focusId }) {
    const listRef = useRef(null)
    const lastSeen = useRef({ first: null, last: null, height: 0, top: 0, hadNewer: false })
    const stickRef = useRef(false)
    const [showJump, setShowJump] = useState(false)

    const reportSeen = () => {
        const list = listRef.current
        const bottom = list.getBoundingClientRect().bottom - SEEN_EDGE
        const bubbles = list.querySelectorAll('[data-message-id]')
        for (let i = bubbles.length - 1; i >= 0; i--) {
            if (bubbles[i].getBoundingClientRect().top < bottom) {
                onSeen(bubbles[i].dataset.messageId)
                return
            }
        }
    }

    const updateJump = () => {
        const list = listRef.current
        setShowJump(hasNewer || list.scrollHeight - list.scrollTop - list.clientHeight > JUMP_SHOW_OFFSET)
    }

    useLayoutEffect(() => {
        const list = listRef.current
        const first = messages[0]?.key ?? messages[0]?.id ?? null
        const last = messages.at(-1)?.key ?? messages.at(-1)?.id ?? null
        const previous = lastSeen.current
        const wasAtBottom = previous.height - previous.top - list.clientHeight < LOAD_OLDER_OFFSET

        if (previous.last === null && last !== null) {
            const line = list.querySelector('[data-unread-line]')
            if (line) list.scrollTop += line.getBoundingClientRect().top - list.getBoundingClientRect().top - parseFloat(getComputedStyle(list).paddingTop)
            else list.scrollTop = list.scrollHeight
        } else if (previous.first && first !== previous.first && last === previous.last) {
            list.scrollTop = list.scrollHeight - previous.height + previous.top
        } else if (last !== previous.last) {
            const sending = messages.at(-1)?.status === 'sending'
            if (stickRef.current || sending || (wasAtBottom && !previous.hadNewer)) list.scrollTop = list.scrollHeight
        }
        stickRef.current = false

        lastSeen.current = { first, last, height: list.scrollHeight, top: list.scrollTop, hadNewer: hasNewer }
        if (last !== null) reportSeen()
        updateJump()
    }, [messages, hasNewer])

    useEffect(() => {
        if (!focusId) return
        const bubble = listRef.current.querySelector(`[data-message-id="${focusId}"]`)
        bubble?.scrollIntoView({ block: 'center' })
    }, [focusId, messages])

    const handleScroll = () => {
        const list = listRef.current
        lastSeen.current.height = list.scrollHeight
        lastSeen.current.top = list.scrollTop
        if (list.scrollTop < LOAD_OLDER_OFFSET && hasMore && !loadingOlder) onLoadOlder()
        if (list.scrollHeight - list.scrollTop - list.clientHeight < LOAD_OLDER_OFFSET && hasNewer && !loadingNewer) onLoadNewer()
        reportSeen()
        updateJump()
    }

    const jumpToBottom = () => {
        const list = listRef.current
        if (hasNewer) {
            stickRef.current = true
            onJumpToLatest()
            return
        }
        const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
        list.scrollTo({ top: list.scrollHeight, behavior: reduced ? 'auto' : 'smooth' })
    }

    return (
        <div className={styles.wrap}>
            {loadingOlder && (
                <div className={styles.older}>
                    <Spinner size={18} />
                </div>
            )}
            <div className={styles.list} ref={listRef} onScroll={handleScroll}>
                {loading && <MessageSkeleton />}
                {messages.map((msg, i) => (
                    <Fragment key={msg.key ?? msg.id}>
                        {msg.id === unreadFromId && <div className={styles.unreadLine} data-unread-line>Unread messages</div>}
                        <MessageBubble
                            message={msg}
                            focused={msg.id === focusId}
                            groupStart={i > 0 && messages[i - 1].mine !== msg.mine}
                            groupEnd={i === messages.length - 1 || messages[i + 1].mine !== msg.mine}
                            onResend={() => onResend(msg.id)}
                            onDelete={() => onDelete(msg.id)}
                        />
                    </Fragment>
                ))}
                {loadingNewer && (
                    <div className={styles.newer}>
                        <Spinner size={18} />
                    </div>
                )}
            </div>
            {showJump && (
                <button className={styles.jump} type="button" aria-label="Scroll to latest" onClick={jumpToBottom}>
                    <ArrowDownIcon />
                    {unreadCount > 0 && <span className={styles.jumpCount}>{formatCount(unreadCount)}</span>}
                </button>
            )}
        </div>
    )
}

export default MessageList
