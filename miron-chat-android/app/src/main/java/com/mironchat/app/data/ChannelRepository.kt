package com.mironchat.app.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

const val CHANNEL_NAME_LIMIT = 32

data class RoomCategory(val id: String, val name: String, val position: Int, val permissions: List<String> = emptyList())

data class ChannelOverride(val targetType: String, val targetId: String, val allow: List<String>, val deny: List<String>)

data class ChannelPermissionState(
    val overrides: List<ChannelOverride>,
    val permissions: List<String>,
    val synced: Boolean,
    val categoryId: String?,
)

data class ChannelPermission(val key: String, val label: String, val hint: String)

val CHANNEL_PERMISSIONS = listOf(
    ChannelPermission("viewChannel", "View channel", "See the channel and its messages"),
    ChannelPermission("sendMessages", "Send messages", "Write in this channel"),
    ChannelPermission("deleteMessages", "Delete messages", "Delete other members' messages here"),
    ChannelPermission("manageChannel", "Manage channel", "Rename, move and delete this channel"),
    ChannelPermission("managePermissions", "Manage permissions", "Change this channel's permissions"),
)

data class RoomChannel(
    val id: String,
    val name: String,
    val categoryId: String?,
    val position: Int,
    val unreadCount: Int,
    val lastReadId: Long,
    val readUpTo: Long,
    val permissions: List<String> = emptyList(),
    val isPrivate: Boolean = false,
)

data class ChannelList(val categories: List<RoomCategory>, val channels: List<RoomChannel>)

fun cleanChannelName(text: String): String =
    text.trim().lowercase().replace(' ', '-').filter { it.isLetterOrDigit() || it == '-' || it == '_' }

class ChannelRepository(private val api: ApiClient, private val prefs: SharedPreferences) {
    suspend fun list(roomId: String): ChannelList {
        val json = required(api.authorized("/rooms/$roomId/channels"))
        val categories = json.optJSONArray("categories")
        val channels = json.optJSONArray("channels")
        return ChannelList(
            categories = (0 until (categories?.length() ?: 0)).map { categories!!.getJSONObject(it).toCategory() },
            channels = (0 until (channels?.length() ?: 0)).map { channels!!.getJSONObject(it).toChannel() },
        )
    }

    suspend fun create(roomId: String, name: String, categoryId: String?): RoomChannel =
        required(api.authorized("/rooms/$roomId/channels", "POST", JSONObject().put("name", name).put("categoryId", categoryId ?: ""))).toChannel()

    suspend fun rename(roomId: String, channelId: String, name: String): RoomChannel =
        patchChannel(roomId, channelId, JSONObject().put("name", name))

    suspend fun move(roomId: String, channelId: String, up: Boolean): RoomChannel =
        patchChannel(roomId, channelId, JSONObject().put("move", if (up) "up" else "down"))

    suspend fun setCategory(roomId: String, channelId: String, categoryId: String?): RoomChannel =
        patchChannel(roomId, channelId, JSONObject().put("categoryId", categoryId ?: ""))

    suspend fun delete(roomId: String, channelId: String) {
        api.authorized("/rooms/$roomId/channels/$channelId", "DELETE")
    }

    suspend fun createCategory(roomId: String, name: String): RoomCategory =
        required(api.authorized("/rooms/$roomId/categories", "POST", JSONObject().put("name", name))).toCategory()

    suspend fun renameCategory(roomId: String, categoryId: String, name: String): RoomCategory =
        required(api.authorized("/rooms/$roomId/categories/$categoryId", "PATCH", JSONObject().put("name", name))).toCategory()

    suspend fun moveCategory(roomId: String, categoryId: String, up: Boolean): RoomCategory =
        required(api.authorized("/rooms/$roomId/categories/$categoryId", "PATCH", JSONObject().put("move", if (up) "up" else "down"))).toCategory()

    suspend fun deleteCategory(roomId: String, categoryId: String) {
        api.authorized("/rooms/$roomId/categories/$categoryId", "DELETE")
    }

    suspend fun permissions(roomId: String, kind: String, id: String): ChannelPermissionState =
        required(api.authorized("/rooms/$roomId/${scopePath(kind)}/$id/permissions")).toPermissionState()

    suspend fun setOverride(roomId: String, kind: String, id: String, item: ChannelOverride): ChannelPermissionState {
        val body = JSONObject().put("allow", JSONArray(item.allow)).put("deny", JSONArray(item.deny))
        return required(api.authorized("/rooms/$roomId/${scopePath(kind)}/$id/permissions/${item.targetType}/${item.targetId}", "PUT", body))
            .toPermissionState()
    }

    suspend fun sync(roomId: String, channelId: String): ChannelPermissionState =
        required(api.authorized("/rooms/$roomId/channels/$channelId/sync", "POST")).toPermissionState()

    fun openChannel(roomId: String): String? = prefs.getString("open:$roomId", null)

    fun saveOpenChannel(roomId: String, channelId: String) {
        prefs.edit().putString("open:$roomId", channelId).apply()
    }

    fun collapsed(roomId: String): Set<String> = prefs.getStringSet("collapsed:$roomId", emptySet()) ?: emptySet()

    fun saveCollapsed(roomId: String, ids: Set<String>) {
        prefs.edit().putStringSet("collapsed:$roomId", ids).apply()
    }

    private suspend fun patchChannel(roomId: String, channelId: String, body: JSONObject): RoomChannel =
        required(api.authorized("/rooms/$roomId/channels/$channelId", "PATCH", body)).toChannel()

    private fun required(value: JSONObject?) = value ?: throw ApiError(0, "unknown", "Something went wrong")

    private fun scopePath(kind: String) = if (kind == "category") "categories" else "channels"

    private fun JSONArray?.keys(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }

    private fun JSONObject.toPermissionState(): ChannelPermissionState {
        val items = optJSONArray("overrides")
        return ChannelPermissionState(
            overrides = (0 until (items?.length() ?: 0)).map { index ->
                val item = items!!.getJSONObject(index)
                ChannelOverride(item.optString("targetType"), item.optString("targetId"), item.optJSONArray("allow").keys(), item.optJSONArray("deny").keys())
            },
            permissions = optJSONArray("permissions").keys(),
            synced = optBoolean("synced"),
            categoryId = if (isNull("categoryId")) null else optString("categoryId").ifEmpty { null },
        )
    }

    private fun JSONObject.toCategory() = RoomCategory(optString("id"), optString("name"), optInt("position"), optJSONArray("permissions").keys())

    private fun JSONObject.toChannel() = RoomChannel(
        id = optString("id"),
        name = optString("name"),
        categoryId = if (isNull("categoryId")) null else optString("categoryId"),
        position = optInt("position"),
        unreadCount = optInt("unreadCount"),
        lastReadId = optString("lastReadId").toLongOrNull() ?: 0L,
        readUpTo = optString("readUpTo").toLongOrNull() ?: 0L,
        permissions = optJSONArray("permissions").keys(),
        isPrivate = optBoolean("private"),
    )
}
