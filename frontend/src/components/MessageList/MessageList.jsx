import { Fragment, useEffect, useLayoutEffect, useRef, useState } from 'react'
import CloseIcon from '../Icons/CloseIcon'
import styles from './MessageList.module.css'
import MessageBubble from '../MessageBubble/MessageBubble'
import GroupMessage from '../GroupMessage/GroupMessage'
import { dayKey, dayLabel } from '../../utils/dayLabel'
import MessageSkeleton from '../Skeleton/MessageSkeleton'
import Spinner from '../Spinner/Spinner'
import ArrowDownIcon from '../Icons/ArrowDownIcon'
import { formatCount } from '../../utils/formatCount'

const LOAD_OLDER_OFFSET = 120
const JUMP_SHOW_OFFSET = 200
const SEEN_EDGE = 16

function MessageList({ messages, loading, hasMore, loadingOlder, hasNewer, loadingNewer, unreadFromId, unreadCount, onLoadOlder, onLoadNewer, onJumpToLatest, onSeen, onResend, onDelete, focusId, showSenders, hideTicks, canDeleteOthers = true, channel, postStats, canManagePosts, onReact, onOpenComments, onToggleSwitch, onEditPost, onDeleteMany, flat, selfName, peerName, peerPhoto, nameColors, ownerId }) {
    const sameSide = (a, b) => (channel ? true : showSenders ? a.senderId === b.senderId : a.mine === b.mine)
    const RUN_GAP = 5 * 60 * 1000
    const joins = (a, b) => Boolean(a && b) && (a.mine ? b.mine : !b.mine && a.senderId === b.senderId)
        && b.createdAt - a.createdAt <= RUN_GAP && dayKey(a.createdAt) === dayKey(b.createdAt)
    const listRef = useRef(null)
    const lastSeen = useRef({ first: null, last: null, height: 0, top: 0, hadNewer: false })
    const stickRef = useRef(false)
    const [showJump, setShowJump] = useState(false)
    const [selected, setSelected] = useState(() => new Set())
    const selecting = selected.size > 0
    const drag = useRef(null)
    const skipClick = useRef(false)

    const canDelete = (msg) => msg.mine || (canDeleteOthers && (!flat || !ownerId || msg.senderId !== ownerId))
    const clearSelection = () => setSelected(new Set())
    const toggle = (id) => setSelected((current) => {
        const next = new Set(current)
        if (next.has(id)) next.delete(id)
        else next.add(id)
        return next
    })

    const idAt = (x, y) => document.elementFromPoint(x, y)?.closest('[data-message-id]')?.dataset.messageId ?? null
    const range = (a, b) => {
        const from = messages.findIndex((m) => String(m.id) === a)
        const to = messages.findIndex((m) => String(m.id) === b)
        if (from < 0 || to < 0) return []
        return messages.slice(Math.min(from, to), Math.max(from, to) + 1).map((m) => String(m.id))
    }
    const begin = (anchor) => {
        drag.current = { ...drag.current, active: true, anchor, base: new Set(selected) }
        window.getSelection()?.removeAllRanges()
        setSelected(new Set([...selected, anchor]))
    }

    const handlePointerDown = (e) => {
        if (e.button !== 0 || e.target.closest('button, a, input, textarea')) return
        const id = idAt(e.clientX, e.clientY)
        if (!id) return
        const state = { id, x: e.clientX, y: e.clientY, touch: e.pointerType !== 'mouse', active: false }
        if (state.touch) state.timer = setTimeout(() => begin(id), 450)
        drag.current = state
    }

    const handlePointerMove = (e) => {
        const state = drag.current
        if (!state) return
        if (!state.active) {
            if (state.touch) {
                if (Math.hypot(e.clientX - state.x, e.clientY - state.y) > 10) {
                    clearTimeout(state.timer)
                    drag.current = null
                }
                return
            }
            const id = idAt(e.clientX, e.clientY)
            if (id && id !== state.id) begin(state.id)
            else return
        }
        e.preventDefault()
        const id = idAt(e.clientX, e.clientY)
        if (id) setSelected(new Set([...drag.current.base, ...range(drag.current.anchor, id)]))
    }

    const handlePointerUp = () => {
        const state = drag.current
        if (state?.timer) clearTimeout(state.timer)
        if (state?.active) skipClick.current = true
        drag.current = null
    }

    const handleClickCapture = (e) => {
        if (skipClick.current) {
            skipClick.current = false
            e.preventDefault()
            e.stopPropagation()
            return
        }
        if (!selecting) return
        const id = e.target.closest('[data-message-id]')?.dataset.messageId
        if (!id) return
        e.preventDefault()
        e.stopPropagation()
        toggle(id)
    }

    useEffect(() => {
        const list = listRef.current
        const stopScroll = (e) => {
            if (drag.current?.active) e.preventDefault()
        }
        list.addEventListener('touchmove', stopScroll, { passive: false })
        return () => list.removeEventListener('touchmove', stopScroll)
    }, [])

    useEffect(() => {
        if (!selecting) return
        const onKey = (e) => {
            if (e.key !== 'Escape') return
            e.stopImmediatePropagation()
            clearSelection()
        }
        window.addEventListener('keydown', onKey, true)
        return () => window.removeEventListener('keydown', onKey, true)
    }, [selecting])

    const chosen = messages.filter((m) => selected.has(String(m.id)))
    const copyChosen = () => {
        navigator.clipboard?.writeText(chosen.map((m) => m.text).join('\n'))
        clearSelection()
    }
    const deleteChosen = () => {
        onDeleteMany?.(chosen.map((m) => m.id))
        clearSelection()
    }

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

    const markStuck = () => {
        const list = listRef.current
        const top = list.getBoundingClientRect().top + parseFloat(getComputedStyle(list).paddingTop)
        list.querySelectorAll('[data-day-line]').forEach((line) => {
            line.toggleAttribute('data-stuck', line.getBoundingClientRect().top - top <= parseFloat(getComputedStyle(line).top) + 2)
        })
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
        markStuck()
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
        markStuck()
    }

    const jumpToBottom = () => {
        const list = listRef.current
        if (hasNewer) {
            stickRef.current = true
            onJumpToLatest()
            return
        }
        list.scrollTo({ top: list.scrollHeight })
    }

    return (
        <div className={styles.wrap}>
            {selecting && (
                <div className={styles.selectBar}>
                    <button className={styles.selectClose} type="button" aria-label="Cancel selection" onClick={clearSelection}>
                        <CloseIcon />
                    </button>
                    <span className={styles.selectCount} key={chosen.length}>{chosen.length === 1 ? '1 selected' : `${chosen.length} selected`}</span>
                    <button className={styles.selectAction} type="button" onClick={copyChosen}>Copy</button>
                    {chosen.length > 0 && chosen.every(canDelete) && (
                        <button className={`${styles.selectAction} ${styles.selectDanger}`} type="button" onClick={deleteChosen}>Delete</button>
                    )}
                </div>
            )}
            {loadingOlder && (
                <div className={styles.older}>
                    <Spinner size={18} />
                </div>
            )}
            <div
                className={[styles.list, flat && styles.flat, selecting && styles.selecting].filter(Boolean).join(' ')}
                ref={listRef}
                onScroll={handleScroll}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerCancel={handlePointerUp}
                onClickCapture={handleClickCapture}
            >
                {loading && <MessageSkeleton />}
                {messages.map((msg, i) => (
                    <Fragment key={msg.key ?? msg.id}>
                        {msg.id === unreadFromId && <div className={styles.unreadLine} data-unread-line>Unread messages</div>}
                        {flat && (i === 0 || dayKey(messages[i - 1].createdAt) !== dayKey(msg.createdAt)) && (
                            <div className={styles.dayLine} data-day-line><span>{dayLabel(msg.createdAt)}</span></div>
                        )}
                        {flat ? (
                            <GroupMessage
                                message={msg}
                                start={!joins(messages[i - 1], msg) || msg.id === unreadFromId}
                                focused={msg.id === focusId}
                                selfName={selfName}
                                peerName={peerName}
                                peerPhoto={peerPhoto}
                                nameColor={nameColors?.[msg.senderId] ?? null}
                                canDeleteOthers={canDeleteOthers && (!ownerId || msg.senderId !== ownerId)}
                                selecting={selecting}
                                selected={selected.has(String(msg.id))}
                                onSelect={() => setSelected(new Set([...selected, String(msg.id)]))}
                                onResend={() => onResend(msg.id)}
                                onDelete={() => onDelete(msg.id)}
                            />
                        ) : (
                            <MessageBubble
                                message={msg}
                                focused={msg.id === focusId}
                                groupStart={i > 0 && (Boolean(channel) || !sameSide(messages[i - 1], msg))}
                                groupEnd={Boolean(channel) || i === messages.length - 1 || !sameSide(messages[i + 1], msg)}
                                showSender={Boolean(channel) || (showSenders && !msg.mine)}
                                firstOfSender={Boolean(channel) || i === 0 || !sameSide(messages[i - 1], msg)}
                                channel={channel}
                                stats={postStats?.[msg.id]}
                                canManagePosts={canManagePosts}
                                onReact={(emoji) => onReact(msg.id, emoji)}
                                onOpenComments={() => onOpenComments(msg.id)}
                                onToggleSwitch={(key) => onToggleSwitch(msg.id, key)}
                                onEditPost={() => onEditPost(msg)}
                                hideTicks={hideTicks}
                                canDeleteOthers={canDeleteOthers}
                                selecting={selecting}
                                selected={selected.has(String(msg.id))}
                                onSelect={() => setSelected(new Set([...selected, String(msg.id)]))}
                                onResend={() => onResend(msg.id)}
                                onDelete={() => onDelete(msg.id)}
                            />
                        )}
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
