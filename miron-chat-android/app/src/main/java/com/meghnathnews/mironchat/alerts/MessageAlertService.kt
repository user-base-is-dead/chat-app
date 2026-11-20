package com.meghnathnews.mironchat.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.meghnathnews.mironchat.MainActivity
import com.meghnathnews.mironchat.MironApp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val STATUS_CHANNEL = "connection"
private const val MESSAGE_CHANNEL = "messages"
private const val STATUS_ID = 1
private const val MESSAGE_ID = 2
private const val SYNC_WAIT = 300L
private const val PREFS = "alerts"
private const val BASELINE = "baseline"
const val EXTRA_CHAT_ID = "chatId"

private val syncEvents = setOf("ready", "message.new", "message.read", "message.deleted", "chat.changed", "chat.deleted")

object AppState {
    var visible = false
}

object AlertRoute {
    var chatId by mutableStateOf<String?>(null)
}

object MessageAlerts {
    fun start(context: Context) {
        val intent = Intent(context, MessageAlertService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: IllegalStateException) {
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, MessageAlertService::class.java))
        NotificationManagerCompat.from(context).cancelAll()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as MironApp
        if (app.auth.hasSession) MessageAlerts.start(context)
    }
}

class MessageAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val app get() = application as MironApp
    private val seen by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private lateinit var realtime: RealtimeClient
    private var watching = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        realtime = app.newRealtime(background = true)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        } else {
            0
        }
        ServiceCompat.startForeground(this, STATUS_ID, statusNotification(), type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!app.auth.hasSession) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!watching) {
            watching = true
            watchEvents()
            realtime.start(scope)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        realtime.stop()
        scope.cancel()
        super.onDestroy()
    }

    @OptIn(FlowPreview::class)
    private fun watchEvents() {
        realtime.events
            .filter { it.name in syncEvents || it.name == "session.check" }
            .debounce(SYNC_WAIT)
            .onEach { sync() }
            .launchIn(scope)
    }

    private fun sync() {
        scope.launch {
            try {
                val chats = app.chats.list()
                val baseline = seen.getBoolean(BASELINE, false)
                val editor = seen.edit()
                chats.forEach { chat ->
                    val last = chat.lastAt ?: return@forEach
                    if (chat.unreadCount == 0) NotificationManagerCompat.from(this@MessageAlertService).cancel(chat.id, MESSAGE_ID)
                    if (baseline && last > seen.getLong(chat.id, 0L) && shouldAlert(chat)) post(chat, titleOf(chat))
                    editor.putLong(chat.id, last)
                }
                editor.putBoolean(BASELINE, true).apply()
            } catch (error: ApiError) {
                if (error.status == 401 && !app.auth.hasSession) stopSelf()
            }
        }
    }

    private fun shouldAlert(chat: Chat) =
        !AppState.visible && !chat.muted && chat.status != "restricted" && chat.unreadCount > 0 &&
            !(chat.isRoom && chat.status == "request")

    private suspend fun titleOf(chat: Chat): String {
        if (chat.kind != "group" || chat.lastChatId.isEmpty()) return chat.name
        return try {
            val channel = app.channels.list(chat.id).channels.firstOrNull { it.id == chat.lastChatId }
            if (channel != null) "${chat.name} \u203A #${channel.name}" else chat.name
        } catch (_: ApiError) {
            chat.name
        }
    }

    private fun post(chat: Chat, title: String) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val open = Intent(this, MainActivity::class.java)
            .putExtra(EXTRA_CHAT_ID, chat.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(
            this,
            chat.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (chat.kind == "group" && chat.lastSender.isNotEmpty()) "${chat.lastSender}: ${chat.lastText}" else chat.lastText.orEmpty()
        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL)
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

    private fun statusNotification() = NotificationCompat.Builder(this, STATUS_CHANNEL)
        .setSmallIcon(R.drawable.ic_chats)
        .setContentTitle("Miron Chat is running")
        .setContentText("Keeps you connected for new messages")
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, "Connection", NotificationManager.IMPORTANCE_MIN),
        )
        manager.createNotificationChannel(
            NotificationChannel(MESSAGE_CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH),
        )
    }
}
