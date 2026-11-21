package com.mironchat.app.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.mironchat.app.BuildConfig
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

const val APP_PLATFORM = "android"
val APP_VERSION: String get() = BuildConfig.VERSION_CODE.toString()
private const val PLAY_STORE = "com.android.vending"
private const val UPDATE_FILE = "miron-update.apk"

data class Release(
    val code: Int,
    val name: String,
    val notes: String,
    val downloadUrl: String,
    val playUrl: String,
)

enum class DownloadState { Idle, Downloading, Ready, Failed }

enum class AlertKind { Update, Notice }

data class AppAlert(
    val id: String,
    val kind: AlertKind,
    val title: String,
    val body: String,
    val at: Long,
    val versionCode: Int = 0,
)

private const val ALERT_PREFS = "app_alerts"
private const val SEEN_AT = "seen_at"
private const val NOTICES = "notices"

object AppUpdate {
    var required by mutableStateOf(false)
    var latest by mutableStateOf<Release?>(null)
        private set
    var flags by mutableStateOf(JSONObject())
        private set
    var dismissed by mutableStateOf(false)
    var download by mutableStateOf(DownloadState.Idle)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var alerts by mutableStateOf(emptyList<AppAlert>())
        private set
    var seenAt by mutableStateOf(0L)
        private set
    var pageOpen by mutableStateOf(false)
    private var context: Context? = null

    val unread: Int get() = alerts.count { isNew(it, seenAt) }

    fun isNew(alert: AppAlert, since: Long): Boolean =
        alert.at > since && (alert.kind == AlertKind.Notice || alert.versionCode > BuildConfig.VERSION_CODE)

    fun attach(app: Context) {
        context = app.applicationContext
        seenAt = prefs()?.getLong(SEEN_AT, 0L) ?: 0L
    }

    private fun prefs() = context?.getSharedPreferences(ALERT_PREFS, Context.MODE_PRIVATE)

    fun markSeen() {
        seenAt = maxOf(System.currentTimeMillis(), alerts.maxOfOrNull { it.at } ?: 0L)
        prefs()?.edit()?.putLong(SEEN_AT, seenAt)?.apply()
    }

    private fun notices(current: String): List<Pair<String, Long>> {
        val store = prefs() ?: return emptyList()
        val saved = try {
            org.json.JSONArray(store.getString(NOTICES, "[]"))
        } catch (_: JSONException) {
            org.json.JSONArray()
        }
        val list = (0 until saved.length()).mapNotNull { index ->
            saved.optJSONObject(index)?.let { it.optString("text") to it.optLong("at") }
        }.toMutableList()
        if (current.isNotBlank() && list.none { it.first == current }) {
            list.add(current to System.currentTimeMillis())
            val out = org.json.JSONArray()
            list.takeLast(30).forEach { (text, at) -> out.put(JSONObject().put("text", text).put("at", at)) }
            store.edit().putString(NOTICES, out.toString()).apply()
        }
        return list.takeLast(30)
    }

    val available: Boolean get() = (latest?.code ?: 0) > BuildConfig.VERSION_CODE
    val notice: String get() = flags.optString("notice")

    fun enabled(feature: String): Boolean = flags.optBoolean(feature, true)

    suspend fun check(http: OkHttpClient) {
        val body = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${BuildConfig.API_URL}/v1/app/version?platform=$APP_PLATFORM")
                    .build()
                http.newCall(request).execute().use { if (it.isSuccessful) it.body?.string() else null }
            } catch (_: IOException) {
                null
            }
        } ?: return
        try {
            val json = JSONObject(body)
            val code = BuildConfig.VERSION_CODE
            val blocked = json.optJSONArray("blocked")
            val isBlocked = blocked != null && (0 until blocked.length()).any { blocked.optInt(it) == code }
            required = code < json.optInt("minVersion", 0) || isBlocked
            flags = json.optJSONObject("flags") ?: JSONObject()
            val releases = json.optJSONArray("releases")
            val updates = (0 until (releases?.length() ?: 0)).mapNotNull { index ->
                releases?.optJSONObject(index)?.let {
                    val name = it.optString("versionName")
                    AppAlert(
                        id = "release-${it.optInt("versionCode")}",
                        kind = AlertKind.Update,
                        title = "Miron Chat $name",
                        body = it.optString("notes").ifEmpty { "A new version of Miron Chat is ready." },
                        at = it.optLong("createdAt"),
                        versionCode = it.optInt("versionCode"),
                    )
                }
            }
            val notes = notices(flags.optString("notice")).map { (text, at) ->
                AppAlert(id = "notice-$at", kind = AlertKind.Notice, title = "Message from Miron Chat", body = text, at = at)
            }
            alerts = (updates + notes).sortedByDescending { it.at }
            latest = json.optJSONObject("latest")?.let {
                Release(
                    code = it.optInt("versionCode"),
                    name = it.optString("versionName"),
                    notes = it.optString("notes"),
                    downloadUrl = it.optString("downloadUrl"),
                    playUrl = it.optString("playUrl"),
                )
            }
        } catch (_: JSONException) {
        }
    }

    fun fromPlayStore(context: Context): Boolean {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
            null
        }
        return installer == PLAY_STORE
    }

    fun openPlayStore(context: Context) {
        val release = latest
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            val web = release?.playUrl?.ifEmpty { null } ?: "https://play.google.com/store/apps/details?id=${context.packageName}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    suspend fun start(context: Context, http: OkHttpClient) {
        val release = latest
        if (fromPlayStore(context) || release == null || release.downloadUrl.isEmpty()) {
            openPlayStore(context)
            return
        }
        if (download == DownloadState.Ready) {
            install(context)
            return
        }
        if (download == DownloadState.Downloading) return
        download = DownloadState.Downloading
        progress = 0f
        val file = File(context.cacheDir, UPDATE_FILE)
        val ok = withContext(Dispatchers.IO) {
            try {
                http.newCall(Request.Builder().url(release.downloadUrl).build()).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) return@use false
                    val total = body.contentLength()
                    file.outputStream().use { out ->
                        val input = body.byteStream()
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            done += read
                            if (total > 0) progress = done.toFloat() / total
                        }
                    }
                    true
                }
            } catch (_: IOException) {
                false
            }
        }
        download = if (ok) DownloadState.Ready else DownloadState.Failed
        if (ok) install(context)
    }

    fun install(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return
        }
        val file = File(context.cacheDir, UPDATE_FILE)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
