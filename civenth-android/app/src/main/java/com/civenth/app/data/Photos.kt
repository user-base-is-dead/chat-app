package com.civenth.app.data

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.civenth.app.BuildConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val CACHE_SIZE = 48
private const val TARGET_SIZE = 320

object Photos {
    private val http = OkHttpClient()
    private val cache = LruCache<String, ImageBitmap>(CACHE_SIZE)

    suspend fun load(path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        val image = withContext(Dispatchers.IO) { fetch(path) } ?: return null
        cache.put(path, image)
        return image
    }

    private fun fetch(path: String): ImageBitmap? = try {
        val request = Request.Builder().url("${BuildConfig.API_URL}$path").build()
        http.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes()
            if (!response.isSuccessful || bytes == null) null else decode(bytes)
        }
    } catch (_: IOException) {
        null
    }

    private fun decode(bytes: ByteArray): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_SIZE && bounds.outHeight / (sample * 2) >= TARGET_SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }
}
