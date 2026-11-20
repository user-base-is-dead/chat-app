import { useEffect, useRef, useState } from 'react'
import styles from './Sidebar.module.css'
import SearchIcon from '../Icons/SearchIcon'
import ChatListItem from '../ChatListItem/ChatListItem'
import ChatListSkeleton from '../Skeleton/ChatListSkeleton'
import BottomTabs from '../BottomTabs/BottomTabs'
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

const folders = [{ id: 'all', label: 'General' }]
const PIN_LIMIT = 5
const tabKinds = { groups: 'group', channels: 'channel' }
const titles = { chats: 'Chats', groups: 'Groups', channels: 'Channels', settings: 'Settings' }

function Sidebar({ user, onUserChange, onAccountGone, loading, chats, requests, restricted, searchResults, selectedChatId, getLastMessage, search, searchFocus, onSearchChange, onSelectChat, onOpenMessage, onStartChat, onPinChat, onMuteChat, onRestrictChat, onUnrestrictChat, onDeleteChat, onLogout, rooms, knownPeople, roomPinnedCount, onRoomCreated, onOpenRoom, onOpenKey, onLeaveRoom, onDeleteRoom, onAcceptInvite, onDeclineInvite, railGroup, channelList, onSelectGroup, onOpenChannel, onOpenGroupInfo, onChannelsChanged, onNotice }) {
    const [activeTab, setActiveTab] = useState('chats')
    const [activeFolder, setActiveFolder] = useState('all')
    const [requestsOpen, setRequestsOpen] = useState(false)
    const [requestsTab, setRequestsTab] = useState('requests')
    const [findKind, setFindKind] = useState(null)
    const [createKind, setCreateKind] = useState(null)
    const searchRef = useRef(null)
    const wantSearch = useRef(false)

    useEffect(() => {
        if (!searchFocus) return
        wantSearch.current = true
        setActiveTab('chats')
    }, [searchFocus])

    useEffect(() => {
        if (!wantSearch.current || !searchRef.current?.offsetParent) return
        wantSearch.current = false
        searchRef.current.focus()
    })

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
        if (!(requestsOpen || findKind || createKind) || selectedChatId) return
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') window.history.back()
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [requestsOpen, findKind, createKind, selectedChatId])

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
    const unreadChats = chats.filter((chat) => chat.unreadCount > 0 && !chat.muted).length

    const inRequests = activeTab === 'chats' && requestsOpen
    const title = inRequests ? 'New & Restricted' : titles[activeTab]

    return (
        <div className={styles.sidebar}>
            <BottomTabs user={user} activeTab={activeTab} onChange={setActiveTab} />
            <div className={styles.panel}>
                {!covered && (
                    <div className={styles.header}>
                        <div className={styles.titleRow}>
                            {inRequests && (
                                <button className={styles.requestsBack} type="button" aria-label="Back" onClick={() => window.history.back()}>
                                    <BackIcon />
                                </button>
                            )}
                            <h1 key={title} className={inRequests ? `${styles.title} ${styles.titleSmall}` : styles.title}>{title}</h1>
                        </div>
                        {activeTab !== 'settings' && (
                            <label className={styles.searchBox}>
                                <span className={styles.searchIcon}><SearchIcon /></span>
                                <input ref={searchRef} className={styles.search} type="text" placeholder={`Search your ${titles[activeTab].toLowerCase()}`} value={search} onChange={(e) => onSearchChange(e.target.value)} />
                            </label>
                        )}
                    </div>
                )}
                {findKind && <FindPanel kind={findKind} onStartChat={onStartChat} onOpenRoom={onOpenRoom} onOpenKey={onOpenKey} />}
                {createKind && <RoomCreate kind={createKind} known={knownPeople} onCreated={finishCreate} />}
                {!covered && activeTab === 'chats' && !requestsOpen && searching && (
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
                {!covered && activeTab === 'chats' && !requestsOpen && !searching && (
                    <>
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
                                    onClick={() => onSelectChat(chat)}
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
                {!covered && !requestsOpen && activeTab !== 'settings' && <ActionBubble onSelect={openFind} />}
            </div>
        </div>
    )
}

export default Sidebar
