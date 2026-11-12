package com.meghnathnews.mironchat

import android.app.Application
import com.meghnathnews.mironchat.data.ApiClient
import com.meghnathnews.mironchat.data.AuthRepository
import com.meghnathnews.mironchat.data.ChatRepository
import com.meghnathnews.mironchat.data.RealtimeClient
import com.meghnathnews.mironchat.data.RoomRepository
import com.meghnathnews.mironchat.data.TokenStore
import com.meghnathnews.mironchat.ui.theme.Appearance

class MironApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Appearance.load(this)
    }

    private val tokens by lazy { TokenStore(this) }
    private val api by lazy { ApiClient(tokens) }

    val auth by lazy { AuthRepository(api, tokens) }
    val chats by lazy { ChatRepository(api) }
    val rooms by lazy { RoomRepository(api, chats) }

    fun newRealtime(background: Boolean = false) = RealtimeClient(api, tokens, background)
}
