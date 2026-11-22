package com.civenth.app.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.civenth.app.MainActivity
import com.civenth.app.CiventhApp
import com.civenth.app.R
import com.civenth.app.data.ApiError
import com.civenth.app.data.Chat

private val OLD_STATUS_CHANNELS = listOf("connection", "connection_quiet")
private const val MESSAGE_CHANNEL = "messages"
private const val MESSAGE_ID = 2
private const val PREFS = "alerts"
private const val BASELINE = "baseline"
private const val SETTINGS = "alert_settings"
private const val ENABLED = "enabled"
const val EXTRA_CHAT_ID = "chatId"

object AppState {
    var visible = false
}

object AlertRoute {
    var chatId by mutableStateOf<String?>(null)
}

object MessageAlerts {
    fun stop(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun enabled(context: Context) =
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, on).apply()
        if (!on) NotificationManagerCompat.from(context).cancelAll()
    }
}

class MessageNotifier(private val context: Context) {
    private val app get() = context.applicationContext as CiventhApp
    private val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        createChannels(context)
    }

    suspend fun sync(needBaseline: Boolean): Boolean {
        return try {
            val chats = app.chats.list()
            val baseline = !needBaseline || seen.getBoolean(BASELINE, false)
            val editor = seen.edit()
            chats.forEach { chat ->
                val last = chat.lastAt ?: return@forEach
                if (chat.unreadCount == 0) NotificationManagerCompat.from(context).cancel(chat.id, MESSAGE_ID)
                if (baseline && last > seen.getLong(chat.id, 0L) && shouldAlert(chat)) post(chat, titleOf(chat))
                editor.putLong(chat.id, last)
            }
            editor.putBoolean(BASELINE, true).apply()
            true
        } catch (error: ApiError) {
            !(error.status == 401 && !app.auth.hasSession)
        }
    }

    private fun shouldAlert(chat: Chat) =
        !AppState.visible && !chat.muted && chat.status != "restricted" && chat.unreadCount > 0 &&
            !(chat.isRoom && chat.status == "request")

    private suspend fun titleOf(chat: Chat): String {
        if (chat.kind != "group" || chat.lastChatId.isEmpty()) return chat.name
        return try {
            val channel = app.channels.list(chat.id).channels.firstOrNull { it.id == chat.lastChatId }
            if (channel != null) "${chat.name} › #${channel.name}" else chat.name
        } catch (_: ApiError) {
            chat.name
        }
    }

    private fun post(chat: Chat, title: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !MessageAlerts.enabled(context)) return
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_CHAT_ID, chat.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(
            context,
            chat.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (chat.kind == "group" && chat.lastSender.isNotEmpty()) "${chat.lastSender}: ${chat.lastText}" else chat.lastText.orEmpty()
        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_chats)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(if (chat.unreadCount > 1) "${chat.unreadCount} new messages" else null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        try {
            manager.notify(chat.id, MESSAGE_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        fun createChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            OLD_STATUS_CHANNELS.forEach(manager::deleteNotificationChannel)
            manager.createNotificationChannel(
                NotificationChannel(MESSAGE_CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }
}
