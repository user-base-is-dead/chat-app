package com.civenth.app.data

import com.civenth.app.BuildConfig
import com.civenth.app.update.APP_PLATFORM
import com.civenth.app.update.APP_VERSION
import com.civenth.app.update.AppUpdate
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private val JSON_TYPE = "application/json".toMediaType()

class ApiError(val status: Int, val code: String, message: String) : Exception(message)

class ApiClient(private val tokens: TokenStore) {
    val http = OkHttpClient()
    private val refreshLock = Mutex()

    suspend fun send(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = null,
    ): JSONObject? = parseObject(fetch(path, method, body, token))

    suspend fun authorized(path: String, method: String = "GET", body: JSONObject? = null): JSONObject? =
        parseObject(withRefresh { token -> fetch(path, method, body, token) })

    suspend fun authorizedList(path: String): JSONArray {
        val text = withRefresh { token -> fetch(path, "GET", null, token) }
        return try {
            JSONArray(text ?: "[]")
        } catch (_: JSONException) {
            throw ApiError(0, "unknown", "Something went wrong")
        }
    }

    suspend fun upload(path: String, bytes: ByteArray, type: String): JSONObject? =
        parseObject(withRefresh { token -> fetch(path, "POST", null, token, bytes.toRequestBody(type.toMediaType())) })

    suspend fun refreshAccess() = refreshAfter(tokens.accessToken)

    private suspend fun fetch(
        path: String,
        method: String,
        body: JSONObject?,
        token: String?,
        raw: RequestBody? = null,
    ): String? = withContext(Dispatchers.IO) {
        val payload = raw ?: body?.toString()?.toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url("${BuildConfig.API_URL}/v1$path")
            .method(method, payload ?: if (method == "POST") ByteArray(0).toRequestBody() else null)
            .header("X-App-Platform", APP_PLATFORM)
            .header("X-App-Version", APP_VERSION)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        try {
            http.newCall(request).execute().use { response ->
                if (response.code == 426) AppUpdate.required = true
                if (response.code == 204) return@withContext null
                val text = response.body?.string()
                if (!response.isSuccessful) {
                    val error = parseObject(text)?.optJSONObject("error")
                    throw ApiError(
                        response.code,
                        error?.optString("code")?.ifEmpty { null } ?: "unknown",
                        error?.optString("message")?.ifEmpty { null } ?: "Something went wrong",
                    )
                }
                text
            }
        } catch (_: IOException) {
            throw ApiError(0, "network", "Cannot reach the server. Check your connection.")
        }
    }

    private suspend fun <T> withRefresh(call: suspend (String?) -> T): T {
        val token = tokens.accessToken
        return try {
            call(token)
        } catch (error: ApiError) {
            if (error.status != 401) throw error
            refreshAfter(token)
            call(tokens.accessToken)
        }
    }

    private suspend fun refreshAfter(rejectedToken: String?) = refreshLock.withLock {
        if (tokens.accessToken != rejectedToken) return@withLock
        val refreshToken = tokens.refreshToken
            ?: throw ApiError(401, "unauthorized", "Please log in again")
        try {
            val session = send("/auth/refresh", "POST", JSONObject().put("refreshToken", refreshToken))
                ?: throw ApiError(0, "unknown", "Something went wrong")
            tokens.save(session.getString("accessToken"), session.getString("refreshToken"))
        } catch (error: ApiError) {
            if (error.status == 401) tokens.clear()
            throw error
        }
    }

    private fun parseObject(text: String?): JSONObject? {
        if (text.isNullOrBlank()) return null
        return try {
            JSONObject(text)
        } catch (_: JSONException) {
            null
        }
    }
}
