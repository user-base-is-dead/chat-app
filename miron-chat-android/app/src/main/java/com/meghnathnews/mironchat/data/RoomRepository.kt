package com.meghnathnews.mironchat.data

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

const val MEMBER_PAGE_SIZE = 100

data class RoomPreview(
    val id: String,
    val kind: String,
    val title: String,
    val about: String,
    val username: String,
    val photo: String,
    val memberCount: Int,
    val status: String,
)

data class RoomMember(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val status: String,
    val disabled: Boolean,
    val deleted: Boolean,
    val online: Boolean = false,
    val lastSeenHidden: Boolean = false,
    val roleIds: List<String> = emptyList(),
)

data class Presence(val online: Boolean, val hidden: Boolean, val at: Long)

data class AddResult(val added: Int, val invited: Int, val skipped: Int)

data class UploadedPhoto(val id: String, val url: String)

data class RoomDetail(val chat: Chat, val onlineCount: Int)

fun roomLabel(kind: String) = if (kind == "channel") "Channel" else "Group"

private val joinPath = Regex("/join/([A-Za-z0-9_]+)/?$")

fun joinKey(text: String): String? = joinPath.find(text.trim())?.groupValues?.get(1)

class RoomRepository(private val api: ApiClient, private val chats: ChatRepository) {
    suspend fun create(
        kind: String,
        title: String,
        about: String,
        isPublic: Boolean,
        username: String,
        photoId: String,
        userIds: List<String>,
    ): Pair<Chat, AddResult> {
        val body = JSONObject()
            .put("kind", kind)
            .put("title", title)
            .put("about", about)
            .put("isPublic", isPublic)
            .put("username", if (isPublic) username else "")
            .put("photoId", photoId)
            .put("userIds", JSONArray(userIds))
        val room = required(api.authorized("/rooms", "POST", body))
        return chats.toRoom(room) to room.toAddResult()
    }

    suspend fun detail(roomId: String): RoomDetail {
        val room = required(api.authorized("/rooms/$roomId"))
        return RoomDetail(chats.toRoom(room), room.optInt("onlineCount"))
    }

    suspend fun update(roomId: String, changes: JSONObject): Chat =
        chats.toRoom(required(api.authorized("/rooms/$roomId", "PATCH", changes)))

    suspend fun delete(roomId: String) {
        api.authorized("/rooms/$roomId", "DELETE")
    }

    suspend fun accept(roomId: String) {
        api.authorized("/rooms/$roomId/accept", "POST")
    }

    suspend fun decline(roomId: String) {
        api.authorized("/rooms/$roomId/decline", "POST")
    }

    suspend fun leave(roomId: String) {
        api.authorized("/rooms/$roomId/leave", "POST")
    }

    suspend fun setMuted(roomId: String, muted: Boolean) {
        api.authorized("/rooms/$roomId/${if (muted) "mute" else "unmute"}", "POST")
    }

    suspend fun setPinned(roomId: String, pinned: Boolean) {
        api.authorized("/rooms/$roomId/${if (pinned) "pin" else "unpin"}", "POST")
    }

    suspend fun resetInvite(roomId: String): Chat =
        chats.toRoom(required(api.authorized("/rooms/$roomId/invite", "POST")))

    suspend fun transfer(roomId: String, userId: String) {
        api.authorized("/rooms/$roomId/transfer", "POST", JSONObject().put("userId", userId))
    }

    suspend fun members(roomId: String, offset: Int = 0, channelId: String? = null): List<RoomMember> {
        val items = api.authorizedList("/rooms/$roomId/members?offset=$offset" + if (channelId != null) "&channel=$channelId" else "")
        return List(items.length()) {
            val member = items.getJSONObject(it)
            val deleted = member.optBoolean("deleted")
            RoomMember(
                id = member.getString("id"),
                username = member.optString("username"),
                name = if (deleted) "Deleted Account" else member.optString("displayName"),
                role = member.optString("role"),
                status = member.optString("status"),
                disabled = member.optBoolean("disabled"),
                deleted = deleted,
                online = member.optBoolean("online"),
                lastSeenHidden = member.optBoolean("lastSeenHidden"),
                roleIds = member.optJSONArray("roleIds")?.let { ids -> List(ids.length()) { ids.getString(it) } }.orEmpty(),
            )
        }
    }

    suspend fun addMembers(roomId: String, userIds: List<String>): AddResult {
        val body = JSONObject().put("userIds", JSONArray(userIds))
        return required(api.authorized("/rooms/$roomId/members", "POST", body)).toAddResult()
    }

    suspend fun setRole(roomId: String, userId: String, role: String) {
        api.authorized("/rooms/$roomId/members/$userId", "PATCH", JSONObject().put("role", role))
    }

    suspend fun removeMember(roomId: String, userId: String) {
        api.authorized("/rooms/$roomId/members/$userId", "DELETE")
    }

    suspend fun find(query: String, kind: String): List<RoomPreview> {
        val items = api.authorizedList("/find/rooms?q=${Uri.encode(query)}&kind=$kind")
        return List(items.length()) { items.getJSONObject(it).toPreview() }
    }

    suspend fun preview(key: String): RoomPreview =
        required(api.authorized("/join/${Uri.encode(key)}")).toPreview()

    suspend fun join(key: String): Chat =
        chats.toRoom(required(api.authorized("/join/${Uri.encode(key)}", "POST")))

    suspend fun uploadPhoto(bytes: ByteArray, type: String): UploadedPhoto {
        val photo = required(api.upload("/media/photo", bytes, type))
        return UploadedPhoto(photo.getString("id"), photo.getString("url"))
    }

    private fun required(value: JSONObject?) = value ?: throw ApiError(0, "unknown", "Something went wrong")

    private fun JSONObject.toAddResult() = AddResult(optInt("added"), optInt("invited"), optInt("skipped"))

    private fun JSONObject.toPreview() = RoomPreview(
        id = getString("id"),
        kind = getString("kind"),
        title = getString("title"),
        about = optString("about"),
        username = optString("username"),
        photo = optString("photo"),
        memberCount = optInt("memberCount"),
        status = optString("status"),
    )
}
