import { useEffect, useRef, useState } from 'react'
import { clearReaction, setPostSwitches, setReaction, VIEW_BATCH, viewPosts } from '../../api/posts'

const isTemp = (id) => id.startsWith('temp-')

export function usePosts(roomId, messages, onError) {
    const [stats, setStats] = useState({})
    const [commentEvent, setCommentEvent] = useState(null)
    const asked = useRef(new Set())

    const store = (list) => {
        setStats((prev) => {
            const next = { ...prev }
            for (const item of list) next[item.id] = item
            return next
        })
    }

    const patch = (messageId, change) => {
        setStats((prev) => (prev[messageId] ? { ...prev, [messageId]: { ...prev[messageId], ...change(prev[messageId]) } } : prev))
    }

    const fetchStats = async (ids) => {
        for (let i = 0; i < ids.length; i += VIEW_BATCH) {
            const batch = ids.slice(i, i + VIEW_BATCH)
            try {
                const { posts } = await viewPosts(roomId, batch)
                store(posts)
            } catch {
                for (const id of batch) asked.current.delete(id)
            }
        }
    }

    useEffect(() => {
        if (!roomId) return
        const ids = messages.filter((m) => !isTemp(m.id) && !m.leaving && !asked.current.has(m.id)).map((m) => m.id)
        if (ids.length === 0) return
        for (const id of ids) asked.current.add(id)
        fetchStats(ids)
    })

    const react = async (messageId, emoji) => {
        const current = stats[messageId]
        if (!current) return
        try {
            const next = current.myReaction === emoji ? await clearReaction(roomId, messageId) : await setReaction(roomId, messageId, emoji)
            store([next])
        } catch (error) {
            onError(error)
        }
    }

    const toggle = async (messageId, key) => {
        const current = stats[messageId]
        if (!current) return
        try {
            store([await setPostSwitches(roomId, messageId, { [key]: !current[key] })])
        } catch (error) {
            onError(error)
        }
    }

    const update = async (messageId, changes) => {
        const current = stats[messageId]
        if (!current) return
        const changed = Object.fromEntries(Object.entries(changes).filter(([key, value]) => current[key] !== value))
        if (Object.keys(changed).length === 0) return
        store([await setPostSwitches(roomId, messageId, changed)])
    }

    const onEvent = (event, data) => {
        if (event === 'post.reactions') {
            patch(data.messageId, () => ({ reactions: data.reactions }))
        } else if (event === 'post.comment' || event === 'post.commentDeleted') {
            patch(data.messageId, () => ({ comments: data.comments }))
            setCommentEvent({ event, data })
        } else if (event === 'post.settings') {
            patch(data.messageId, () => ({ allowReactions: data.allowReactions, allowComments: data.allowComments }))
            if (data.chatId === roomId) fetchStats([data.messageId])
        }
    }

    return { stats, commentEvent, react, toggle, update, onEvent }
}
