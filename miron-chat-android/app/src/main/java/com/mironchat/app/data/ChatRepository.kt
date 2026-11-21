package com.mironchat.app.data

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

const val MESSAGE_PAGE_SIZE = 50

data class Chat(
    val id: String,
    val name: String,
    val username: String,
    val peerId: String,
    val status: String,
    val unreadCount: Int,
    val pinnedAt: Long?,
    val muted: Boolean,
    val peerDisabled: Boolean,
    val peerDeleted: Boolean,
    val peerOnline: Boolean,
    val peerLastSeenAt: Long?,
    val peerBio: String,
    val lastReadId: Long,
    val peerLastReadId: Long,
    val lastId: String?,
    val lastText: String?,
    val lastAt: Long?,
    val kind: String = "",
    val about: String = "",
    val isPublic: Boolean = false,
    val photo: String = "",
    val inviteCode: String = "",
    val role: String = "",
    val canSend: Boolean = true,
    val canAdd: Boolean = false,
    val memberCount: Int = 0,
    val membersCanSend: Boolean = true,
    val membersCanAdd: Boolean = false,
    val lastSender: String = "",
    val lastChatId: String = "",
    val permissions: List<String> = emptyList(),
    val rank: Int? = null,
) {
    val isRoom: Boolean get() = kind.isNotEmpty()
}

data class ServerMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: Long,
)

data class Person(
    val id: String,
    val username: String,
    val displayName: String,
)

class ChatRepository(private val api: ApiClient) {
    suspend fun list(): List<Chat> {
        val items = api.authorizedList("/chats")
        val rooms = api.authorizedList("/rooms")
        return List(items.length()) { items.getJSONObject(it).toChat() } +
            List(rooms.length()) { toRoom(rooms.getJSONObject(it)) }
    }

    suspend fun searchUsers(query: String): List<Person> {
        val items = api.authorizedList("/users/search?q=${Uri.encode(query)}")
        return List(items.length()) {
            val person = items.getJSONObject(it)
            Person(person.getString("id"), person.getString("username"), person.getString("displayName"))
        }
    }

    suspend fun openDm(userId: String): Chat {
        val chat = api.authorized("/chats/dm", "POST", JSONObject().put("userId", userId))
            ?: throw ApiError(0, "unknown", "Something went wrong")
        return chat.toChat()
    }

    suspend fun messagesBefore(chatId: String, beforeId: Long? = null): List<ServerMessage> {
        val before = beforeId?.let { "&before=$it" }.orEmpty()
        return messages("/chats/$chatId/messages?limit=$MESSAGE_PAGE_SIZE$before")
    }

    suspend fun messagesAfter(chatId: String, afterId: Long): List<ServerMessage> =
        messages("/chats/$chatId/messages?limit=$MESSAGE_PAGE_SIZE&after=$afterId")

    suspend fun send(chatId: String, text: String, switches: PostSwitches? = null): ServerMessage {
        val content = Base64.encodeToString(text.toByteArray(), Base64.NO_WRAP)
        val body = JSONObject().put("ciphertext", content)
        if (switches != null) body.put("allowReactions", switches.allowReactions).put("allowComments", switches.allowComments)
        val saved = api.authorized("/chats/$chatId/messages", "POST", body)
            ?: throw ApiError(0, "unknown", "Something went wrong")
        return toMessage(saved)
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Boolean =
        api.authorized("/chats/$chatId/messages/$messageId", "DELETE")?.optBoolean("chatDeleted") == true

    suspend fun markRead(chatId: String, messageId: Long) {
        api.authorized("/chats/$chatId/read", "POST", JSONObject().put("messageId", messageId.toString()))
    }

    suspend fun accept(chatId: String) {
        api.authorized("/chats/$chatId/accept", "POST")
    }

    suspend fun unrestrict(chatId: String) {
        api.authorized("/chats/$chatId/unrestrict", "POST")
    }

    suspend fun restrict(chatId: String) {
        api.authorized("/chats/$chatId/restrict", "POST")
    }

    suspend fun setPinned(chatId: String, pinned: Boolean) {
        api.authorized("/chats/$chatId/${if (pinned) "pin" else "unpin"}", "POST")
    }

    suspend fun setMuted(chatId: String, muted: Boolean) {
        api.authorized("/chats/$chatId/${if (muted) "mute" else "unmute"}", "POST")
    }

    suspend fun deleteChat(chatId: String) {
        api.authorized("/chats/$chatId", "DELETE")
    }

    fun toMessage(message: JSONObject) = ServerMessage(
        id = message.getString("id"),
        chatId = message.getString("chatId"),
        senderId = message.getString("senderId"),
        senderName = message.optString("senderName"),
        text = messageText(message),
        createdAt = message.optLong("createdAt"),
    )

    fun toRoom(room: JSONObject): Chat {
        val last = room.optJSONObject("lastMessage")
        return Chat(
            id = room.getString("id"),
            name = room.getString("title"),
            username = room.optString("username"),
            peerId = "",
            status = room.getString("status"),
            unreadCount = room.optInt("unreadCount"),
            pinnedAt = if (room.isNull("pinnedAt")) null else room.optLong("pinnedAt"),
            muted = room.optBoolean("muted"),
            peerDisabled = false,
            peerDeleted = false,
            peerOnline = false,
            peerLastSeenAt = null,
            peerBio = "",
            lastReadId = room.optString("lastReadId").toLongOrNull() ?: 0L,
            peerLastReadId = room.optString("readUpTo").toLongOrNull() ?: 0L,
            lastId = last?.optString("id"),
            lastText = last?.let { messageText(it) },
            lastAt = last?.optLong("createdAt"),
            kind = room.getString("kind"),
            about = room.optString("about"),
            isPublic = room.optBoolean("isPublic"),
            photo = room.optString("photo"),
            inviteCode = room.optString("inviteCode"),
            role = room.optString("role"),
            canSend = room.optBoolean("canSend"),
            canAdd = room.optBoolean("canAdd"),
            memberCount = room.optInt("memberCount"),
            membersCanSend = room.optBoolean("membersCanSend"),
            membersCanAdd = room.optBoolean("membersCanAdd"),
            lastSender = last?.optString("senderName").orEmpty().ifEmpty { if (last != null) "Deleted Account" else "" },
            lastChatId = last?.optString("chatId").orEmpty(),
            permissions = room.optJSONArray("permissions")?.let { keys -> List(keys.length()) { keys.getString(it) } }.orEmpty(),
            rank = if (room.isNull("rank")) null else room.optInt("rank"),
        )
    }

    private suspend fun messages(path: String): List<ServerMessage> {
        val items = api.authorizedList(path)
        return List(items.length()) { toMessage(items.getJSONObject(it)) }
    }

    private fun JSONObject.toChat(): Chat {
        val peer = getJSONObject("peer")
        val deleted = peer.optBoolean("deleted")
        val last = optJSONObject("lastMessage")
        return Chat(
            id = getString("id"),
            name = if (deleted) "Deleted Account" else peer.getString("displayName"),
            username = peer.optString("username"),
            peerId = peer.getString("id"),
            status = getString("status"),
            unreadCount = optInt("unreadCount"),
            pinnedAt = if (isNull("pinnedAt")) null else optLong("pinnedAt"),
            muted = optBoolean("muted"),
            peerDisabled = peer.optBoolean("disabled"),
            peerDeleted = deleted,
            peerOnline = peer.optBoolean("online"),
            peerLastSeenAt = if (peer.isNull("lastSeenAt")) null else peer.optLong("lastSeenAt"),
            peerBio = peer.optString("bio"),
            lastReadId = optString("lastReadId").toLongOrNull() ?: 0L,
            peerLastReadId = optString("peerLastReadId").toLongOrNull() ?: 0L,
            lastId = last?.optString("id"),
            lastText = last?.let { messageText(it) },
            lastAt = last?.optLong("createdAt"),
        )
    }

    private fun messageText(message: JSONObject): String {
        val content = message.optString("ciphertext")
        if (content.isEmpty()) return ""
        return try {
            String(Base64.decode(content, Base64.DEFAULT))
        } catch (_: IllegalArgumentException) {
            ""
        }
    }
}
