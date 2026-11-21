package com.mironchat.app.ui.chats

import android.content.Context

object SearchHistory {
    private const val PREFS = "chat_search"
    private const val RECENT = "recent"
    private const val LIMIT = 15

    fun load(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECENT, "").orEmpty()
            .split(',').filter { it.isNotEmpty() }

    fun save(context: Context, ids: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(RECENT, ids.take(LIMIT).joinToString(",")).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
