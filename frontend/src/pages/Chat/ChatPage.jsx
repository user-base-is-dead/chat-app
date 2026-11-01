import { useEffect, useRef, useState } from 'react'
import styles from './ChatPage.module.css'
import Sidebar from '../../components/Sidebar/Sidebar'
import ChatHeader from '../../components/ChatHeader/ChatHeader'
import MessageList from '../../components/MessageList/MessageList'
import MessageInput from '../../components/MessageInput/MessageInput'
import ChatWelcome from '../../components/ChatWelcome/ChatWelcome'
import RequestActions from '../../components/RequestActions/RequestActions'
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog'
import UnrestrictBar from '../../components/UnrestrictBar/UnrestrictBar'
import AccountBar from '../../components/AccountBar/AccountBar'
import UserInfo from '../../components/UserInfo/UserInfo'
import Toast from '../../components/Toast/Toast'
import {
    acceptChat,
    deleteChat as removeChat,
    deleteMessage as removeMessage,
    listChats,
    loadMessages,
    loadNewerMessages,
    markRead,
    MESSAGE_PAGE_SIZE,
    messageText,
    muteChat,
    openDm,
    pinChat,
    restrictChat,
    sendMessage,
    unmuteChat,
    unpinChat,
    unrestrictChat,
} from '../../api/chats'
import { connectRealtime } from '../../api/realtime'

const TYPING_SEND_GAP = 3000
const TYPING_SHOW_FOR = 6000
const FOCUS_DURATION = 2000
const TOAST_DURATION = 2000
const SNIPPET_LEAD = 24
const LEAVE_DURATION = 280
const READ_DELAY = 300

const formatTime = (ms) => new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })

const isAfter = (a, b) => BigInt(a) > BigInt(b)

function toChat(chat) {
    return {
        id: chat.id,
        name: chat.peer.deleted ? 'Deleted Account' : chat.peer.displayName,
        username: chat.peer.username,
        peerId: chat.peer.id,
        status: chat.status,
        lastReadId: chat.lastReadId,
        unreadCount: chat.unreadCount ?? 0,
        pinnedAt: chat.pinnedAt,
        pinned: chat.pinnedAt != null,
        muted: chat.muted,
        peerLastReadId: chat.peerLastReadId,
        peerOnline: chat.peer.online,
        peerLastSeenAt: chat.peer.lastSeenAt,
        peerDisabled: chat.peer.disabled,
        peerDeleted: chat.peer.deleted,
        peerBio: chat.peer.bio,
        lastMessage: chat.lastMessage,
    }
}

function toMessage(message, userId, peerLastReadId) {
    const mine = message.senderId === userId
    return {
        id: message.id,
        text: messageText(message),
        time: formatTime(message.createdAt),
        createdAt: message.createdAt,
        mine,
        status: mine ? (isAfter(message.id, peerLastReadId) ? 'sent' : 'read') : undefined,
    }
}

const isTemp = (id) => id.startsWith('temp-')

function compareMessages(a, b) {
    if (isTemp(a.id) || isTemp(b.id)) {
        if (isTemp(a.id) && isTemp(b.id)) return a.createdAt - b.createdAt
        return isTemp(a.id) ? 1 : -1
    }
    return BigInt(a.id) < BigInt(b.id) ? -1 : 1
}

function mergeMessages(existing = [], incoming) {
    const byId = new Map(existing.map((message) => [message.id, message]))
    for (const message of incoming) byId.set(message.id, { ...byId.get(message.id), ...message })
    return [...byId.values()].sort(compareMessages)
}

function snippet(text, index) {
    return index > SNIPPET_LEAD ? `…${text.slice(index - SNIPPET_LEAD + 4)}` : text
}

function ChatPage({ user, onLogout, onUserChange, onAccountGone }) {
    const [chats, setChats] = useState([])
    const [chatsLoading, setChatsLoading] = useState(true)
    const [selectedChatId, setSelectedChatId] = useState(null)
    const [search, setSearch] = useState('')
    const [focus, setFocus] = useState(null)
    const [infoOpen, setInfoOpen] = useState(false)
    const [messagesByChat, setMessagesByChat] = useState({})
    const [confirm, setConfirm] = useState(null)
    const [pagesByChat, setPagesByChat] = useState({})
    const [typingUntil, setTypingUntil] = useState({})
    const [toast, setToast] = useState(null)
    const [searchFocus, setSearchFocus] = useState(0)
    const realtimeRef = useRef(null)
    const realtimeHandlers = useRef(null)
    const lastTypingSent = useRef(0)
    const openChatRef = useRef(null)
    const readTimers = useRef({})
    const seenRef = useRef(null)

    useEffect(() => {
        listChats()
            .then((list) => setChats(list.map(toChat)))
            .catch(() => setChats([]))
            .finally(() => setChatsLoading(false))
    }, [])

    useEffect(() => {
        let firstReady = true
        const connection = connectRealtime({
            onEvent: (event, data) => realtimeHandlers.current?.onEvent(event, data),
            onReady: () => {
                if (firstReady) {
                    firstReady = false
                    return
                }
                realtimeHandlers.current?.onReconnect()
            },
        })
        realtimeRef.current = connection
        return () => connection.close()
    }, [])

    useEffect(() => {
        const handleVisible = () => {
            if (document.visibilityState === 'visible') realtimeHandlers.current?.onVisible()
        }
        document.addEventListener('visibilitychange', handleVisible)
        return () => document.removeEventListener('visibilitychange', handleVisible)
    }, [])

    useEffect(() => {
        if (!focus) return
        const timer = setTimeout(() => setFocus(null), FOCUS_DURATION)
        return () => clearTimeout(timer)
    }, [focus])

    useEffect(() => {
        window.history.replaceState(null, '')
        const handlePopState = () => {
            const state = window.history.state
            setInfoOpen(Boolean(state?.infoOpen))
            if (!state?.chatOpen && !state?.infoOpen) setSelectedChatId(null)
        }
        window.addEventListener('popstate', handlePopState)
        return () => window.removeEventListener('popstate', handlePopState)
    }, [])

    useEffect(() => {
        if (!selectedChatId) return
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') window.history.back()
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [selectedChatId])

    useEffect(() => {
        openChatRef.current = selectedChatId
    }, [selectedChatId])

    useEffect(() => {
        if (!toast) return
        const timer = setTimeout(() => setToast(null), TOAST_DURATION)
        return () => clearTimeout(timer)
    }, [toast])

    const selectedChat = chats.find((chat) => chat.id === selectedChatId) ?? null
    const messages = selectedChatId ? messagesByChat[selectedChatId] || [] : []

    const updateMessage = (chatId, messageId, changes) => {
        setMessagesByChat((prev) => ({
            ...prev,
            [chatId]: (prev[chatId] || []).map((message) => (message.id === messageId ? { ...message, ...changes } : message)),
        }))
    }

    const fetchUnread = async (opened) => {
        const neverRead = opened.lastReadId === '0'
        const [older, newer, all] = await Promise.all([
            neverRead ? [] : loadMessages(opened.id, (BigInt(opened.lastReadId) + 1n).toString()),
            loadNewerMessages(opened.id, opened.lastReadId),
            listChats(),
        ])
        const chat = all.map(toChat).find((c) => c.id === opened.id) ?? opened
        setChats(all.map(toChat))
        setMessagesByChat((prev) => ({
            ...prev,
            [chat.id]: [...older, ...newer].map((m) => toMessage(m, user.id, chat.peerLastReadId)),
        }))
        setPagesByChat((prev) => ({
            ...prev,
            [chat.id]: {
                hasMore: !neverRead && older.length === MESSAGE_PAGE_SIZE,
                loadingOlder: false,
                hasNewer: newer.length === MESSAGE_PAGE_SIZE,
                loadingNewer: false,
                unreadFrom: newer.find((m) => m.senderId !== user.id)?.id ?? null,
            },
        }))
    }

    const fetchLatest = async (opened) => {
        const [list, all] = await Promise.all([loadMessages(opened.id), listChats()])
        const freshChats = all.map(toChat)
        const chat = freshChats.find((c) => c.id === opened.id) ?? opened
        const gap = pagesByChat[chat.id]?.hasNewer
        setChats(freshChats)
        setMessagesByChat((prev) => ({
            ...prev,
            [chat.id]: mergeMessages(gap ? [] : prev[chat.id], list.map((m) => toMessage(m, user.id, chat.peerLastReadId))),
        }))
        setPagesByChat((prev) => ({
            ...prev,
            [chat.id]: {
                hasMore: prev[chat.id] && !gap ? prev[chat.id].hasMore : list.length === MESSAGE_PAGE_SIZE,
                loadingOlder: false,
                hasNewer: false,
                loadingNewer: false,
                unreadFrom: null,
            },
        }))
    }

    const fetchMessages = async (opened) => {
        try {
            if (opened.unreadCount > 0) await fetchUnread(opened)
            else await fetchLatest(opened)
        } catch {
            return
        }
    }

    const openChat = (chat) => {
        if (chat.id === selectedChatId) return
        if (!selectedChatId) window.history.pushState({ chatOpen: true }, '')
        if (chat.unreadCount > 0) setMessagesByChat((prev) => ({ ...prev, [chat.id]: undefined }))
        setSelectedChatId(chat.id)
        fetchMessages(chat)
    }

    const setPage = (chatId, changes) => {
        setPagesByChat((prev) => ({ ...prev, [chatId]: { ...prev[chatId], ...changes } }))
    }

    const loadOlder = async () => {
        const chat = selectedChat
        const page = pagesByChat[chat?.id]
        if (!chat || !page?.hasMore || page.loadingOlder) return
        const oldest = (messagesByChat[chat.id] || []).find((m) => !isTemp(m.id))
        if (!oldest) return

        setPage(chat.id, { loadingOlder: true })
        try {
            const list = await loadMessages(chat.id, oldest.id)
            setMessagesByChat((prev) => ({
                ...prev,
                [chat.id]: mergeMessages(prev[chat.id], list.map((m) => toMessage(m, user.id, chat.peerLastReadId))),
            }))
            setPage(chat.id, { hasMore: list.length === MESSAGE_PAGE_SIZE, loadingOlder: false })
        } catch {
            setPage(chat.id, { loadingOlder: false })
        }
    }

    const loadNewer = async () => {
        const chat = selectedChat
        const page = pagesByChat[chat?.id]
        if (!chat || !page?.hasNewer || page.loadingNewer) return
        const newest = (messagesByChat[chat.id] || []).findLast((m) => !isTemp(m.id))
        if (!newest) return

        setPage(chat.id, { loadingNewer: true })
        try {
            const list = await loadNewerMessages(chat.id, newest.id)
            setMessagesByChat((prev) => ({
                ...prev,
                [chat.id]: mergeMessages(prev[chat.id], list.map((m) => toMessage(m, user.id, chat.peerLastReadId))),
            }))
            setPage(chat.id, { hasNewer: list.length === MESSAGE_PAGE_SIZE, loadingNewer: false })
        } catch {
            setPage(chat.id, { loadingNewer: false })
        }
    }

    const jumpToLatest = async (chat = selectedChat) => {
        if (!chat) return
        try {
            const list = await loadMessages(chat.id)
            setMessagesByChat((prev) => ({ ...prev, [chat.id]: list.map((m) => toMessage(m, user.id, chat.peerLastReadId)) }))
            setPage(chat.id, { hasMore: list.length === MESSAGE_PAGE_SIZE, hasNewer: false, loadingNewer: false })
        } catch {
            return
        }
    }

    const startChat = async (person) => {
        try {
            const chat = toChat(await openDm(person.id))
            setChats((prev) => (prev.some((c) => c.id === chat.id) ? prev.map((c) => (c.id === chat.id ? chat : c)) : [chat, ...prev]))
            openChat(chat)
        } catch {
            return
        }
    }

    const getLastMessage = (chatId) => {
        const loaded = messagesByChat[chatId]
        if (loaded?.length && !pagesByChat[chatId]?.hasNewer) return loaded.at(-1)
        const chat = chats.find((c) => c.id === chatId)
        return chat?.lastMessage ? toMessage(chat.lastMessage, user.id, chat.peerLastReadId) : undefined
    }

    const sortKey = (chat) => getLastMessage(chat.id)?.createdAt ?? 0
    const query = search.trim().toLowerCase()

    const byRecent = (a, b) => sortKey(b) - sortKey(a)
    const byPinned = (a, b) => (b.pinnedAt ?? 0) - (a.pinnedAt ?? 0) || byRecent(a, b)
    const activeChats = chats.filter((chat) => chat.status === 'active').sort(byPinned)
    const requests = chats.filter((chat) => chat.status === 'request')
    const restricted = chats.filter((chat) => chat.status === 'restricted').sort(byRecent)

    const findResults = () => {
        if (!query) return { chats: [], messages: [] }
        const needle = query.replace(/^@/, '')
        const found = [...chats].filter((chat) => !chat.leaving).sort(byRecent)
        const chatMatches = found
            .filter((chat) => chat.name.toLowerCase().includes(needle) || chat.username?.toLowerCase().includes(needle))
            .map((chat) => ({
                chat,
                lastMessage: chat.name.toLowerCase().includes(needle) ? getLastMessage(chat.id) : { text: `@${chat.username}`, time: '' },
            }))
        const messageMatches = found.flatMap((chat) => {
            const loaded = messagesByChat[chat.id]
            const pool = loaded ?? (chat.lastMessage ? [toMessage(chat.lastMessage, user.id, chat.peerLastReadId)] : [])
            return pool
                .filter((message) => !isTemp(message.id) && !message.leaving)
                .map((message) => ({ message, index: message.text.toLowerCase().indexOf(needle) }))
                .filter(({ index }) => index !== -1)
                .map(({ message, index }) => ({ chat, message: { ...message, text: snippet(message.text, index) } }))
        })
        messageMatches.sort((a, b) => b.message.createdAt - a.message.createdAt)
        return { chats: chatMatches, messages: messageMatches }
    }

    const searchResults = findResults()

    const openMessage = (chat, messageId) => {
        openChat(chat)
        setFocus({ chatId: chat.id, messageId })
    }

    const isRequest = selectedChat?.status === 'request'
    const isRestricted = selectedChat?.status === 'restricted'

    const changeStatus = async (chatId, request) => {
        try {
            const chat = toChat(await request(chatId))
            setChats((prev) => prev.map((c) => (c.id === chatId ? chat : c)))
        } catch {
            return
        }
    }

    const acceptRequest = () => changeStatus(selectedChat?.id, acceptChat)
    const restrict = (chatId) => changeStatus(chatId, restrictChat)
    const unrestrict = (chatId) => changeStatus(chatId, unrestrictChat)
    const togglePin = (chat) => changeStatus(chat.id, chat.pinned ? unpinChat : pinChat)
    const toggleMute = (chat) => changeStatus(chat.id, chat.muted ? unmuteChat : muteChat)

    const openInfo = () => {
        if (infoOpen) return
        window.history.pushState({ infoOpen: true }, '')
        setInfoOpen(true)
    }

    const showToast = (text) => setToast({ text, at: Date.now() })

    const focusSearch = () => {
        if (window.matchMedia('(max-width: 768px)').matches) window.history.go(infoOpen ? -2 : -1)
        setSearchFocus((count) => count + 1)
    }

    const closeConfirm = () => setConfirm(null)

    const dropChat = (chatId) => {
        setChats((prev) => prev.map((c) => (c.id === chatId ? { ...c, leaving: true } : c)))
        if (openChatRef.current === chatId) {
            openChatRef.current = null
            window.history.go(window.history.state?.infoOpen ? -2 : -1)
        }
        setTimeout(() => {
            setChats((prev) => prev.filter((c) => c.id !== chatId))
            setMessagesByChat((prev) => {
                const next = { ...prev }
                delete next[chatId]
                return next
            })
        }, LEAVE_DURATION)
    }

    const dropMessage = (chatId, messageId) => {
        const chat = chats.find((c) => c.id === chatId)
        const updateList = (change) =>
            setMessagesByChat((prev) => (prev[chatId] ? { ...prev, [chatId]: change(prev[chatId]) } : prev))
        updateList((list) => list.map((m) => (m.id === messageId ? { ...m, leaving: true } : m)))
        setTimeout(() => {
            updateList((list) => list.filter((m) => m.id !== messageId))
            if (chat?.lastMessage?.id === messageId) refreshChats()
        }, LEAVE_DURATION)
    }

    const deleteChat = (chatId) => {
        setConfirm({
            title: 'Delete chat?',
            text: 'All messages in this chat will be deleted for everyone.',
            action: async () => {
                try {
                    await removeChat(chatId)
                    dropChat(chatId)
                } catch {
                    return
                }
            },
        })
    }

    const deliver = async (chatId, messageId, text) => {
        try {
            const saved = await sendMessage(chatId, text)
            const mapped = { ...toMessage(saved, user.id, '0'), key: messageId }
            setMessagesByChat((prev) => ({
                ...prev,
                [chatId]: (prev[chatId] || [])
                    .filter((m) => m.id !== saved.id)
                    .map((m) => (m.id === messageId ? { ...m, ...mapped } : m)),
            }))
        } catch {
            updateMessage(chatId, messageId, { status: 'failed' })
        }
    }

    const sendTyping = () => {
        const now = Date.now()
        if (!selectedChat || now - lastTypingSent.current < TYPING_SEND_GAP) return
        lastTypingSent.current = now
        realtimeRef.current?.send('typing', { chatId: selectedChat?.id, active: true })
    }

    const readUpTo = (chat, messageId) => {
        if (!isAfter(messageId, chat.lastReadId)) return chat
        const allRead = !chat.lastMessage || !isAfter(chat.lastMessage.id, messageId)
        const nowRead = allRead
            ? chat.unreadCount
            : (messagesByChat[chat.id] || []).filter((m) => !m.mine && !isTemp(m.id) && isAfter(m.id, chat.lastReadId) && !isAfter(m.id, messageId)).length
        return { ...chat, lastReadId: messageId, unreadCount: Math.max(0, chat.unreadCount - nowRead) }
    }

    const markChatRead = (chatId, messageId) => {
        setChats((prev) => prev.map((c) => (c.id === chatId ? readUpTo(c, messageId) : c)))
        clearTimeout(readTimers.current[chatId])
        readTimers.current[chatId] = setTimeout(() => markRead(chatId, messageId).catch(() => {}), READ_DELAY)
    }

    const handleSeen = (messageId) => {
        if (!selectedChat || isTemp(messageId)) return
        seenRef.current = { chatId: selectedChat.id, messageId }
        if (document.visibilityState === 'visible' && isAfter(messageId, selectedChat.lastReadId)) markChatRead(selectedChat.id, messageId)
    }

    const refreshChats = () => {
        listChats()
            .then((list) => setChats(list.map(toChat)))
            .catch(() => {})
    }

    const applyMessage = (data, isNew) => {
        const chat = chats.find((c) => c.id === data.chatId)
        if (!chat) {
            refreshChats()
            return
        }
        if (!chat.lastMessage && data.senderId !== user.id) refreshChats()
        const message = toMessage(data, user.id, chat.peerLastReadId)

        setChats((prev) => prev.map((c) => {
            if (c.id !== data.chatId) return c
            if (c.lastMessage && isAfter(c.lastMessage.id, data.id)) return c
            const fresh = isNew && data.senderId !== user.id && (!c.lastMessage || isAfter(data.id, c.lastMessage.id))
            const unreadCount = fresh ? c.unreadCount + 1 : c.unreadCount
            return { ...c, lastMessage: data, unreadCount }
        }))

        setMessagesByChat((prev) => {
            const list = prev[data.chatId]
            if (!list) return prev
            const exists = list.some((m) => m.id === data.id)
            if (!exists && !isNew) return prev
            if (!exists && pagesByChat[data.chatId]?.hasNewer) return prev
            const pendingCopy = message.mine && list.some((m) => isTemp(m.id) && m.status === 'sending' && m.text === message.text)
            if (!exists && pendingCopy) return prev
            return { ...prev, [data.chatId]: mergeMessages(list, [message]) }
        })
    }

    const handlers = {
        onEvent: (event, data) => {
            if (event === 'message.new') {
                applyMessage(data, true)
                if (data.senderId !== user.id) setTypingUntil((prev) => ({ ...prev, [data.chatId]: 0 }))
            } else if (event === 'message.edited') {
                applyMessage(data, false)
            } else if (event === 'message.deleted') {
                dropMessage(data.chatId, data.id)
            } else if (event === 'chat.deleted') {
                if (chats.some((c) => c.id === data.chatId)) dropChat(data.chatId)
            } else if (event === 'message.read') {
                if (data.userId === user.id) {
                    setChats((prev) => prev.map((c) => (c.id === data.chatId ? readUpTo(c, data.upToMessageId) : c)))
                    return
                }
                setChats((prev) => prev.map((c) => (c.id === data.chatId && isAfter(data.upToMessageId, c.peerLastReadId) ? { ...c, peerLastReadId: data.upToMessageId } : c)))
                setMessagesByChat((prev) => {
                    const list = prev[data.chatId]
                    if (!list) return prev
                    return {
                        ...prev,
                        [data.chatId]: list.map((m) => (m.mine && !isTemp(m.id) && !isAfter(m.id, data.upToMessageId) ? { ...m, status: 'read' } : m)),
                    }
                })
            } else if (event === 'typing') {
                if (data.userId === user.id) return
                setTypingUntil((prev) => ({ ...prev, [data.chatId]: data.active ? Date.now() + TYPING_SHOW_FOR : 0 }))
                setTimeout(() => setTypingUntil((prev) => (prev[data.chatId] && prev[data.chatId] <= Date.now() ? { ...prev, [data.chatId]: 0 } : prev)), TYPING_SHOW_FOR + 100)
            } else if (event === 'chat.changed') {
                refreshChats()
            } else if (event === 'presence') {
                setChats((prev) => prev.map((c) => (c.peerId === data.userId ? { ...c, peerOnline: data.online, peerLastSeenAt: data.lastSeenAt ?? c.peerLastSeenAt } : c)))
            }
        },
        onReconnect: () => {
            if (selectedChat) fetchMessages(selectedChat)
            else refreshChats()
        },
        onVisible: () => {
            const seen = seenRef.current
            if (selectedChat && seen?.chatId === selectedChat.id) handleSeen(seen.messageId)
        },
    }

    useEffect(() => {
        realtimeHandlers.current = handlers
    })

    const handleSend = async (text) => {
        const chatId = selectedChat?.id
        if (pagesByChat[chatId]?.hasNewer) await jumpToLatest()
        lastTypingSent.current = 0
        realtimeRef.current?.send('typing', { chatId, active: false })
        const now = Date.now()
        const tempId = `temp-${now}`
        setMessagesByChat((prev) => ({
            ...prev,
            [chatId]: [...(prev[chatId] || []), { id: tempId, text, time: formatTime(now), createdAt: now, mine: true, status: 'sending' }],
        }))
        deliver(chatId, tempId, text)
    }

    const resendMessage = (messageId) => {
        const chatId = selectedChat?.id
        const message = messages.find((m) => m.id === messageId)
        if (!message) return
        updateMessage(chatId, messageId, { status: 'sending' })
        deliver(chatId, messageId, message.text)
    }

    const deleteMessage = (messageId) => {
        const chatId = selectedChat?.id
        if (!messages.some((m) => m.id === messageId && !m.leaving)) return
        if (isTemp(messageId)) {
            dropMessage(chatId, messageId)
            return
        }
        setConfirm({
            title: 'Delete message?',
            text: 'This message will be deleted for everyone.',
            action: async () => {
                try {
                    const { chatDeleted } = await removeMessage(chatId, messageId)
                    if (chatDeleted) dropChat(chatId)
                    else dropMessage(chatId, messageId)
                } catch {
                    return
                }
            },
        })
    }

    return (
        <div className={[styles.layout, selectedChat && styles.chatOpen, selectedChat && infoOpen && styles.infoOpen].filter(Boolean).join(' ')}>
            <div className={styles.sidebarArea}>
                <Sidebar
                    user={user}
                    onUserChange={onUserChange}
                    onAccountGone={onAccountGone}
                    loading={chatsLoading}
                    chats={activeChats}
                    requests={requests}
                    restricted={restricted}
                    searchResults={searchResults}
                    selectedChatId={selectedChat?.id}
                    getLastMessage={getLastMessage}
                    search={search}
                    searchFocus={searchFocus}
                    onSearchChange={setSearch}
                    onSelectChat={openChat}
                    onOpenMessage={openMessage}
                    onStartChat={startChat}
                    onPinChat={togglePin}
                    onMuteChat={toggleMute}
                    onRestrictChat={restrict}
                    onUnrestrictChat={unrestrict}
                    onDeleteChat={deleteChat}
                    onLogout={onLogout}
                />
            </div>
            <div className={styles.chat}>
                {selectedChat ? (
                    <div className={styles.chatInner}>
                        <ChatHeader
                            chat={selectedChat}
                            typing={(typingUntil[selectedChat.id] ?? 0) > 0}
                            onBack={() => window.history.back()}
                            onOpenInfo={openInfo}
                            onCall={() => showToast('Calls coming soon')}
                            onSearch={focusSearch}
                            onToggleMute={() => toggleMute(selectedChat)}
                            onDelete={() => deleteChat(selectedChat.id)}
                        />
                        {toast && <Toast key={toast.at} text={toast.text} />}
                        <MessageList
                            key={selectedChat.id}
                            messages={messages}
                            loading={!messagesByChat[selectedChat.id]}
                            hasMore={Boolean(pagesByChat[selectedChat.id]?.hasMore)}
                            loadingOlder={Boolean(pagesByChat[selectedChat.id]?.loadingOlder)}
                            hasNewer={Boolean(pagesByChat[selectedChat.id]?.hasNewer)}
                            loadingNewer={Boolean(pagesByChat[selectedChat.id]?.loadingNewer)}
                            unreadFromId={pagesByChat[selectedChat.id]?.unreadFrom ?? null}
                            unreadCount={selectedChat.unreadCount}
                            onLoadOlder={loadOlder}
                            onLoadNewer={loadNewer}
                            onJumpToLatest={jumpToLatest}
                            onSeen={handleSeen}
                            onResend={resendMessage}
                            onDelete={deleteMessage}
                            focusId={focus?.chatId === selectedChat.id ? focus.messageId : null}
                        />
                        {isRequest ? (
                            <RequestActions name={selectedChat.name} onAccept={acceptRequest} onDelete={() => deleteChat(selectedChat?.id)} />
                        ) : isRestricted ? (
                            <UnrestrictBar onUnrestrict={() => unrestrict(selectedChat.id)} />
                        ) : selectedChat.peerDeleted ? (
                            <AccountBar text="This account is deleted" />
                        ) : selectedChat.peerDisabled ? (
                            <AccountBar text="This account is disabled" />
                        ) : (
                            <MessageInput onSend={handleSend} onTyping={sendTyping} />
                        )}
                    </div>
                ) : (
                    <ChatWelcome />
                )}
            </div>
            {selectedChat && (
                <div className={infoOpen ? `${styles.infoArea} ${styles.infoShown}` : styles.infoArea} inert={!infoOpen}>
                    <UserInfo
                        chat={selectedChat}
                        typing={(typingUntil[selectedChat.id] ?? 0) > 0}
                        onClose={() => window.history.back()}
                        onToggleMute={() => toggleMute(selectedChat)}
                        onDelete={() => deleteChat(selectedChat.id)}
                    />
                </div>
            )}
            {confirm && (
                <ConfirmDialog
                    title={confirm.title}
                    text={confirm.text}
                    confirmLabel="Yes"
                    onConfirm={confirm.action}
                    onClose={closeConfirm}
                />
            )}
        </div>
    )
}

export default ChatPage
