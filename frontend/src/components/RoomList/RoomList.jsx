import styles from './RoomList.module.css'
import Avatar from '../Avatar/Avatar'
import ChatListItem from '../ChatListItem/ChatListItem'
import ChatListSkeleton from '../Skeleton/ChatListSkeleton'

const PIN_LIMIT = 5

function RoomList({ rooms, loading, emptyPage, selectedChatId, pinnedCount, getLastMessage, onSelect, onPin, onMute, onLeave, onDelete, onAccept, onDecline }) {
    const invites = rooms.filter((room) => room.status === 'request')
    const joined = rooms.filter((room) => room.status === 'active')

    if (loading) return <div className={styles.list}><ChatListSkeleton /></div>
    if (rooms.length === 0) return emptyPage

    return (
        <div className={styles.list}>
            {invites.length > 0 && <div className={styles.sectionTitle}>Invites</div>}
            {invites.map((room) => (
                <div key={room.id} className={styles.invite}>
                    <Avatar name={room.name} photo={room.photo} size={48} />
                    <span className={styles.text}>
                        <span className={styles.name}>{room.name}</span>
                        <span className={styles.count}>{room.memberCount} {room.memberCount === 1 ? 'member' : 'members'}</span>
                    </span>
                    <button className={styles.decline} type="button" onClick={() => onDecline(room)}>Decline</button>
                    <button className={styles.accept} type="button" onClick={() => onAccept(room)}>Accept</button>
                </div>
            ))}
            {invites.length > 0 && joined.length > 0 && <div className={styles.sectionTitle}>Joined</div>}
            {joined.map((room, index) => (
                <ChatListItem
                    key={room.id}
                    chat={room}
                    index={index}
                    lastMessage={getLastMessage(room.id)}
                    active={room.id === selectedChatId}
                    leaving={room.leaving}
                    onClick={() => onSelect(room)}
                    onPin={room.pinned || pinnedCount < PIN_LIMIT ? () => onPin(room) : undefined}
                    onMute={() => onMute(room)}
                    onLeave={room.role === 'owner' ? undefined : () => onLeave(room)}
                    onDelete={room.role === 'owner' ? () => onDelete(room) : undefined}
                />
            ))}
        </div>
    )
}

export default RoomList
