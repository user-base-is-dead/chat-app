import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import styles from './PostComments.module.css'
import Avatar from '../Avatar/Avatar'
import BackIcon from '../Icons/BackIcon'
import DeleteIcon from '../Icons/DeleteIcon'
import CopyIcon from '../Icons/CopyIcon'
import ContextMenu from '../ContextMenu/ContextMenu'
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog'
import MessageInput from '../MessageInput/MessageInput'
import PostFooter from '../PostFooter/PostFooter'
import Spinner from '../Spinner/Spinner'
import { addComment, deleteComment, listComments } from '../../api/posts'

const PAGE = 50
const LOAD_OLDER_OFFSET = 80
const MAX_LENGTH = 2000

const formatTime = (ms) => new Date(ms).toLocaleString([], { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false })

function PostComments({ room, post, stats, userId, canModerate, liveEvent, onBack, onReact, onError }) {
    const [comments, setComments] = useState(null)
    const [hasMore, setHasMore] = useState(false)
    const [loadingOlder, setLoadingOlder] = useState(false)
    const [menu, setMenu] = useState(null)
    const [confirm, setConfirm] = useState(null)
    const [handledEvent, setHandledEvent] = useState(liveEvent)
    const listRef = useRef(null)
    const stick = useRef(true)
    const keep = useRef(null)

    useEffect(() => {
        let active = true
        listComments(room.id, post.id)
            .then(({ comments: list }) => {
                if (!active) return
                setComments(list)
                setHasMore(list.length === PAGE)
            })
            .catch((error) => {
                if (active) onError(error)
            })
        return () => {
            active = false
        }
    }, [room.id, post.id])

    if (liveEvent !== handledEvent) {
        setHandledEvent(liveEvent)
        if (liveEvent?.data.messageId === post.id && comments) {
            if (liveEvent.event === 'post.comment') {
                const added = liveEvent.data.comment
                if (!comments.some((c) => c.id === added.id)) setComments([...comments, added])
            } else {
                setComments(comments.filter((c) => c.id !== liveEvent.data.commentId))
            }
        }
    }

    useLayoutEffect(() => {
        const list = listRef.current
        if (!list) return
        if (keep.current !== null) {
            list.scrollTop = list.scrollHeight - keep.current
            keep.current = null
        } else if (stick.current) {
            list.scrollTop = list.scrollHeight
        }
    }, [comments])

    const loadOlder = async () => {
        if (loadingOlder || !hasMore || !comments?.length) return
        setLoadingOlder(true)
        try {
            const { comments: older } = await listComments(room.id, post.id, comments[0].id)
            keep.current = listRef.current.scrollHeight - listRef.current.scrollTop
            setComments((prev) => [...older.filter((c) => !prev.some((p) => p.id === c.id)), ...prev])
            setHasMore(older.length === PAGE)
        } catch (error) {
            onError(error)
        }
        setLoadingOlder(false)
    }

    const handleScroll = () => {
        const list = listRef.current
        stick.current = list.scrollHeight - list.scrollTop - list.clientHeight < LOAD_OLDER_OFFSET
        if (list.scrollTop < LOAD_OLDER_OFFSET) loadOlder()
    }

    const send = async (text) => {
        const body = text.trim()
        if (!body) return
        if ([...body].length > MAX_LENGTH) {
            onError(new Error('A comment can be up to 2000 characters'))
            return
        }
        stick.current = true
        try {
            const added = await addComment(room.id, post.id, body)
            setComments((prev) => (prev && !prev.some((c) => c.id === added.id) ? [...prev, added] : prev))
        } catch (error) {
            onError(error)
        }
    }

    const remove = (comment) => {
        setConfirm({
            title: 'Delete comment?',
            text: 'This comment will be deleted for everyone.',
            action: async () => {
                try {
                    await deleteComment(room.id, post.id, comment.id)
                    setComments((prev) => prev?.filter((c) => c.id !== comment.id) ?? prev)
                } catch (error) {
                    onError(error)
                }
            },
        })
    }

    const openMenu = (e, comment) => {
        e.preventDefault()
        setMenu({ x: e.clientX, y: e.clientY, comment })
    }

    const menuItems = menu ? [
        { label: 'Copy', Icon: CopyIcon, onClick: () => navigator.clipboard?.writeText(menu.comment.text) },
        ...(canModerate || menu.comment.userId === userId ? [{ label: 'Delete', Icon: DeleteIcon, danger: true, onClick: () => remove(menu.comment) }] : []),
    ] : []

    return (
        <div className={styles.screen}>
            <div className={styles.bar}>
                <button className={styles.back} type="button" aria-label="Back" onClick={onBack}>
                    <BackIcon />
                </button>
                <span className={styles.title}>Comments</span>
            </div>
            <div className={styles.list} ref={listRef} onScroll={handleScroll}>
                <div className={styles.post}>
                    <span className={styles.postName}>{room.name}</span>
                    <span className={styles.postText}>{post.text}</span>
                    <PostFooter stats={stats} time={post.time} onReact={onReact} />
                </div>
                {loadingOlder && <div className={styles.center}><Spinner size={18} /></div>}
                {comments === null && <div className={styles.center}><Spinner size={22} /></div>}
                {comments?.length === 0 && <p className={styles.empty}>No comments yet</p>}
                {comments?.map((comment) => {
                    const name = comment.userDeleted ? 'Deleted Account' : comment.userName
                    return (
                        <div key={comment.id} className={styles.comment} onContextMenu={(e) => openMenu(e, comment)}>
                            <Avatar name={name} size={36} deleted={comment.userDeleted} />
                            <div className={styles.body}>
                                <div className={styles.head}>
                                    <span className={styles.name}>{name}</span>
                                    <span className={styles.time}>{formatTime(comment.createdAt)}</span>
                                </div>
                                <span className={styles.text}>{comment.text}</span>
                            </div>
                        </div>
                    )
                })}
            </div>
            <MessageInput onSend={send} onTyping={() => {}} onFocused={() => {}} />
            {menu && <ContextMenu x={menu.x} y={menu.y} items={menuItems} onClose={() => setMenu(null)} />}
            {confirm && (
                <ConfirmDialog
                    title={confirm.title}
                    text={confirm.text}
                    confirmLabel="Yes"
                    onConfirm={confirm.action}
                    onClose={() => setConfirm(null)}
                />
            )}
        </div>
    )
}

export default PostComments
