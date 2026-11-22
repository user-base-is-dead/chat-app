package com.civenth.app.data

import org.json.JSONArray
import org.json.JSONObject

const val ROLE_NAME_LIMIT = 32
const val MEMBER_LIMIT = 1000

val ROLE_COLORS = listOf("red", "orange", "amber", "yellow", "lime", "green", "teal", "cyan", "blue", "indigo", "purple", "pink")

data class Permission(val key: String, val label: String, val hint: String)

val PERMISSIONS = listOf(
    Permission("manageGroup", "Manage group", "Name, photo, link and group switches"),
    Permission("manageChannels", "Manage channels", "Create, rename, move and delete channels"),
    Permission("manageRoles", "Manage roles", "Edit and give roles below their own"),
    Permission("addMembers", "Add members", "Even when members cannot add"),
    Permission("removeMembers", "Remove members", "Remove and ban members below them"),
    Permission("deleteMessages", "Delete messages", "Delete other members' messages"),
    Permission("sendAlways", "Always send", "Even when members cannot send"),
    Permission("administrator", "Administrator", "Every permission above"),
)

data class Role(
    val id: String,
    val name: String,
    val color: String?,
    val separate: Boolean,
    val permissions: List<String>,
    val position: Int,
    val memberCount: Int,
)

data class RoleDraft(
    val name: String = "",
    val color: String? = null,
    val separate: Boolean = false,
    val permissions: List<String> = emptyList(),
)

data class Standing(val owner: Boolean, val rank: Int?)

data class GroupData(val members: List<RoomMember>, val roles: List<Role>, val loadedAt: Long)

fun Chat.can(key: String) = permissions.contains(key)

fun rankOf(roleIds: List<String>, roles: List<Role>): Int? =
    roles.filter { roleIds.contains(it.id) }.minOfOrNull { it.position }

fun colorOf(roleIds: List<String>, roles: List<Role>): String? =
    roles.firstOrNull { it.color != null && roleIds.contains(it.id) }?.color

fun separateRoleOf(roleIds: List<String>, roles: List<Role>): Role? =
    roles.firstOrNull { it.separate && roleIds.contains(it.id) }

fun Standing.above(position: Int): Boolean = owner || (rank != null && rank < position)

fun Standing.outranks(other: Standing): Boolean = when {
    other.owner -> false
    owner -> true
    rank == null -> false
    else -> other.rank == null || rank < other.rank
}

class RoleRepository(private val api: ApiClient) {
    suspend fun list(roomId: String): List<Role> {
        val items = api.authorizedList("/rooms/$roomId/roles")
        return List(items.length()) { items.getJSONObject(it).toRole() }
    }

    suspend fun create(roomId: String, draft: RoleDraft) {
        api.authorized("/rooms/$roomId/roles", "POST", draft.toJson())
    }

    suspend fun update(roomId: String, roleId: String, draft: RoleDraft) {
        api.authorized("/rooms/$roomId/roles/$roleId", "PATCH", draft.toJson())
    }

    suspend fun move(roomId: String, roleId: String, direction: String): Role? =
        api.authorized("/rooms/$roomId/roles/$roleId", "PATCH", JSONObject().put("move", direction))?.toRole()

    suspend fun delete(roomId: String, roleId: String) {
        api.authorized("/rooms/$roomId/roles/$roleId", "DELETE")
    }

    suspend fun setMemberRoles(roomId: String, userId: String, roleIds: List<String>) {
        api.authorized("/rooms/$roomId/members/$userId/roles", "PUT", JSONObject().put("roleIds", JSONArray(roleIds)))
    }

    private fun RoleDraft.toJson(): JSONObject = JSONObject()
        .put("name", name.trim())
        .put("color", color ?: JSONObject.NULL)
        .put("separate", separate)
        .put("permissions", JSONArray(permissions))

    private fun JSONObject.toRole(): Role {
        val keys = optJSONArray("permissions") ?: JSONArray()
        return Role(
            id = getString("id"),
            name = optString("name"),
            color = if (isNull("color")) null else optString("color").ifEmpty { null },
            separate = optBoolean("separate"),
            permissions = List(keys.length()) { keys.getString(it) },
            position = optInt("position"),
            memberCount = optInt("memberCount"),
        )
    }
}
