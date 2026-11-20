package com.meghnathnews.mironchat.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

const val VIEW_BATCH = 100
const val COMMENT_PAGE_SIZE = 50
const val COMMENT_MAX_LENGTH = 2000

val REACTIONS = listOf("👍", "❤️", "🔥", "😂", "😮", "😢")

data class ReactionCount(val emoji: String, val count: Int)

data class PostStats(
    val id: String,
    val views: Int,
    val comments: Int,
    val allowReactions: Boolean,
    val allowComments: Boolean,
    val reactions: List<ReactionCount>,
    val myReaction: String,
)

data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userDeleted: Boolean,
    val text: String,
    val createdAt: Long,
)

data class PostSwitches(val allowReactions: Boolean = true, val allowComments: Boolean = true)

fun JSONArray.toReactions(): List<ReactionCount> =
    (0 until length()).map { index ->
        val item = getJSONObject(index)
        ReactionCount(item.optString("emoji"), item.optInt("count"))
    }

fun JSONObject.toComment() = Comment(
    id = optString("id"),
    userId = optString("userId"),
    userName = optString("userName"),
    userDeleted = optBoolean("userDeleted"),
    text = optString("text"),
    createdAt = optLong("createdAt"),
)

class PostRepository(private val api: ApiClient, private val prefs: SharedPreferences) {
    suspend fun view(roomId: String, ids: List<String>): List<PostStats> {
        val body = JSONObject().put("ids", JSONArray(ids))
        val posts = api.authorized("/rooms/$roomId/views", "POST", body)?.optJSONArray("posts") ?: return emptyList()
        return (0 until posts.length()).map { posts.getJSONObject(it).toStats() }
    }

    suspend fun react(roomId: String, messageId: String, emoji: String): PostStats =
        required(api.authorized("/rooms/$roomId/posts/$messageId/reaction", "PUT", JSONObject().put("emoji", emoji))).toStats()

    suspend fun unreact(roomId: String, messageId: String): PostStats =
        required(api.authorized("/rooms/$roomId/posts/$messageId/reaction", "DELETE")).toStats()

    suspend fun comments(roomId: String, messageId: String, beforeId: String? = null): List<Comment> {
        val before = if (beforeId != null) "?before=$beforeId" else ""
        val list = api.authorized("/rooms/$roomId/posts/$messageId/comments$before")?.optJSONArray("comments") ?: return emptyList()
        return (0 until list.length()).map { list.getJSONObject(it).toComment() }
    }

    suspend fun addComment(roomId: String, messageId: String, text: String): Comment =
        required(api.authorized("/rooms/$roomId/posts/$messageId/comments", "POST", JSONObject().put("text", text))).toComment()

    suspend fun deleteComment(roomId: String, messageId: String, commentId: String) {
        api.authorized("/rooms/$roomId/posts/$messageId/comments/$commentId", "DELETE")
    }

    suspend fun setSwitches(roomId: String, messageId: String, allowReactions: Boolean, allowComments: Boolean): PostStats {
        val body = JSONObject().put("allowReactions", allowReactions).put("allowComments", allowComments)
        return required(api.authorized("/rooms/$roomId/posts/$messageId", "PATCH", body)).toStats()
    }

    fun switches(roomId: String) = PostSwitches(
        allowReactions = prefs.getBoolean("reactions:$roomId", true),
        allowComments = prefs.getBoolean("comments:$roomId", true),
    )

    fun saveSwitches(roomId: String, switches: PostSwitches) {
        prefs.edit()
            .putBoolean("reactions:$roomId", switches.allowReactions)
            .putBoolean("comments:$roomId", switches.allowComments)
            .apply()
    }

    private fun required(value: JSONObject?) = value ?: throw ApiError(0, "unknown", "Something went wrong")

    private fun JSONObject.toStats() = PostStats(
        id = optString("id"),
        views = optInt("views"),
        comments = optInt("comments"),
        allowReactions = optBoolean("allowReactions"),
        allowComments = optBoolean("allowComments"),
        reactions = optJSONArray("reactions")?.toReactions() ?: emptyList(),
        myReaction = optString("myReaction"),
    )
}
