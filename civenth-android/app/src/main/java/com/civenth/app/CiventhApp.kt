package com.civenth.app

import android.app.Application
import com.civenth.app.data.ApiClient
import com.civenth.app.data.AuthRepository
import com.civenth.app.data.ChatRepository
import com.civenth.app.data.RealtimeClient
import com.civenth.app.data.ChannelRepository
import com.civenth.app.data.PostRepository
import com.civenth.app.data.RoleRepository
import com.civenth.app.data.RoomRepository
import com.civenth.app.data.TokenStore
import com.civenth.app.ui.theme.Appearance

class CiventhApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Appearance.load(this)
    }

    private val tokens by lazy { TokenStore(this) }
    private val api by lazy { ApiClient(tokens) }

    val http get() = api.http
    val auth by lazy { AuthRepository(api, tokens) }
    val chats by lazy { ChatRepository(api) }
    val rooms by lazy { RoomRepository(api, chats) }
    val roles by lazy { RoleRepository(api) }
    val channels by lazy { ChannelRepository(api, getSharedPreferences("channels", MODE_PRIVATE)) }
    val posts by lazy { PostRepository(api) }

    fun newRealtime(background: Boolean = false) = RealtimeClient(api, tokens, background)
}
