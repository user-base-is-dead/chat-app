package com.meghnathnews.mironchat.data

import android.os.Build
import org.json.JSONObject

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String,
)

class AuthRepository(
    private val api: ApiClient,
    private val tokens: TokenStore,
) {
    val hasSession: Boolean get() = tokens.refreshToken != null

    suspend fun login(username: String, password: String): User {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("platform", "android")
            .put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
        val session = requireData(api.send("/auth/login", "POST", body))
        tokens.save(session.getString("accessToken"), session.getString("refreshToken"))
        return session.getJSONObject("user").toUser()
    }

    suspend fun register(displayName: String, username: String, password: String) {
        val body = JSONObject()
            .put("displayName", displayName)
            .put("username", username)
            .put("password", password)
        api.send("/auth/register", "POST", body)
    }

    suspend fun currentUser(): User = requireData(api.authorized("/auth/me")).toUser()

    suspend fun logout() {
        try {
            api.authorized("/auth/logout", "POST")
        } catch (_: ApiError) {
        } finally {
            tokens.clear()
        }
    }

    suspend fun updateProfile(displayName: String, username: String, bio: String): User {
        val body = JSONObject()
            .put("displayName", displayName)
            .put("username", username)
            .put("bio", bio)
        return requireData(api.authorized("/users/me", "PATCH", body)).toUser()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = JSONObject()
            .put("currentPassword", currentPassword)
            .put("newPassword", newPassword)
        api.authorized("/users/me/password", "POST", body)
    }

    suspend fun disableAccount(password: String) {
        api.authorized("/users/me/disable", "POST", JSONObject().put("password", password))
    }

    suspend fun deleteAccount(password: String) {
        api.authorized("/users/me/delete", "POST", JSONObject().put("password", password))
    }

    fun forget() {
        tokens.clear()
    }

    private fun requireData(data: JSONObject?): JSONObject =
        data ?: throw ApiError(0, "unknown", "Something went wrong")

    private fun JSONObject.toUser() = User(
        id = getString("id"),
        username = getString("username"),
        displayName = getString("displayName"),
        bio = optString("bio"),
    )
}
