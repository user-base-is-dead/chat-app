import { useEffect, useRef, useState } from 'react'
import { inSidebar } from '../../utils/zone'
import styles from './Sidebar.module.css'
import SearchIcon from '../Icons/SearchIcon'
import CloseIcon from '../Icons/CloseIcon'
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog'
import { AlertButtons, AlertsPage } from '../AppAlerts/AppAlerts'
import ChatListItem from '../ChatListItem/ChatListItem'
import ChatSelectionBar from '../ChatSelectionBar/ChatSelectionBar'
import ChatListSkeleton from '../Skeleton/ChatListSkeleton'
import BottomTabs, { MoreMenu } from '../BottomTabs/BottomTabs'
import RequestsIcon from '../Icons/RequestsIcon'
import BackIcon from '../Icons/BackIcon'
import GroupsPage from '../../pages/Groups/GroupsPage'
import ChannelsPage from '../../pages/Channels/ChannelsPage'
import SettingsPage from '../../pages/Settings/SettingsPage'
import ActionBubble from '../ActionBubble/ActionBubble'
import FindPanel from '../FindPanel/FindPanel'
import RoomCreate from '../RoomCreate/RoomCreate'
import RoomList from '../RoomList/RoomList'
import GroupRail from '../GroupRail/GroupRail'
import ChannelList from '../ChannelList/ChannelList'
import Avatar from '../Avatar/Avatar'
import { formatCount } from '../../utils/formatCount'
import { can } from '../../utils/roles'
import { chatActions } from '../../utils/chatActions'

const folders = [{ id: 'all', label: 'General' }]
const PIN_LIMIT = 5
const tabKinds = { groups: 'group', channels: 'channel' }
const titles = { chats: 'Chats', groups: 'Groups', channels: 'Channels', settings: 'Settings' }
const RECENT_KEY = 'recentSearches'
const RECENT_LIMIT = 15

function loadRecent() {
    try {
        return JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
    } catch {
        return []
    }
}

function saveRecent(ids) {
    try {
        localStorage.setItem(RECENT_KEY, JSON.stringify(ids.slice(0, RECENT_LIMIT)))
    } catch {
        return
    }
}

function Sidebar({ user, onUserChange, onAccountGone, loading, syncStatus, onRetry, chats, requests, restricted, searchResults, selectedChatId, getLastMessage, search, searchFocus, onSearchChange, onSelectChat, onOpenMessage, onStartChat, onPinChat, onMuteChat, onRestrictChat, onUnrestrictChat, onDeleteChat, onLogout, rooms, knownPeople, roomPinnedCount, onRoomCreated, onOpenRoom, onOpenKey, onLeaveRoom, onDeleteRoom, onAcceptInvite, onDeclineInvite, railGroup, channelList, onSelectGroup, onOpenChannel, onOpenGroupInfo, onChannelsChanged, onNotice }) {
    const [activeTab, setActiveTab] = useState('chats')
    const [activeFolder, setActiveFolder] = useState('all')
    const [requestsOpen, setRequestsOpen] = useState(false)
    const [requestsTab, setRequestsTab] = useState('requests')
    const [alertsOpen, setAlertsOpen] = useState(false)
    const [searchOpen, setSearchOpen] = useState(false)
    const [recentIds, setRecentIds] = useState(loadRecent)
    const [askClear, setAskClear] = useState(false)
    const [findKind, setFindKind] = useState(null)
    const [createKind, setCreateKind] = useState(null)
    const [selection, setSelection] = useState({ key: '', ids: [] })
    const searchRef = useRef(null)
    const wantSearch = useRef(false)

    useEffect(() => {
        if (!searchFocus) return
        wantSearch.current = true
        setActiveTab('chats')
        setSearchOpen(true)
    }, [searchFocus])

    useEffect(() => {
        if (!wantSearch.current || !searchRef.current?.offsetParent) return
        wantSearch.current = false
        searchRef.current.focus()
    })

    useEffect(() => {
        setAlertsOpen(false)
        setSearchOpen(false)
    }, [activeTab])

    const selectionKey = [activeTab, requestsOpen, searchOpen, findKind, createKind].join('|')
    const selectedIds = selection.key === selectionKey ? selection.ids.filter((id) => chats.some((chat) => chat.id === id)) : []
    const setSelectedIds = (update) => setSelection((current) => {
        const ids = current.key === selectionKey ? current.ids : []
        return { key: selectionKey, ids: typeof update === 'function' ? update(ids) : update }
    })
    const selecting = selectedIds.length > 0

    useEffect(() => {
        if (!selecting) return
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape' || e.defaultPrevented) return
            e.preventDefault()
            e.stopPropagation()
            setSelection({ key: '', ids: [] })
        }
        document.addEventListener('keydown', handleKeyDown, true)
        return () => document.removeEventListener('keydown', handleKeyDown, true)
    }, [selecting])

    useEffect(() => {
        const handlePopState = () => {
            setRequestsOpen(Boolean(window.history.state?.requestsOpen))
            setFindKind(window.history.state?.findKind ?? null)
            setCreateKind(window.history.state?.createKind ?? null)
        }
        window.addEventListener('popstate', handlePopState)
        return () => window.removeEventListener('popstate', handlePopState)
    }, [])

    useEffect(() => {
        const searching = searchOpen && activeTab === 'chats' && !requestsOpen
        if (!(requestsOpen || findKind || createKind || searching)) return
        const handleKeyDown = (e) => {
            if (e.key !== 'Escape' || e.defaultPrevented) return
            if (selectedChatId && !inSidebar()) return
            e.preventDefault()
            if (searching) {
                setSearchOpen(false)
                onSearchChange('')
            } else {
                window.history.back()
            }
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [requestsOpen, findKind, createKind, selectedChatId, searchOpen, activeTab])

    const openRequests = () => {
        window.history.pushState({ requestsOpen: true }, '')
        setRequestsTab('requests')
        setRequestsOpen(true)
    }

    const openFind = (kind) => {
        if (kind.startsWith('new-')) {
            const nextKind = kind.replace('new-', '')
            window.history.pushState({ createKind: nextKind }, '')
            setCreateKind(nextKind)
            return
        }
        window.history.pushState({ findKind: kind }, '')
        setFindKind(kind)
    }

    const finishCreate = (room) => {
        window.history.replaceState(null, '')
        setCreateKind(null)
        setActiveTab(room.kind === 'channel' ? 'channels' : 'groups')
        onRoomCreated(room)
    }

    const searching = search.trim().length > 0
    const covered = findKind || createKind
    const tabKind = tabKinds[activeTab]
    const tabRooms = rooms.filter((room) => room.kind === tabKind)
    const tabResults = searchResults.chats.filter(({ chat }) => chat.kind === tabKind)
    const tabMessages = searchResults.messages.filter(({ chat }) => chat.kind === tabKind)
    const pinnedCount = chats.filter((chat) => chat.pinned).length
    const actionsFor = (chat) => chatActions(chat, {
        onPin: chat.pinned || pinnedCount < PIN_LIMIT ? () => onPinChat(chat) : undefined,
        onMute: () => onMuteChat(chat),
        onRestrict: () => onRestrictChat(chat.id),
        onDelete: () => onDeleteChat(chat.id),
    })
    const selectedChats = chats.filter((chat) => selectedIds.includes(chat.id))
    const selectChat = (chat) => setSelectedIds((ids) => (ids.includes(chat.id) ? ids : [...ids, chat.id]))
    const toggleChat = (chat) => setSelectedIds((ids) => (ids.includes(chat.id) ? ids.filter((id) => id !== chat.id) : [...ids, chat.id]))
    const unreadChats = chats.filter((chat) => chat.unreadCount > 0 && !chat.muted).length

    const inRequests = activeTab === 'chats' && requestsOpen
    const title = inRequests ? 'New & Restricted' : titles[activeTab]
    const fullSearch = searchOpen && activeTab === 'chats' && !requestsOpen
    const known = [...chats, ...requests, ...restricted, ...rooms]
    const pageHeader = !inRequests && activeTab !== 'settings'
    const retry = syncStatus === 'Offline' || syncStatus === 'Sync failed'
    const recentChats = recentIds.map((id) => known.find((chat) => chat.id === id)).filter(Boolean)

    const remember = (chat) => {
        const next = [chat.id, ...recentIds.filter((id) => id !== chat.id)]
        setRecentIds(next)
        saveRecent(next)
    }
    const forget = (chat) => {
        const next = recentIds.filter((id) => id !== chat.id)
        setRecentIds(next)
        saveRecent(next)
    }
    const forgetAll = () => {
        setRecentIds([])
        saveRecent([])
    }
    const openSearch = () => {
        setSearchOpen(true)
        wantSearch.current = true
    }
    const closeSearch = () => {
        setSearchOpen(false)
        onSearchChange('')
    }
    const pickChat = (chat) => {
        remember(chat)
        onSelectChat(chat)
    }
    const pickMessage = (chat, messageId) => {
        remember(chat)
        onOpenMessage(chat, messageId)
    }

    return (
        <div className={styles.sidebar}>
            <BottomTabs user={user} activeTab={activeTab} onChange={setActiveTab} />
            <div className={styles.panel}>
                {alertsOpen && <AlertsPage onBack={() => setAlertsOpen(false)} />}
                {fullSearch && (
                    <div className={styles.searchPage}>
                        <div className={styles.searchTop}>
                            <button className={styles.requestsBack} type="button" aria-label="Back" onClick={closeSearch}>
                                <BackIcon />
                            </button>
                            <label className={`${styles.searchBox} ${styles.searchBoxWide}`}>
                                <span className={styles.searchIcon}><SearchIcon /></span>
                                <input
                                    ref={searchRef}
                                    className={styles.search}
                                    type="text"
                                    placeholder="Search people and messages"
                                    value={search}
                                    onChange={(e) => onSearchChange(e.target.value)}
                                />
                                {search && (
                                    <button className={styles.searchClear} type="button" aria-label="Clear" onClick={() => onSearchChange('')}>
                                        <CloseIcon />
                                    </button>
                                )}
                            </label>
                        </div>
                        {!searching && recentChats.length === 0 && <div className={styles.noResults}>Search for people, groups and messages</div>}
                        {!searching && recentChats.length > 0 && (
                            <div className={styles.list}>
                                <div className={styles.recentHead}>
                                    <span>Recent</span>
                                    <button className={styles.recentClear} type="button" onClick={() => setAskClear(true)}>Clear all</button>
                                </div>
                                {recentChats.map((chat) => (
                                    <div key={chat.id} className={styles.recentRow}>
                                        <button className={styles.recentOpen} type="button" onClick={() => pickChat(chat)}>
                                            <Avatar name={chat.name} photo={chat.photo} size={44} />
                                            <span className={styles.recentText}>
                                                <span className={styles.recentName}>{chat.name}</span>
                                                {chat.username && <span className={styles.recentUser}>@{chat.username}</span>}
                                            </span>
                                        </button>
                                        <button className={styles.recentRemove} type="button" aria-label={`Remove ${chat.name}`} onClick={() => forget(chat)}>
                                            <CloseIcon />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                        {searching && (
                            <div className={styles.list}>
                                {searchResults.chats.length === 0 && searchResults.messages.length === 0 && <div className={styles.noResults}>No results</div>}
                                {searchResults.chats.length > 0 && <div className={styles.sectionTitle}>Chats</div>}
                                {searchResults.chats.map(({ chat, lastMessage }, index) => (
                                    <ChatListItem key={chat.id} chat={chat} index={index} lastMessage={lastMessage} active={chat.id === selectedChatId} query={search} onClick={() => pickChat(chat)} />
                                ))}
                                {searchResults.messages.length > 0 && <div className={styles.sectionTitle}>Messages</div>}
                                {searchResults.messages.map(({ chat, message }, index) => (
                                    <ChatListItem key={`${chat.id}-${message.id}`} chat={chat} index={index} lastMessage={message} query={search} onClick={() => pickMessage(chat, message.id)} />
                                ))}
                            </div>
                        )}
                    </div>
                )}
                {askClear && (
                    <ConfirmDialog
                        title="Clear recent searches?"
                        text="All recent searches will be removed from this browser."
                        confirmLabel="Clear all"
                        onConfirm={forgetAll}
                        onClose={() => setAskClear(false)}
                    />
                )}
                {!covered && !fullSearch && (
                    <div className={styles.header}>
                        <div className={styles.titleWrap}>
                        <div className={selectedChats.length > 0 ? `${styles.titleRow} ${styles.titleRowHidden}` : styles.titleRow}>
                            {inRequests && (
                                <button className={styles.requestsBack} type="button" aria-label="Back" onClick={() => window.history.back()}>
                                    <BackIcon />
                                </button>
                            )}
                            {pageHeader && <MoreMenu className={styles.mobileMore} />}
                            <h1 key={title} className={inRequests ? `${styles.title} ${styles.titleSmall}` : pageHeader ? styles.centerTitle : styles.title}>{pageHeader && syncStatus && !retry ? syncStatus : title}</h1>
                            {!inRequests && activeTab !== 'settings' && <AlertButtons onOpen={() => setAlertsOpen(true)} />}
                        </div>
                        {selectedChats.length > 0 && (
                            <ChatSelectionBar
                                chats={selectedChats}
                                total={chats.length}
                                actionsFor={actionsFor}
                                onClear={() => setSelectedIds([])}
                                onSelectAll={() => setSelectedIds(chats.map((chat) => chat.id))}
                            />
                        )}
                        </div>
                        {activeTab === 'chats' && !inRequests && (
                            <button className={styles.searchFake} type="button" onClick={openSearch}>
                                <span className={styles.searchFakeIcon}><SearchIcon /></span>
                                Search your chats
                            </button>
                        )}
                        {activeTab !== 'settings' && activeTab !== 'chats' && (
                            <label className={styles.searchBox}>
                                <span className={styles.searchIcon}><SearchIcon /></span>
                                <input ref={searchRef} className={styles.search} type="text" placeholder={`Search your ${titles[activeTab].toLowerCase()}`} value={search} onChange={(e) => onSearchChange(e.target.value)} />
                            </label>
                        )}
                    </div>
                )}
                {findKind && <FindPanel kind={findKind} onStartChat={onStartChat} onOpenRoom={onOpenRoom} onOpenKey={onOpenKey} />}
                {createKind && <RoomCreate kind={createKind} known={knownPeople} onCreated={finishCreate} />}
                {false && (
                    <div className={styles.list}>
                        {searchResults.chats.length === 0 && searchResults.messages.length === 0 && <div className={styles.noResults}>No results</div>}
                        {searchResults.chats.length > 0 && <div className={styles.sectionTitle}>Chats</div>}
                        {searchResults.chats.map(({ chat, lastMessage }, index) => (
                            <ChatListItem
                                key={chat.id}
                                chat={chat}
                                index={index}
                                lastMessage={lastMessage}
                                active={chat.id === selectedChatId}
                                query={search}
                                onClick={() => onSelectChat(chat)}
                            />
                        ))}
                        {searchResults.messages.length > 0 && <div className={styles.sectionTitle}>Messages</div>}
                        {searchResults.messages.map(({ chat, message }, index) => (
                            <ChatListItem
                                key={`${chat.id}-${message.id}`}
                                chat={chat}
                                index={index}
                                lastMessage={message}
                                query={search}
                                onClick={() => onOpenMessage(chat, message.id)}
                            />
                        ))}
                    </div>
                )}
                {!covered && !fullSearch && pageHeader && retry && <div className={styles.syncError} role="status"><span>{syncStatus === 'Offline' ? 'Cannot reach the server.' : 'Could not update your chats.'}</span><button type="button" onClick={onRetry}>Retry</button></div>}
                {!covered && !fullSearch && activeTab === 'chats' && !requestsOpen && (
                    <>
                        <div className={styles.folders}>
                            {folders.map((folder) => (
                                <button
                                    key={folder.id}
                                    className={folder.id === activeFolder ? `${styles.folder} ${styles.folderActive}` : styles.folder}
                                    type="button"
                                    onClick={() => setActiveFolder(folder.id)}
                                >
                                    {folder.label}
                                    {unreadChats > 0 && <span className={styles.folderCount}>{formatCount(unreadChats)}</span>}
                                </button>
                            ))}
                        </div>
                        <button className={styles.requestsRow} type="button" onClick={openRequests}>
                            <span className={styles.requestsIcon}><RequestsIcon /></span>
                            <span className={styles.requestsText}>
                                <span className={styles.requestsLabel}>Message Requests</span>
                                <span className={styles.requestsHint}>
                                    {requests.length > 0 ? `${requests.length} new ${requests.length === 1 ? 'request' : 'requests'}` : 'New & Restricted chats'}
                                </span>
                            </span>
                            {requests.length > 0 && <span className={styles.requestsCount}>{requests.length}</span>}
                        </button>
                        <div className={styles.list}>
                            {loading && <ChatListSkeleton />}
                            {!loading && chats.length === 0 && <div className={styles.noResults}>No chats found</div>}
                            {chats.map((chat, index) => (
                                <ChatListItem
                                    key={chat.id}
                                    chat={chat}
                                    index={index}
                                    lastMessage={getLastMessage(chat.id)}
                                    active={chat.id === selectedChatId}
                                    leaving={chat.leaving}
                                    selected={selectedIds.includes(chat.id)}
                                    onSelect={() => selectChat(chat)}
                                    onClick={() => (selectedChats.length > 0 ? toggleChat(chat) : onSelectChat(chat))}
                                    onPin={chat.pinned || pinnedCount < PIN_LIMIT ? () => onPinChat(chat) : undefined}
                                    onMute={() => onMuteChat(chat)}
                                    onRestrict={() => onRestrictChat(chat.id)}
                                    onDelete={() => onDeleteChat(chat.id)}
                                />
                            ))}
                        </div>
                    </>
                )}
                {!covered && inRequests && (
                    <>
                        <div className={styles.requestsTabs}>
                            <span className={requestsTab === 'requests' ? styles.requestsSlider : `${styles.requestsSlider} ${styles.requestsSliderRight}`} />
                            <button
                                className={requestsTab === 'requests' ? `${styles.requestsTab} ${styles.requestsTabActive}` : styles.requestsTab}
                                type="button"
                                onClick={() => setRequestsTab('requests')}
                            >
                                Message Requests
                            </button>
                            <button
                                className={requestsTab === 'restricted' ? `${styles.requestsTab} ${styles.requestsTabActive}` : styles.requestsTab}
                                type="button"
                                onClick={() => setRequestsTab('restricted')}
                            >
                                Restricted
                            </button>
                        </div>
                        <div className={styles.list}>
                            {requestsTab === 'restricted' && !loading && restricted.length === 0 && <div className={styles.noResults}>No restricted chats</div>}
                            {requestsTab === 'restricted' && restricted.map((chat, index) => (
                                <ChatListItem
                                    key={chat.id}
                                    chat={chat}
                                    index={index}
                                    lastMessage={getLastMessage(chat.id)}
                                    withDate
                                    active={chat.id === selectedChatId}
                                    leaving={chat.leaving}
                                    onClick={() => onSelectChat(chat)}
                                    onUnrestrict={() => onUnrestrictChat(chat.id)}
                                    onDelete={() => onDeleteChat(chat.id)}
                                />
                            ))}
                            {requestsTab === 'requests' && loading && <ChatListSkeleton />}
                            {requestsTab === 'requests' && !loading && requests.length === 0 && <div className={styles.noResults}>No message requests</div>}
                            {requestsTab === 'requests' && requests.map((chat, index) => (
                                <ChatListItem
                                    key={chat.id}
                                    chat={chat}
                                    index={index}
                                    lastMessage={getLastMessage(chat.id)}
                                    withDate
                                    active={chat.id === selectedChatId}
                                    leaving={chat.leaving}
                                    onClick={() => onSelectChat(chat)}
                                />
                            ))}
                        </div>
                    </>
                )}
                {!covered && tabKind && searching && (
                    <div className={styles.list}>
                        {tabResults.length === 0 && tabMessages.length === 0 && <div className={styles.noResults}>No results</div>}
                        {tabResults.map(({ chat, lastMessage }, index) => (
                            <ChatListItem key={chat.id} chat={chat} index={index} lastMessage={lastMessage} active={chat.id === selectedChatId} query={search} onClick={() => onSelectChat(chat)} />
                        ))}
                        {tabMessages.length > 0 && <div className={styles.sectionTitle}>Messages</div>}
                        {tabMessages.map(({ chat, message }, index) => (
                            <ChatListItem key={`${chat.id}-${message.id}`} chat={chat} index={index} lastMessage={message} query={search} onClick={() => onOpenMessage(chat, message.id)} />
                        ))}
                    </div>
                )}
                {!covered && activeTab === 'groups' && !searching && loading && <div className={styles.list}><ChatListSkeleton /></div>}
                {!covered && activeTab === 'groups' && !searching && !loading && tabRooms.length === 0 && <GroupsPage />}
                {!covered && activeTab === 'groups' && !searching && !loading && tabRooms.length > 0 && (
                    <div className={styles.groups}>
                        <GroupRail
                            groups={tabRooms}
                            selectedId={railGroup?.id}
                            pinnedCount={roomPinnedCount}
                            onSelect={onSelectGroup}
                            onNew={() => openFind('new-group')}
                            onPin={onPinChat}
                            onMute={onMuteChat}
                            onLeave={onLeaveRoom}
                            onDelete={onDeleteRoom}
                        />
                        {railGroup?.status === 'request' && (
                            <div className={styles.invite}>
                                <Avatar name={railGroup.name} photo={railGroup.photo} size={88} />
                                <span className={styles.inviteName}>{railGroup.name}</span>
                                <span className={styles.inviteCount}>{railGroup.memberCount} {railGroup.memberCount === 1 ? 'member' : 'members'}</span>
                                <div className={styles.inviteButtons}>
                                    <button className={styles.inviteDecline} type="button" onClick={() => onDeclineInvite(railGroup)}>Decline</button>
                                    <button className={styles.inviteAccept} type="button" onClick={() => onAcceptInvite(railGroup)}>Accept</button>
                                </div>
                            </div>
                        )}
                        {railGroup?.status === 'active' && (
                            <ChannelList
                                key={railGroup.id}
                                room={railGroup}
                                list={channelList}
                                openId={selectedChatId}
                                canManage={can(railGroup, 'manageChannels')}
                                onOpen={(channel) => onOpenChannel(railGroup, channel)}
                                onOpenInfo={() => onOpenGroupInfo(railGroup)}
                                onChanged={() => onChannelsChanged(railGroup.id)}
                                onError={(error) => onNotice(error.message)}
                            />
                        )}
                    </div>
                )}
                {!covered && activeTab === 'channels' && !searching && (
                    <RoomList
                        rooms={tabRooms}
                        loading={loading}
                        emptyPage={activeTab === 'groups' ? <GroupsPage /> : <ChannelsPage />}
                        selectedChatId={selectedChatId}
                        pinnedCount={roomPinnedCount}
                        getLastMessage={getLastMessage}
                        onSelect={onSelectChat}
                        onPin={onPinChat}
                        onMute={onMuteChat}
                        onLeave={onLeaveRoom}
                        onDelete={onDeleteRoom}
                        onAccept={onAcceptInvite}
                        onDecline={onDeclineInvite}
                    />
                )}
                {!covered && activeTab === 'settings' && (
                    <SettingsPage user={user} onLogout={onLogout} onUserChange={onUserChange} onAccountGone={onAccountGone} />
                )}
                {!covered && !requestsOpen && !fullSearch && !alertsOpen && activeTab !== 'settings' && <ActionBubble onSelect={openFind} />}
            </div>
        </div>
    )
}

export default Sidebar
