import { useEffect, useRef, useState } from 'react'
import styles from './Sidebar.module.css'
import MenuIcon from '../Icons/MenuIcon'
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
import { formatCount } from '../../utils/formatCount'

const folders = [{ id: 'all', label: 'General' }]
const PIN_LIMIT = 5

function Sidebar({ user, onUserChange, onAccountGone, loading, chats, requests, restricted, searchResults, selectedChatId, getLastMessage, search, searchFocus, onSearchChange, onSelectChat, onOpenMessage, onStartChat, onPinChat, onMuteChat, onRestrictChat, onUnrestrictChat, onDeleteChat, onLogout }) {
    const [activeTab, setActiveTab] = useState('chats')
    const [activeFolder, setActiveFolder] = useState('all')
    const [requestsOpen, setRequestsOpen] = useState(false)
    const [requestsTab, setRequestsTab] = useState('requests')
    const [findKind, setFindKind] = useState(null)
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
        }
        window.addEventListener('popstate', handlePopState)
        return () => window.removeEventListener('popstate', handlePopState)
    }, [])

    useEffect(() => {
        if (!(requestsOpen || findKind) || selectedChatId) return
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') window.history.back()
        }
        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [requestsOpen, findKind, selectedChatId])

    const openRequests = () => {
        window.history.pushState({ requestsOpen: true }, '')
        setRequestsTab('requests')
        setRequestsOpen(true)
    }

    const openFind = (kind) => {
        window.history.pushState({ findKind: kind }, '')
        setFindKind(kind)
    }

    const searching = search.trim().length > 0
    const pinnedCount = chats.filter((chat) => chat.pinned).length
    const unreadChats = chats.filter((chat) => chat.unreadCount > 0 && !chat.muted).length

    return (
        <div className={styles.sidebar}>
            {!findKind && (
                <div className={styles.header}>
                    <button className={styles.menuButton} type="button" aria-label="Menu">
                        <MenuIcon />
                    </button>
                    <label className={styles.searchBox}>
                        <span className={styles.searchIcon}><SearchIcon /></span>
                        <input ref={searchRef} className={styles.search} type="text" placeholder="Search" value={search} onChange={(e) => onSearchChange(e.target.value)} />
                    </label>
                </div>
            )}
            {findKind && <FindPanel kind={findKind} onStartChat={onStartChat} />}
            {!findKind && activeTab === 'chats' && !requestsOpen && searching && (
                <div className={styles.list}>
                    {searchResults.chats.length === 0 && searchResults.messages.length === 0 && <div className={styles.noResults}>No results</div>}
                    {searchResults.chats.length > 0 && <div className={styles.sectionTitle}>Chats</div>}
                    {searchResults.chats.map(({ chat, lastMessage }) => (
                        <ChatListItem
                            key={chat.id}
                            chat={chat}
                            lastMessage={lastMessage}
                            active={chat.id === selectedChatId}
                            query={search}
                            onClick={() => onSelectChat(chat)}
                        />
                    ))}
                    {searchResults.messages.length > 0 && <div className={styles.sectionTitle}>Messages</div>}
                    {searchResults.messages.map(({ chat, message }) => (
                        <ChatListItem
                            key={`${chat.id}-${message.id}`}
                            chat={chat}
                            lastMessage={message}
                            query={search}
                            onClick={() => onOpenMessage(chat, message.id)}
                        />
                    ))}
                </div>
            )}
            {!findKind && activeTab === 'chats' && !requestsOpen && !searching && (
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
                        {chats.map((chat) => (
                            <ChatListItem
                                key={chat.id}
                                chat={chat}
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
            {!findKind && activeTab === 'chats' && requestsOpen && (
                <>
                    <div className={styles.requestsHeader}>
                        <button className={styles.requestsBack} type="button" aria-label="Back" onClick={() => window.history.back()}>
                            <BackIcon />
                        </button>
                        <span className={styles.requestsTitle}>New & Restricted</span>
                    </div>
                    <div className={styles.requestsTabs}>
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
                        {requestsTab === 'restricted' && restricted.map((chat) => (
                            <ChatListItem
                                key={chat.id}
                                chat={chat}
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
                        {requestsTab === 'requests' && requests.map((chat) => (
                            <ChatListItem
                                key={chat.id}
                                chat={chat}
                                lastMessage={getLastMessage(chat.id)}
                                active={chat.id === selectedChatId}
                                leaving={chat.leaving}
                                onClick={() => onSelectChat(chat)}
                            />
                        ))}
                    </div>
                </>
            )}
            {!findKind && (activeTab === 'groups' || activeTab === 'channels') && searching && (
                <div className={styles.noResults}>No results</div>
            )}
            {!findKind && activeTab === 'groups' && !searching && <GroupsPage />}
            {!findKind && activeTab === 'channels' && !searching && <ChannelsPage />}
            {!findKind && activeTab === 'settings' && (
                <SettingsPage user={user} onLogout={onLogout} onUserChange={onUserChange} onAccountGone={onAccountGone} />
            )}
            {!findKind && !requestsOpen && activeTab !== 'settings' && <ActionBubble onSelect={openFind} />}
            <BottomTabs activeTab={activeTab} onChange={setActiveTab} />
        </div>
    )
}

export default Sidebar
