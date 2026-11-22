import { useCallback, useEffect, useRef, useState } from 'react'
import styles from './ChatPage.module.css'
import Sidebar from '../../components/Sidebar/Sidebar'
import ChatHeader from '../../components/ChatHeader/ChatHeader'
import MessageList from '../../components/MessageList/MessageList'
import MessageInput from '../../components/MessageInput/MessageInput'
import ChatWelcome from '../../components/ChatWelcome/ChatWelcome'
import RequestActions from '../../components/RequestActions/RequestActions'
import ConfirmDialog from '../../components/ConfirmDialog/ConfirmDialog'
import { APP_PLATFORM, checkVersion } from '../../utils/appUpdate'
import UnrestrictBar from '../../components/UnrestrictBar/UnrestrictBar'
import AccountBar from '../../components/AccountBar/AccountBar'
import UserInfo from '../../components/UserInfo/UserInfo'
import RoomInfo from '../../components/RoomInfo/RoomInfo'
import JoinDialog from '../../components/JoinDialog/JoinDialog'
import Toast from '../../components/Toast/Toast'
import { chatStyle } from '../../utils/chroma'
import { animateChange, isPhone, morphChange } from '../../utils/motion'
import { inSidebar } from '../../utils/zone'
import {
    acceptChat,
    deleteChat as removeChat,
    deleteMessage as removeMessage,
    editMessage,
    forwardMessage,
    listChats,
    loadMessages,
    loadMessagesAround,
    loadNewerMessages,
    loadPinned,
    loadPlace,
    markRead,
    MESSAGE_PAGE_SIZE,
    messageText,
    muteChat,
    openDm,
    pinChat,
    pinMessage,
    restrictChat,
    sendMessage,
    unmuteChat,
    unpinChat,
    unpinMessage,
    unrestrictChat,
} from '../../api/chats'
import { connectRealtime } from '../../api/realtime'
import {
    acceptInvite,
    declineInvite,
    deleteRoom,
    getRoom,
    joinRoom,
    leaveRoom,
    listRooms,
    muteRoom,
    pinRoom,
    previewJoin,
    unmuteRoom,
    unpinRoom,
} from '../../api/rooms'
import { joinKey, roomLabel } from '../../utils/roomLink'
import { DEFAULT_SWITCHES } from '../../utils/reactions'
import { usePosts } from './usePosts'
import { listChannels } from '../../api/channels'
import { readOpenChannel, saveOpenChannel } from '../../utils/channelName'
import { readMembersPanel, saveMembersPanel } from '../../utils/membersPanel'
import MemberList from '../../components/MemberList/MemberList'
import { useGroupMembers } from './useGroupMembers'
import { can, colorOf } from '../../utils/roles'
import PostComments from '../../components/PostComments/PostComments'
import PostDialog from '../../components/PostDialog/PostDialog'
import ForwardDialog from '../../components/ForwardDialog/ForwardDialog'
import { InputStrip } from '../../components/MessageExtras/MessageExtras'

const TYPING_SEND_GAP = 3000
const TYPING_SHOW_FOR = 6000
const FOCUS_DURATION = 2000
const TOAST_DURATION = 2000
const SNIPPET_LEAD = 24
const LEAVE_DURATION = 280
const READ_DELAY = 300
const ROOM_REFRESH = 30000

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

function toRoom(room) {
    const chat = {
        id: room.id,
        kind: room.kind,
        name: room.title,
        about: room.about,
        username: room.username,
        isPublic: room.isPublic,
        photo: room.photo,
        inviteCode: room.inviteCode,
        membersCanSend: room.membersCanSend,
        membersCanAdd: room.membersCanAdd,
        status: room.status,
        role: room.role,
        canSend: room.canSend,
        canAdd: room.canAdd,
        permissions: room.permissions ?? [],
        rank: room.rank ?? null,
        lastReadId: room.lastReadId,
        unreadCount: room.unreadCount ?? 0,
        memberCount: room.memberCount,
        pinnedAt: room.pinnedAt,
        pinned: room.pinnedAt != null,
        muted: room.muted,
        peerLastReadId: room.readUpTo,
        lastMessage: room.lastMessage,
    }
    if (room.onlineCount !== undefined) chat.onlineCount = room.onlineCount
    return chat
}

const toAny = (item) => (item.kind ? toRoom(item) : toChat(item))

function asText(group, channel) {
    return {
        ...group,
        id: channel.id,
        roomId: group.id,
        channelName: channel.channelName,
        lastReadId: channel.lastReadId,
        unreadCount: channel.unreadCount,
        peerLastReadId: channel.peerLastReadId,
        lastMessage: channel.lastMessage,
        pinned: false,
        pinnedAt: null,
        leaving: false,
    }
}

const fromChannel = (channel) => ({
    id: channel.id,
    channelName: channel.name,
    lastReadId: channel.lastReadId,
    unreadCount: channel.unreadCount,
    peerLastReadId: channel.readUpTo,
    lastMessage: channel.lastMessage,
})

async function loadAll(previous = [], onResponse) {
    const [chats, rooms] = await Promise.all([listChats(onResponse), listRooms(onResponse)])
    const counts = new Map(previous.map((chat) => [chat.id, chat.onlineCount]))
    const groups = rooms.map((room) => ({ onlineCount: counts.get(room.id), ...toRoom(room) }))
    const byId = new Map(groups.map((group) => [group.id, group]))
    const texts = previous.filter((chat) => chat.roomId && byId.get(chat.roomId)?.status === 'active').map((chat) => asText(byId.get(chat.roomId), chat))
    return [...chats.map(toChat), ...groups, ...texts]
}

const MESSAGE_PATH = /^\/m\/(\d+)\/(\d+)\/?$/

function toMessage(message, userId, peerLastReadId) {
    const mine = message.senderId === userId
    const reply = message.replyTo
    return {
        id: message.id,
        senderId: message.senderId,
        senderName: message.senderName,
        text: messageText(message),
        time: formatTime(message.createdAt),
        createdAt: message.createdAt,
        editedAt: message.editedAt ?? null,
        replyTo: reply ? { id: reply.id, senderId: reply.senderId, senderName: reply.senderName, text: reply.deleted ? '' : messageText(reply), deleted: Boolean(reply.deleted) } : null,
        forwardedFrom: message.forwardedFrom ?? null,
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
    const [membersPanel, setMembersPanel] = useState(readMembersPanel)
    const [membersScreen, setMembersScreen] = useState(false)
    const [presence, setPresence] = useState({})
    const [messagesByChat, setMessagesByChat] = useState({})
    const [confirm, setConfirm] = useState(null)
    const [pagesByChat, setPagesByChat] = useState({})
    const [typingUntil, setTypingUntil] = useState({})
    const [typingNames, setTypingNames] = useState({})
    const [toast, setToast] = useState(null)
    const [searchFocus, setSearchFocus] = useState(0)
    const [inputFocus, setInputFocus] = useState(false)
    const [joining, setJoining] = useState(null)
    const [roomTick, setRoomTick] = useState(0)
    const [commentsFor, setCommentsFor] = useState(null)
    const [postDialog, setPostDialog] = useState(null)
    const [inputClear, setInputClear] = useState(0)
    const [channelsByRoom, setChannelsByRoom] = useState({})
    const [railGroupId, setRailGroupId] = useState(null)
    const [inputMode, setInputMode] = useState(null)
    const [draft, setDraft] = useState(null)
    const [pinnedByChat, setPinnedByChat] = useState({})
    const [forwarding, setForwarding] = useState(null)
    const pendingJump = useRef(null)
    const linkTarget = useRef(window.location.pathname.match(MESSAGE_PATH))
    const realtimeRef = useRef(null)
    const realtimeHandlers = useRef(null)
    const lastTypingSent = useRef(0)
    const openChatRef = useRef(null)
    const readTimers = useRef({})
    const seenRef = useRef(null)

    const [syncStatus, setSyncStatus] = useState('')
    const listRequest = useRef(0)
    const syncList = useCallback(async (previous = []) => {
        const request = ++listRequest.current
        let phase = 'Loading…'
        let slow = false
        const show = () => { if (request === listRequest.current) setSyncStatus(phase) }
        const timer = setTimeout(() => { slow = true; show() }, 250)
        try {
            const result = await loadAll(previous, () => { phase = 'Updating…'; if (slow) show() })
            if (request === listRequest.current) { setChats(result); setSyncStatus('') }
        } catch (error) {
            phase = error.status === 0 ? 'Offline' : 'Sync failed'
            show()
        } finally {
            clearTimeout(timer)
            if (request === listRequest.current) setChatsLoading(false)
        }
    }, [])
    useEffect(() => {
        syncList()
        return () => { listRequest.current++ }
    }, [syncList])

    useEffect(() => {
        const key = joinKey(window.location.pathname)
        if (!key) return
        window.history.replaceState(null, '', '/')
        previewJoin(key)
            .then((room) => setJoining({ key, room }))
            .catch((error) => setJoining({ error: error.message }))
    }, [])

    useEffect(() => {
        let firstReady = true
        const connection = connectRealtime({
            onEvent: (event, data) => {
                if (event === 'app.version') {
                    if (data?.platform === APP_PLATFORM) checkVersion()
                    return
                }
                realtimeHandlers.current?.onEvent(event, data)
            },
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
        if (!selectedChatId) return
        let active = true
        loadPinned(selectedChatId)
            .then(({ message }) => {
                if (active) setPinnedByChat((prev) => ({ ...prev, [selectedChatId]: message ? toMessage(message, user.id, '0') : null }))
            })
            .catch(() => {})
        return () => {
            active = false
        }
    }, [selectedChatId, user.id])

    useEffect(() => {
        window.history.replaceState(null, '')
        const handlePopState = () => {
            const state = window.history.state
            const closing = !state?.chatOpen && !state?.infoOpen && openChatRef.current !== null
            const apply = () => {
                setInfoOpen(Boolean(state?.infoOpen))
                setMembersScreen(Boolean(state?.membersOpen))
                if (!state?.commentsOpen) setCommentsFor(null)
                if (!state?.chatOpen && !state?.infoOpen) setSelectedChatId(null)
            }
            if (closing) animateChange(apply, isPhone() ? 'pop' : 'close')
            else apply()
        }
        window.addEventListener('popstate', handlePopState)
        return () => window.removeEventListener('popstate', handlePopState)
    }, [])

    useEffect(() => {
        if (!selectedChatId) return
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape' || e.defaultPrevented || inSidebar()) return
            window.history.back()
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
    const selectedGroup = selectedChat?.roomId ? chats.find((chat) => chat.id === selectedChat.roomId) ?? selectedChat : selectedChat
    const selectedRoomId = selectedGroup?.kind ? selectedGroup.id : null
    const channelId = selectedChat?.kind === 'channel' && selectedChat.status === 'active' ? selectedChat.id : null

    useEffect(() => {
        if (!selectedRoomId) return
        const load = () => {
            getRoom(selectedRoomId)
                .then((room) => setChats((prev) => prev.map((c) => (c.id === room.id ? { ...c, ...toRoom(room) } : c))))
                .catch(() => {})
        }
        load()
        const timer = setInterval(load, ROOM_REFRESH)
        return () => clearInterval(timer)
    }, [selectedRoomId, roomTick])
    const messages = selectedChatId ? messagesByChat[selectedChatId] || [] : []
    const showToast = (text) => setToast({ text, at: Date.now() })
    const posts = usePosts(channelId, messages, (error) => showToast(error.message))
    const commentsPost = commentsFor && channelId ? messages.find((m) => m.id === commentsFor) : null
    const commentsOff = commentsPost && posts.stats[commentsPost.id] && !posts.stats[commentsPost.id].allowComments

    useEffect(() => {
        if (!commentsFor || (commentsPost && !commentsOff)) return
        if (window.history.state?.commentsOpen) window.history.back()
    }, [commentsFor, commentsPost, commentsOff])

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
            loadAll(chats),
        ])
        const chat = all.find((c) => c.id === opened.id) ?? opened
        setChats(all)
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
        const [list, freshChats] = await Promise.all([loadMessages(opened.id), loadAll(chats)])
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
        if (document.activeElement?.matches('input')) document.activeElement.blur()
        if (chat.id === selectedChatId) return
        if (!selectedChatId) window.history.pushState({ chatOpen: true }, '')
        const source = document.querySelector(`[data-chat-id="${chat.id}"] [data-avatar]`)
        morphChange(source, () => {
            if (chat.unreadCount > 0) setMessagesByChat((prev) => ({ ...prev, [chat.id]: undefined }))
            setSelectedChatId(chat.id)
        }, isPhone() ? 'push' : 'open')
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
            setInputFocus(!['request', 'restricted'].includes(chat.status) && !chat.peerDeleted && !chat.peerDisabled)
        } catch {
            return
        }
    }

    const clearInputFocus = useCallback(() => setInputFocus(false), [])

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
    const people = chats.filter((chat) => !chat.kind)
    const rooms = chats.filter((chat) => chat.kind && !chat.roomId).sort(byPinned)
    const groups = rooms.filter((room) => room.kind === 'group')
    const railGroup = groups.find((room) => room.id === railGroupId)
        ?? (selectedGroup?.kind === 'group' ? groups.find((room) => room.id === selectedGroup.id) : null)
        ?? groups.find((room) => room.status === 'active')
        ?? groups[0]
        ?? null
    const railNeedsList = railGroup?.status === 'active' && !channelsByRoom[railGroup.id] ? railGroup.id : null
    const channelLabel = selectedChat?.kind === 'group'
        ? selectedChat.channelName ?? channelsByRoom[selectedChat.id]?.channels.find((channel) => channel.id === selectedChat.id)?.name ?? 'general'
        : null
    const groupOpen = selectedGroup?.kind === 'group' && selectedGroup.status === 'active'
    const groupData = useGroupMembers(groupOpen ? selectedGroup.id : null, groupOpen ? selectedChat.id : null, roomTick)
    const channelPermissions = selectedChat?.kind === 'group'
        ? channelsByRoom[selectedGroup?.id]?.channels.find((channel) => channel.id === selectedChat.id)?.permissions ?? null
        : null
    const sendBlocked = Boolean(channelPermissions && !channelPermissions.includes('sendMessages'))
    const nameColors = groupData ? Object.fromEntries(groupData.members.map((member) => [member.id, colorOf(member.roleIds, groupData.roles)])) : null
    const ownerId = groupData?.members.find((member) => member.role === 'owner')?.id ?? null

    const toggleMembers = () => {
        if (isPhone()) {
            window.history.pushState({ chatOpen: true, membersOpen: true }, '')
            setMembersScreen(true)
            return
        }
        setMembersPanel((open) => {
            saveMembersPanel(!open)
            return !open
        })
    }

    const activeChats = people.filter((chat) => chat.status === 'active').sort(byPinned)
    const requests = people.filter((chat) => chat.status === 'request')
    const restricted = people.filter((chat) => chat.status === 'restricted').sort(byRecent)
    const knownPeople = activeChats
        .filter((chat) => !chat.peerDeleted && !chat.peerDisabled)
        .map((chat) => ({ id: chat.peerId, displayName: chat.name, username: chat.username }))

    const findResults = () => {
        if (!query) return { chats: [], messages: [] }
        const needle = query.replace(/^@/, '')
        const found = [...chats].filter((chat) => !chat.leaving && !(chat.kind && chat.status === 'request')).sort(byRecent)
        const chatMatches = found
            .filter((chat) => !chat.roomId && (chat.name.toLowerCase().includes(needle) || chat.username?.toLowerCase().includes(needle)))
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
            const chat = toAny(await request(chatId))
            setChats((prev) => prev.map((c) => (c.id === chatId ? { ...c, ...chat } : c)))
        } catch {
            return
        }
    }

    const acceptRequest = () => changeStatus(selectedChat?.id, acceptChat)
    const restrict = (chatId) => changeStatus(chatId, restrictChat)
    const unrestrict = (chatId) => changeStatus(chatId, unrestrictChat)
    const togglePin = (chat) => {
        const pin = chat.kind ? pinRoom : pinChat
        const unpin = chat.kind ? unpinRoom : unpinChat
        changeStatus(chat.id, chat.pinned ? unpin : pin)
    }
    const toggleMute = (chat) => {
        const mute = chat.kind ? muteRoom : muteChat
        const unmute = chat.kind ? unmuteRoom : unmuteChat
        changeStatus(chat.id, chat.muted ? unmute : mute)
    }

    const storeChannels = (roomId, list) => {
        setChannelsByRoom((prev) => ({ ...prev, [roomId]: list }))
        setChats((prev) => {
            const group = prev.find((chat) => chat.id === roomId)
            if (!group) return prev
            const texts = list.channels.filter((channel) => channel.id !== roomId).map((channel) => asText(group, fromChannel(channel)))
            return [...prev.filter((chat) => chat.roomId !== roomId), ...texts]
        })
    }

    const loadChannels = async (roomId) => {
        try {
            const list = await listChannels(roomId)
            storeChannels(roomId, list)
            return list
        } catch {
            return null
        }
    }

    const openChannel = (room, channel) => {
        saveOpenChannel(room.id, channel.id)
        if (channel.id === room.id) openChat(room)
        else openChat(chats.find((chat) => chat.id === channel.id) ?? asText(room, fromChannel(channel)))
    }

    const selectGroup = async (room) => {
        setRailGroupId(room.id)
        if (room.status !== 'active') return
        const list = channelsByRoom[room.id] ?? await loadChannels(room.id)
        if (!list || isPhone()) return
        const saved = readOpenChannel(room.id)
        const target = list.channels.find((channel) => channel.id === saved) ?? list.channels[0]
        if (target) openChannel(room, target)
    }

    const openGroupInfo = (room) => {
        if (selectedGroup?.id !== room.id) openChat(room)
        openInfo()
    }

    useEffect(() => {
        if (!railNeedsList) return
        let active = true
        listChannels(railNeedsList)
            .then((list) => {
                if (active) storeChannels(railNeedsList, list)
            })
            .catch(() => {})
        return () => {
            active = false
        }
    }, [railNeedsList])

    const addRoom = (room) => {
        setChats((prev) => (prev.some((c) => c.id === room.id) ? prev.map((c) => (c.id === room.id ? { ...c, ...room } : c)) : [room, ...prev]))
    }

    const roomCreated = (result) => {
        const room = toRoom(result)
        addRoom(room)
        openChat(room)
        if (result.skipped > 0) showToast(`${result.skipped} could not be added`)
        setInputFocus(room.canSend)
    }

    const openPreview = (preview) => {
        const known = chats.find((c) => c.id === preview.id && c.status === 'active')
        if (known) openChat(known)
        else setJoining({ key: preview.username, room: preview })
    }

    const openKey = (key) => {
        previewJoin(key)
            .then((room) => {
                const known = chats.find((c) => c.id === room.id && c.status === 'active')
                if (known) openChat(known)
                else setJoining({ key, room })
            })
            .catch((error) => setJoining({ error: error.message }))
    }

    const confirmJoin = async () => {
        const room = toRoom(await joinRoom(joining.key))
        addRoom(room)
        setJoining(null)
        openChat(room)
    }

    const closeJoin = useCallback(() => setJoining(null), [])

    const acceptRoom = async (room) => {
        try {
            addRoom(toRoom(await acceptInvite(room.id)))
        } catch {
            return
        }
    }

    const declineRoom = async (room) => {
        try {
            await declineInvite(room.id)
            dropChat(room.id)
        } catch {
            return
        }
    }

    const leaveChat = (room) => {
        setConfirm({
            title: `Leave ${roomLabel(room.kind).toLowerCase()}?`,
            text: `You will leave ${room.name}.`,
            action: async () => {
                try {
                    await leaveRoom(room.id)
                    dropChat(room.id)
                } catch (error) {
                    showToast(error.message)
                }
            },
        })
    }

    const deleteRoomChat = (room) => {
        setConfirm({
            title: `Delete ${roomLabel(room.kind).toLowerCase()}?`,
            text: 'All messages will be deleted for everyone.',
            action: async () => {
                try {
                    await deleteRoom(room.id)
                    dropChat(room.id)
                } catch (error) {
                    showToast(error.message)
                }
            },
        })
    }

    const openInfo = () => {
        if (infoOpen) {
            window.history.back()
            return
        }
        window.history.pushState({ infoOpen: true }, '')
        setInfoOpen(true)
    }

    const focusSearch = () => {
        if (window.matchMedia('(max-width: 768px)').matches) window.history.go(infoOpen ? -2 : -1)
        setSearchFocus((count) => count + 1)
    }

    const closeConfirm = () => setConfirm(null)

    const dropChat = (chatId) => {
        setChats((prev) => prev.map((c) => (c.id === chatId ? { ...c, leaving: true } : c)))
        if (openChatRef.current === chatId || chats.find((c) => c.id === openChatRef.current)?.roomId === chatId) {
            openChatRef.current = null
            window.history.go(window.history.state?.infoOpen ? -2 : -1)
        }
        setTimeout(() => {
            setChats((prev) => prev.filter((c) => c.id !== chatId && c.roomId !== chatId))
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

    const deliver = async (chatId, messageId, text, options, replyToId) => {
        try {
            const saved = await sendMessage(chatId, text, options, replyToId)
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

    const applyRead = (chatId, messageId) => {
        const target = chats.find((c) => c.id === chatId)
        const roomKey = target?.roomId ?? (target?.kind === 'group' ? target.id : null)
        const entry = roomKey ? channelsByRoom[roomKey]?.channels.find((channel) => channel.id === chatId) : null
        if (!target || !entry) {
            setChats((prev) => prev.map((c) => (c.id === chatId ? readUpTo(c, messageId) : c)))
            return
        }
        const own = { ...target, unreadCount: entry.unreadCount }
        const after = readUpTo(own, messageId)
        const cleared = own.unreadCount - after.unreadCount
        setChannelsByRoom((prev) => ({
            ...prev,
            [roomKey]: { ...prev[roomKey], channels: prev[roomKey].channels.map((channel) => (channel.id === chatId ? { ...channel, unreadCount: after.unreadCount, lastReadId: after.lastReadId } : channel)) },
        }))
        setChats((prev) => {
            const current = prev.find((c) => c.id === chatId)
            if (!current || !isAfter(after.lastReadId, current.lastReadId)) return prev
            return prev.map((c) => {
                if (c.id === chatId) return target.roomId ? { ...c, lastReadId: after.lastReadId, unreadCount: after.unreadCount } : { ...c, lastReadId: after.lastReadId, unreadCount: Math.max(0, c.unreadCount - cleared) }
                if (target.roomId && c.id === roomKey) return { ...c, unreadCount: Math.max(0, c.unreadCount - cleared) }
                return c
            })
        })
    }

    const noteMessage = (data) => {
        const fromOther = data.senderId !== user.id
        const roomKey = data.roomId ?? data.chatId
        if (fromOther && channelsByRoom[roomKey]) {
            setChannelsByRoom((prev) => ({
                ...prev,
                [roomKey]: { ...prev[roomKey], channels: prev[roomKey].channels.map((channel) => (channel.id === data.chatId ? { ...channel, unreadCount: channel.unreadCount + 1 } : channel)) },
            }))
        }
        if (!data.roomId || !chats.some((c) => c.id === data.chatId)) return
        setChats((prev) => prev.map((c) => (c.id === data.roomId ? { ...c, lastMessage: data, unreadCount: c.unreadCount + (fromOther ? 1 : 0) } : c)))
    }

    const channelsChanged = async (roomId) => {
        const list = await loadChannels(roomId)
        if (!list || selectedGroup?.id !== roomId || list.channels.some((channel) => channel.id === selectedChat.id)) return
        const group = chats.find((c) => c.id === roomId)
        if (!group) return
        showToast('You no longer have access to this channel')
        if (list.channels[0]) openChannel(group, list.channels[0])
        else setSelectedChatId(null)
    }

    const markChatRead = (chatId, messageId) => {
        applyRead(chatId, messageId)
        clearTimeout(readTimers.current[chatId])
        readTimers.current[chatId] = setTimeout(() => markRead(chatId, messageId).catch(() => {}), READ_DELAY)
    }

    const handleSeen = (messageId) => {
        if (!selectedChat || isTemp(messageId)) return
        seenRef.current = { chatId: selectedChat.id, messageId }
        if (document.visibilityState === 'visible' && isAfter(messageId, selectedChat.lastReadId)) markChatRead(selectedChat.id, messageId)
    }

    const refreshChats = () => {
        syncList(chats)
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
            if (event.startsWith('post.')) {
                posts.onEvent(event, data)
            } else if (event === 'message.new') {
                applyMessage(data, true)
                noteMessage(data)
                if (data.senderId !== user.id) setTypingUntil((prev) => ({ ...prev, [data.chatId]: 0 }))
            } else if (event === 'message.edited') {
                applyMessage(data, false)
                const text = messageText(data)
                setPinnedByChat((prev) => (prev[data.chatId]?.id === data.id ? { ...prev, [data.chatId]: { ...prev[data.chatId], text } } : prev))
                setMessagesByChat((prev) => (prev[data.chatId]
                    ? { ...prev, [data.chatId]: prev[data.chatId].map((m) => (m.replyTo?.id === data.id ? { ...m, replyTo: { ...m.replyTo, text } } : m)) }
                    : prev))
            } else if (event === 'message.pinned') {
                setPinnedByChat((prev) => ({ ...prev, [data.chatId]: data.message ? toMessage(data.message, user.id, '0') : null }))
            } else if (event === 'message.deleted') {
                dropMessage(data.chatId, data.id)
                setPinnedByChat((prev) => (prev[data.chatId]?.id === data.id ? { ...prev, [data.chatId]: null } : prev))
                setMessagesByChat((prev) => (prev[data.chatId]
                    ? { ...prev, [data.chatId]: prev[data.chatId].map((m) => (m.replyTo?.id === data.id ? { ...m, replyTo: { ...m.replyTo, text: '', deleted: true } } : m)) }
                    : prev))
            } else if (event === 'chat.deleted') {
                if (chats.some((c) => c.id === data.chatId)) dropChat(data.chatId)
            } else if (event === 'room.removed') {
                if (chats.some((c) => c.id === data.chatId && !c.leaving)) dropChat(data.chatId)
            } else if (event === 'room.channels') {
                channelsChanged(data.chatId)
            } else if (event === 'room.changed') {
                refreshChats()
                setRoomTick((tick) => tick + 1)
            } else if (event === 'message.read') {
                if (data.userId === user.id) {
                    applyRead(data.chatId, data.upToMessageId)
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
                if (data.name) setTypingNames((prev) => ({ ...prev, [data.chatId]: data.name }))
                setTimeout(() => setTypingUntil((prev) => (prev[data.chatId] && prev[data.chatId] <= Date.now() ? { ...prev, [data.chatId]: 0 } : prev)), TYPING_SHOW_FOR + 100)
            } else if (event === 'chat.changed') {
                refreshChats()
            } else if (event === 'presence') {
                setChats((prev) => prev.map((c) => (c.peerId === data.userId ? { ...c, peerOnline: data.online, peerLastSeenAt: data.hidden ? null : data.lastSeenAt ?? c.peerLastSeenAt } : c)))
                setPresence((prev) => ({ ...prev, [data.userId]: { online: Boolean(data.online), hidden: Boolean(data.hidden), at: Date.now() } }))
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

    const mode = inputMode?.chatId === selectedChatId ? inputMode : null

    const nameOf = (message) => {
        if (message.mine) return user.displayName || user.username
        if (selectedChat?.kind === 'channel') return selectedChat.name
        return message.senderName || (selectedChat?.kind ? 'Deleted Account' : selectedChat?.name)
    }

    const saveMessageEdit = async (message, text) => {
        const chatId = selectedChat.id
        updateMessage(chatId, message.id, { text, editedAt: Date.now() })
        try {
            applyMessage(await editMessage(chatId, message.id, text), false)
        } catch (error) {
            updateMessage(chatId, message.id, { text: message.text, editedAt: message.editedAt })
            showToast(error.message)
        }
    }

    const handleSend = (text) => {
        if (mode?.kind === 'edit') {
            setInputMode(null)
            if (text !== mode.message.text) saveMessageEdit(mode.message, text)
            return
        }
        if (selectedChat?.id === channelId) {
            setPostDialog({ text, switches: DEFAULT_SWITCHES })
            return false
        }
        const replyTo = mode?.kind === 'reply'
            ? { id: mode.message.id, senderId: mode.message.senderId, senderName: nameOf(mode.message), text: mode.message.text, deleted: false }
            : null
        setInputMode(null)
        postMessage(text, undefined, replyTo)
    }

    const closeMode = () => {
        if (mode?.kind === 'edit') setInputClear((tick) => tick + 1)
        setInputMode(null)
    }

    const jumpTo = async (chatId, messageId) => {
        if ((messagesByChat[chatId] || []).some((m) => m.id === messageId)) {
            setFocus({ chatId, messageId })
            return
        }
        try {
            const [older, newer] = await loadMessagesAround(chatId, messageId)
            if (!older.some((m) => m.id === messageId)) {
                showToast('Message not found')
                return
            }
            const chat = chats.find((c) => c.id === chatId)
            setMessagesByChat((prev) => ({ ...prev, [chatId]: mergeMessages([], [...older, ...newer].map((m) => toMessage(m, user.id, chat?.peerLastReadId ?? '0'))) }))
            setPage(chatId, { hasMore: older.length === MESSAGE_PAGE_SIZE, hasNewer: newer.length === MESSAGE_PAGE_SIZE, loadingOlder: false, loadingNewer: false, unreadFrom: null })
            setFocus({ chatId, messageId })
        } catch (error) {
            showToast(error.message)
        }
    }

    useEffect(() => {
        const pending = pendingJump.current
        if (!pending || !messagesByChat[pending.chatId]) return
        pendingJump.current = null
        jumpTo(pending.chatId, pending.messageId)
    })

    const openLink = async (chatId, messageId) => {
        try {
            const place = await loadPlace(chatId)
            if (place.kind === 'text') {
                const group = chats.find((c) => c.id === place.roomId)
                const list = group && (channelsByRoom[group.id] ?? await loadChannels(group.id))
                const channel = list?.channels.find((item) => item.id === chatId)
                if (!channel) throw new Error('Message not found')
                setRailGroupId(group.id)
                openChannel(group, channel)
            } else {
                const chat = chats.find((c) => c.id === chatId)
                if (!chat) throw new Error('Message not found')
                openChat(chat)
            }
            pendingJump.current = { chatId, messageId }
        } catch {
            showToast('Message not found')
        }
    }

    useEffect(() => {
        if (chatsLoading || !linkTarget.current) return
        const [, chatId, messageId] = linkTarget.current
        linkTarget.current = null
        window.history.replaceState(null, '', '/')
        openLink(chatId, messageId)
    })

    const toggleMessagePin = async (message) => {
        const chatId = selectedChatId
        const current = pinnedByChat[chatId] ?? null
        const unpinning = current?.id === message.id
        setPinnedByChat((prev) => ({ ...prev, [chatId]: unpinning ? null : message }))
        try {
            if (unpinning) await unpinMessage(chatId)
            else await pinMessage(chatId, message.id)
        } catch (error) {
            setPinnedByChat((prev) => ({ ...prev, [chatId]: current }))
            showToast(error.message)
        }
    }

    const forwardTo = async (target) => {
        const list = forwarding.filter((m) => !isTemp(m.id)).sort(compareMessages)
        const fromChatId = selectedChatId
        setForwarding(null)
        try {
            for (const message of list) await forwardMessage(target.id, fromChatId, message.id)
            showToast(`Forwarded to ${target.name}`)
        } catch (error) {
            showToast(error.message)
        }
    }

    const broadcast = (text, switches) => {
        setInputClear((tick) => tick + 1)
        postMessage(text, switches)
    }

    const editPost = (message) => {
        const stats = posts.stats[message.id]
        if (!stats) return
        setPostDialog({
            message,
            text: message.text,
            switches: { allowComments: stats.allowComments, allowReactions: stats.allowReactions },
        })
    }

    const saveEdit = async (message, text, switches) => {
        try {
            if (text !== message.text) applyMessage(await editMessage(channelId, message.id, text), false)
            await posts.update(message.id, switches)
        } catch (error) {
            showToast(error.message)
            return false
        }
    }

    const postMessage = async (text, switches, replyTo) => {
        const chatId = selectedChat?.id
        if (pagesByChat[chatId]?.hasNewer) await jumpToLatest()
        lastTypingSent.current = 0
        realtimeRef.current?.send('typing', { chatId, active: false })
        const now = Date.now()
        const tempId = `temp-${now}`
        setMessagesByChat((prev) => ({
            ...prev,
            [chatId]: [...(prev[chatId] || []), { id: tempId, senderId: user.id, text, time: formatTime(now), createdAt: now, mine: true, status: 'sending', replyTo: replyTo ?? null }],
        }))
        deliver(chatId, tempId, text, switches, replyTo?.id)
    }

    const openComments = (messageId) => {
        window.history.pushState({ chatOpen: true, commentsOpen: true }, '')
        setCommentsFor(messageId)
    }

    const resendMessage = (messageId) => {
        const chatId = selectedChat?.id
        const message = messages.find((m) => m.id === messageId)
        if (!message) return
        updateMessage(chatId, messageId, { status: 'sending' })
        deliver(chatId, messageId, message.text, undefined, message.replyTo?.id)
    }

    const deleteMessages = (messageIds) => {
        const chatId = selectedChat?.id
        const live = messageIds.filter((id) => messages.some((m) => m.id === id && !m.leaving))
        if (live.length === 0) return
        live.filter(isTemp).forEach((id) => dropMessage(chatId, id))
        const sent = live.filter((id) => !isTemp(id))
        if (sent.length === 0) return
        setConfirm({
            title: sent.length === 1 ? 'Delete message?' : `Delete ${sent.length} messages?`,
            text: sent.length === 1 ? 'This message will be deleted for everyone.' : 'These messages will be deleted for everyone.',
            action: async () => {
                for (const id of sent) {
                    try {
                        const { chatDeleted } = await removeMessage(chatId, id)
                        if (chatDeleted) {
                            dropChat(chatId)
                            return
                        }
                        dropMessage(chatId, id)
                    } catch {
                        continue
                    }
                }
            },
        })
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

    const chatHeader = selectedChat && (
        <ChatHeader
            chat={selectedGroup}
            channelName={channelLabel}
            typing={(typingUntil[selectedChat.id] ?? 0) > 0}
            typingName={typingNames[selectedChat.id]}
            onBack={() => window.history.go(window.history.state?.infoOpen ? -2 : -1)}
            onOpenInfo={openInfo}
            onCall={() => showToast('Calls coming soon')}
            onSearch={focusSearch}
            onToggleMute={() => toggleMute(selectedGroup)}
            onLeave={() => leaveChat(selectedGroup)}
            onDelete={() => (selectedGroup.kind ? deleteRoomChat(selectedGroup) : deleteChat(selectedGroup.id))}
            onMembers={groupOpen ? toggleMembers : undefined}
            membersOpen={groupOpen && membersPanel}
        />
    )

    const canType = Boolean(selectedChat) && !sendBlocked && !(selectedChat.kind && selectedChat.kind !== 'group' && !selectedChat.canSend)
        && !isRequest && !isRestricted && !selectedChat.peerDeleted && !selectedChat.peerDisabled
    const canPinHere = selectedChat?.kind
        ? (channelPermissions ? channelPermissions.includes('deleteMessages') : can(selectedChat, 'deleteMessages'))
        : canType
    const pinned = selectedChatId ? pinnedByChat[selectedChatId] ?? null : null
    const modeName = !mode ? '' : mode.message.mine
        ? user.displayName || user.username
        : selectedChat?.kind === 'channel' ? selectedChat.name : mode.message.senderName || (selectedChat?.kind ? 'Deleted Account' : selectedChat?.name)
    const messageActions = {
        canReply: canType && !channelId,
        canEdit: canType && !channelId,
        canPin: canPinHere,
        pinnedId: pinned?.id ?? null,
        reply: (message) => {
            if (mode?.kind === 'edit') setInputClear((tick) => tick + 1)
            setInputMode({ kind: 'reply', message, chatId: selectedChatId })
            setInputFocus(true)
        },
        edit: (message) => {
            setInputMode({ kind: 'edit', message, chatId: selectedChatId })
            setDraft((current) => ({ text: message.text, key: (current?.key ?? 0) + 1 }))
            setInputFocus(true)
        },
        copyLink: (message) => {
            const link = `${window.location.origin}/m/${selectedChatId}/${message.id}`
            const copied = navigator.clipboard?.writeText(link)
            if (!copied) {
                showToast('Could not copy the link')
                return
            }
            copied.then(() => showToast('Link copied')).catch(() => showToast('Could not copy the link'))
        },
        pin: toggleMessagePin,
        forward: (list) => setForwarding(list),
        openReply: (messageId) => jumpTo(selectedChatId, messageId),
    }

    return (
        <div className={[styles.layout, selectedChat && styles.chatOpen, selectedChat && infoOpen && styles.infoOpen].filter(Boolean).join(' ')}>
            <div className={styles.sidebarArea} data-zone="sidebar">
                <Sidebar
                    user={user}
                    onUserChange={onUserChange}
                    onAccountGone={onAccountGone}
                    loading={chatsLoading}
                    syncStatus={syncStatus}
                    onRetry={refreshChats}
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
                    rooms={rooms}
                    knownPeople={knownPeople}
                    roomPinnedCount={chats.filter((chat) => chat.pinned).length}
                    railGroup={railGroup}
                    channelList={railGroup ? channelsByRoom[railGroup.id] : null}
                    onSelectGroup={selectGroup}
                    onOpenChannel={openChannel}
                    onOpenGroupInfo={openGroupInfo}
                    onChannelsChanged={loadChannels}
                    onNotice={showToast}
                    onRoomCreated={roomCreated}
                    onOpenRoom={openPreview}
                    onOpenKey={openKey}
                    onLeaveRoom={leaveChat}
                    onDeleteRoom={deleteRoomChat}
                    onAcceptInvite={acceptRoom}
                    onDeclineInvite={declineRoom}
                />
            </div>
            <div className={styles.chat} style={selectedChat ? chatStyle(selectedChat) : undefined}>
                {selectedChat ? (
                    <>
                    <div className={styles.chatInner}>
                        {commentsPost && !commentsOff && chatHeader}
                        {toast && <Toast key={toast.at} text={toast.text} />}
                        {postDialog && (
                            <PostDialog
                                title={postDialog.message ? 'Edit post' : 'New broadcast'}
                                text={postDialog.text}
                                switches={postDialog.switches}
                                actionLabel={postDialog.message ? 'Save' : 'Broadcast'}
                                onSubmit={(text, switches) => (postDialog.message ? saveEdit(postDialog.message, text, switches) : broadcast(text, switches))}
                                onClose={() => setPostDialog(null)}
                            />
                        )}
                        {commentsPost && !commentsOff ? (
                            <PostComments
                                key={commentsPost.id}
                                room={selectedChat}
                                post={commentsPost}
                                stats={posts.stats[commentsPost.id]}
                                userId={user.id}
                                canModerate={selectedChat.role === 'owner' || selectedChat.role === 'admin'}
                                liveEvent={posts.commentEvent}
                                onBack={() => window.history.back()}
                                onReact={(emoji) => posts.react(commentsPost.id, emoji)}
                                onError={(error) => showToast(error.message)}
                            />
                        ) : (
                        <>
                        <MessageList
                            header={chatHeader}
                            pinned={pinned}
                            canUnpin={canPinHere}
                            onOpenPinned={(messageId) => jumpTo(selectedChatId, messageId)}
                            onUnpin={toggleMessagePin}
                            actions={messageActions}
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
                            onDeleteMany={deleteMessages}
                            focusId={focus?.chatId === selectedChat.id ? focus.messageId : null}
                            showSenders={selectedChat.kind === 'group'}
                            flat={selectedChat.kind !== 'channel'}
                            peerName={selectedChat.kind ? '' : selectedChat.name}
                            peerPhoto={selectedChat.kind ? '' : selectedChat.photo}
                            selfName={user.displayName || user.username}
                            hideTicks={selectedChat.kind === 'channel'}
                            canDeleteOthers={!selectedChat.kind || (channelPermissions ? channelPermissions.includes('deleteMessages') : can(selectedChat, 'deleteMessages'))}
                            nameColors={nameColors}
                            ownerId={selectedChat.role === 'owner' ? null : ownerId}
                            channel={channelId ? selectedChat : null}
                            postStats={posts.stats}
                            canManagePosts={selectedChat.role === 'owner'}
                            onReact={posts.react}
                            onOpenComments={openComments}
                            onToggleSwitch={posts.toggle}
                            onEditPost={editPost}
                        />
                        {sendBlocked ? (
                            <AccountBar text="You cannot send messages in this channel" />
                        ) : selectedChat.kind && selectedChat.kind !== 'group' && !selectedChat.canSend ? (
                            selectedChat.kind === 'channel' ? (
                                <UnrestrictBar label={selectedChat.muted ? 'Unmute' : 'Mute'} onUnrestrict={() => toggleMute(selectedChat)} />
                            ) : (
                                <AccountBar text="Only admins can send messages" />
                            )
                        ) : isRequest ? (
                            <RequestActions name={selectedChat.name} onAccept={acceptRequest} onDelete={() => deleteChat(selectedChat?.id)} />
                        ) : isRestricted ? (
                            <UnrestrictBar onUnrestrict={() => unrestrict(selectedChat.id)} />
                        ) : selectedChat.peerDeleted ? (
                            <AccountBar text="This account is deleted" />
                        ) : selectedChat.peerDisabled ? (
                            <AccountBar text="This account is disabled" />
                        ) : (
                            <>
                                <MessageInput
                                    placeholder={channelId ? 'Broadcast' : 'Message'}
                                    clearKey={inputClear}
                                    onSend={handleSend}
                                    onTyping={sendTyping}
                                    focusNow={inputFocus}
                                    onFocused={clearInputFocus}
                                    strip={mode ? <InputStrip mode={mode} name={modeName} onClose={closeMode} /> : null}
                                    draft={draft}
                                />
                            </>
                        )}
                        </>
                        )}
                    </div>
                    {groupOpen && ((membersPanel && !infoOpen) || membersScreen) && (
                        <MemberList
                            group={selectedGroup}
                            userId={user.id}
                            data={groupData}
                            presence={presence}
                            screen={membersScreen}
                            onClose={membersScreen ? () => window.history.back() : toggleMembers}
                        />
                    )}
                    </>
                ) : (
                    <ChatWelcome name={user.displayName || user.username} />
                )}
            </div>
            {selectedChat && (
                <div className={infoOpen ? `${styles.infoArea} ${styles.infoShown}` : styles.infoArea} inert={!infoOpen}>
                    {selectedChat.kind ? (
                        <RoomInfo
                            key={selectedGroup.id}
                            chat={selectedGroup}
                            open={infoOpen}
                            refreshKey={roomTick}
                            typingName={(typingUntil[selectedChat.id] ?? 0) > 0 ? typingNames[selectedChat.id] : null}
                            known={knownPeople}
                            userId={user.id}
                            onClose={() => window.history.back()}
                            onChanged={(room) => addRoom(toRoom(room))}
                            onToggleMute={() => toggleMute(selectedChat)}
                            onLeave={() => leaveChat(selectedChat)}
                            onDelete={() => deleteRoomChat(selectedChat)}
                            onConfirm={setConfirm}
                            onToast={showToast}
                        />
                    ) : (
                        <UserInfo
                            chat={selectedChat}
                            typing={(typingUntil[selectedChat.id] ?? 0) > 0}
                            onClose={() => window.history.back()}
                            onToggleMute={() => toggleMute(selectedChat)}
                            onRestrict={() => restrict(selectedChat.id)}
                            onUnrestrict={() => unrestrict(selectedChat.id)}
                            onDelete={() => deleteChat(selectedChat.id)}
                        />
                    )}
                </div>
            )}
            {joining && <JoinDialog room={joining.room} error={joining.error} onJoin={confirmJoin} onClose={closeJoin} />}
            {forwarding && (
                <ForwardDialog
                    count={forwarding.length}
                    people={chats.filter((c) => !c.kind && c.status === 'active' && !c.peerDeleted && !c.peerDisabled && !c.leaving)}
                    groups={chats.filter((c) => c.kind === 'group' && !c.roomId && c.status === 'active')}
                    channels={chats.filter((c) => c.kind === 'channel' && c.status === 'active' && c.canSend)}
                    channelsByRoom={channelsByRoom}
                    loadChannels={loadChannels}
                    onPick={forwardTo}
                    onClose={() => setForwarding(null)}
                />
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
