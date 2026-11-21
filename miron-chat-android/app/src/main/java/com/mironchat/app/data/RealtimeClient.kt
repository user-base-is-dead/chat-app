package com.mironchat.app.data

import com.mironchat.app.BuildConfig
import com.mironchat.app.update.APP_PLATFORM
import com.mironchat.app.update.APP_VERSION
import com.mironchat.app.update.AppUpdate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject

private const val UNAUTHORIZED = 4001
private const val NORMAL_CLOSE = 1000
private const val UPDATE_REQUIRED = 4426
private const val MAX_DELAY = 15_000L
private const val FIRST_DELAY = 1_000L

data class RealtimeEvent(val name: String, val data: JSONObject?)

class RealtimeClient(
    private val api: ApiClient,
    private val tokens: TokenStore,
    private val background: Boolean,
) {
    private val http = api.http.newBuilder().pingInterval(30, TimeUnit.SECONDS).build()
    private val incoming = MutableSharedFlow<RealtimeEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var job: Job? = null
    private var socket: WebSocket? = null
    private var ready = false

    val events: SharedFlow<RealtimeEvent> = incoming

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            var wait = FIRST_DELAY
            while (isActive) {
                ready = false
                val closeCode = connect()
                if (ready) wait = FIRST_DELAY
                if (closeCode == UNAUTHORIZED && !refreshed()) return@launch
                if (closeCode == UPDATE_REQUIRED) {
                    AppUpdate.required = true
                    return@launch
                }
                delay(wait)
                wait = min(MAX_DELAY, wait * 2)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun send(event: String, data: JSONObject) {
        socket?.send(JSONObject().put("event", event).put("data", data).toString())
    }

    private suspend fun refreshed(): Boolean = try {
        api.refreshAccess()
        true
    } catch (error: ApiError) {
        incoming.tryEmit(RealtimeEvent("session.check", null))
        error.status != 401
    }

    private suspend fun connect(): Int = suspendCancellableCoroutine { result ->
        val url = BuildConfig.API_URL.replaceFirst("http", "ws") + "/v1/ws"
        val connection = http.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val data = JSONObject()
                        .put("token", tokens.accessToken)
                        .put("background", background)
                        .put("platform", APP_PLATFORM)
                        .put("version", APP_VERSION)
                    webSocket.send(JSONObject().put("event", "auth").put("data", data).toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = try {
                        JSONObject(text)
                    } catch (_: JSONException) {
                        return
                    }
                    val event = message.optString("event")
                    if (event == "ready") ready = true
                    incoming.tryEmit(RealtimeEvent(event, message.optJSONObject("data")))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, null)
                    if (result.isActive) result.resume(code)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (result.isActive) result.resume(0)
                }
            },
        )
        socket = connection
        result.invokeOnCancellation {
            socket = null
            connection.close(NORMAL_CLOSE, null)
        }
    }
}
