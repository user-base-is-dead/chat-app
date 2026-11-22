package com.civenth.app.alerts

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.civenth.app.BuildConfig
import com.civenth.app.CiventhApp
import com.civenth.app.data.ApiError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val PUSH_WAIT = 15_000L

object Push {
    val configured: Boolean get() = BuildConfig.FCM_APP_ID.isNotEmpty()

    fun init(context: Context): Boolean {
        if (!configured) return false
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FCM_APP_ID)
                .setApiKey(BuildConfig.FCM_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        return true
    }

    suspend fun register(context: Context): Boolean {
        if (!init(context)) return false
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            send(context, token)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun send(context: Context, token: String): Boolean {
        val app = context.applicationContext as CiventhApp
        if (!app.auth.hasSession) return false
        return try {
            app.auth.savePushToken(token)
        } catch (_: ApiError) {
            false
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error != null) continuation.resumeWithException(error) else continuation.resume(task.result)
    }
}

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        runBlocking {
            withTimeoutOrNull(PUSH_WAIT) { Push.send(this@PushService, token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val app = applicationContext as CiventhApp
        if (!app.auth.hasSession) return
        runBlocking {
            withTimeoutOrNull(PUSH_WAIT) { MessageNotifier(this@PushService).sync(needBaseline = false) }
        }
    }
}
