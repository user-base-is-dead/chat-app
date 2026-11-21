package com.mironchat.app

import android.app.Application
import com.mironchat.app.data.ApiClient
import com.mironchat.app.data.AuthRepository
import com.mironchat.app.data.ChatRepository
import com.mironchat.app.data.RealtimeClient
import com.mironchat.app.data.ChannelRepository
import com.mironchat.app.data.PostRepository
import com.mironchat.app.data.RoleRepository
import com.mironchat.app.data.RoomRepository
import com.mironchat.app.data.TokenStore
import com.mironchat.app.ui.theme.Appearance

class MironApp : Application() {
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
    val posts by lazy { PostRepository(api, getSharedPreferences("posts", MODE_PRIVATE)) }

    fun newRealtime(background: Boolean = false) = RealtimeClient(api, tokens, background)
}
