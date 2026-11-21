package com.mironchat.app.alerts

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
import com.mironchat.app.MainActivity
import com.mironchat.app.MironApp
import com.mironchat.app.R
import com.mironchat.app.data.ApiError
import com.mironchat.app.data.Chat
import com.mironchat.app.data.RealtimeClient
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

private const val STATUS_CHANNEL = "connection_quiet"
private const val OLD_STATUS_CHANNEL = "connection"
private const val MESSAGE_CHANNEL = "messages"
private const val STATUS_ID = 1
private const val MESSAGE_ID = 2
private const val SYNC_WAIT = 300L
private const val PREFS = "alerts"
private const val BASELINE = "baseline"
private const val PUSH_READY = "push"
const val EXTRA_CHAT_ID = "chatId"

private val syncEvents = setOf("ready", "message.new", "message.read", "message.deleted", "chat.changed", "chat.deleted")

object AppState {
    var visible = false
}

object AlertRoute {
    var chatId by mutableStateOf<String?>(null)
}

object MessageAlerts {
    fun begin(context: Context) {
        if (pushReady(context)) {
            pause(context)
            return
        }
        val intent = Intent(context, MessageAlertService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: IllegalStateException) {
        }
    }

    fun pause(context: Context) {
        context.stopService(Intent(context, MessageAlertService::class.java))
    }

    fun stop(context: Context) {
        pause(context)
        NotificationManagerCompat.from(context).cancelAll()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun pushReady(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PUSH_READY, false)

    fun setPushReady(context: Context, ready: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PUSH_READY, ready).apply()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as MironApp
        if (app.auth.hasSession) MessageAlerts.begin(context)
    }
}

class MessageNotifier(private val context: Context) {
    private val app get() = context.applicationContext as MironApp
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
        if (!manager.areNotificationsEnabled()) return
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
            manager.deleteNotificationChannel(OLD_STATUS_CHANNEL)
            manager.createNotificationChannel(
                NotificationChannel(STATUS_CHANNEL, "Connection", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(MESSAGE_CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }
}

class MessageAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val app get() = application as MironApp
    private lateinit var notifier: MessageNotifier
    private lateinit var realtime: RealtimeClient
    private var watching = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifier = MessageNotifier(this)
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
            .onEach { scope.launch { if (!notifier.sync(needBaseline = true)) stopSelf() } }
            .launchIn(scope)
    }

    private fun statusNotification() = NotificationCompat.Builder(this, STATUS_CHANNEL)
        .setSmallIcon(R.drawable.ic_chats)
        .setContentTitle("Miron Chat")
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        .setShowWhen(false)
        .setOngoing(false)
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
}
