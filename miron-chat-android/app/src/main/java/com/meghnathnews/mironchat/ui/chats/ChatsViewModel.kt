package com.meghnathnews.mironchat.ui.chats

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meghnathnews.mironchat.MironApp
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.Person
import com.meghnathnews.mironchat.data.RoomPreview
import com.meghnathnews.mironchat.data.User
import com.meghnathnews.mironchat.ui.chat.ChatRoom
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val REFRESH_WAIT = 250L
private const val PIN_LIMIT = 5

private val listEvents = setOf(
    "ready",
    "message.new",
    "message.edited",
    "message.deleted",
    "message.read",
    "chat.changed",
    "chat.deleted",
    "room.changed",
    "room.removed",
    "presence",
    "session.check",
)

private val byRecent = compareByDescending<Chat> { it.lastAt ?: 0L }
private val byPinned = compareByDescending<Chat> { it.pinnedAt ?: 0L }.then(byRecent)

data class JoinState(
    val key: String,
    val room: RoomPreview? = null,
    val error: String = "",
    val busy: Boolean = false,
)

class ChatsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MironApp
    private val realtime = app.newRealtime()

    private var all by mutableStateOf(emptyList<Chat>())

    var loading by mutableStateOf(true)
        private set
    var failed by mutableStateOf(false)
        private set
    var sessionLost by mutableStateOf(false)
        private set
    var search by mutableStateOf("")
    var wantSearch by mutableStateOf(false)
    var room by mutableStateOf<ChatRoom?>(null)
        private set
    var deleting by mutableStateOf<Chat?>(null)
        private set
    var leaving by mutableStateOf<Chat?>(null)
        private set
    var joining by mutableStateOf<JoinState?>(null)
        private set
    var roomTick by mutableStateOf(0)
        private set

    val rooms get() = app.rooms

    val openedChat by derivedStateOf { all.firstOrNull { it.id == room?.chatId } }
    val active by derivedStateOf { all.filter { !it.isRoom && it.status == "active" }.sortedWith(byPinned) }
    val requests by derivedStateOf { all.filter { !it.isRoom && it.status == "request" } }
    val restricted by derivedStateOf { all.filter { !it.isRoom && it.status == "restricted" }.sortedWith(byRecent) }
    val groups by derivedStateOf { all.filter { it.kind == "group" }.sortedWith(byPinned) }
    val channels by derivedStateOf { all.filter { it.kind == "channel" }.sortedWith(byPinned) }
    val knownPeople by derivedStateOf {
        active.filter { !it.peerDeleted && !it.peerDisabled }.map { Person(it.peerId, it.username, it.name) }
    }
    val canPin by derivedStateOf { all.count { it.pinnedAt != null } < PIN_LIMIT }
    val unreadChats by derivedStateOf { active.count { it.unreadCount > 0 && !it.muted } }
    val searchNeedle by derivedStateOf { search.trim().removePrefix("@") }
    val chatResults by derivedStateOf {
        all.filter {
            !(it.isRoom && it.status == "request") &&
                (it.name.contains(searchNeedle, ignoreCase = true) || it.username.contains(searchNeedle, ignoreCase = true))
        }.sortedWith(byRecent)
    }
    val messageResults by derivedStateOf {
        all.filter { it.lastText?.contains(searchNeedle, ignoreCase = true) == true }.sortedWith(byRecent)
    }

    init {
        watchEvents()
    }

    fun start() {
        refresh()
        realtime.start(viewModelScope)
    }

    fun stop() {
        realtime.stop()
    }

    fun openChat(chat: Chat, user: User, focusInput: Boolean = false, focusId: String? = null) {
        room?.close()
        room = ChatRoom(
            opened = chat,
            userId = user.id,
            repository = app.chats,
            rooms = app.rooms,
            realtime = realtime,
            parent = viewModelScope,
            onChatGone = {
                closeChat()
                refresh()
            },
            onChatChanged = ::refresh,
            focusInput = focusInput,
            focusId = focusId,
        )
    }

    fun togglePin(chat: Chat) = act {
        if (chat.isRoom) app.rooms.setPinned(chat.id, chat.pinnedAt == null)
        else app.chats.setPinned(chat.id, chat.pinnedAt == null)
    }

    fun toggleMute(chat: Chat) = act {
        if (chat.isRoom) app.rooms.setMuted(chat.id, !chat.muted)
        else app.chats.setMuted(chat.id, !chat.muted)
    }

    fun acceptInvite(chat: Chat) = act { app.rooms.accept(chat.id) }

    fun declineInvite(chat: Chat) = act { app.rooms.decline(chat.id) }

    fun askLeave(chat: Chat) {
        leaving = chat
    }

    fun cancelLeave() {
        leaving = null
    }

    fun confirmLeave() {
        val chat = leaving ?: return
        leaving = null
        act { app.rooms.leave(chat.id) }
    }

    fun refreshNow() = refresh()

    fun roomCreated(chat: Chat, user: User) {
        all = all.filterNot { it.id == chat.id } + chat
        openChat(chat, user, focusInput = chat.canSend)
    }

    fun openPreview(preview: RoomPreview, user: User) {
        val known = all.firstOrNull { it.id == preview.id && it.status == "active" }
        if (known != null) openChat(known, user) else joining = JoinState(preview.username, preview)
    }

    fun openKey(key: String, user: User) {
        viewModelScope.launch {
            try {
                val preview = app.rooms.preview(key)
                val known = all.firstOrNull { it.id == preview.id && it.status == "active" }
                if (known != null) openChat(known, user) else joining = JoinState(key, preview)
            } catch (error: ApiError) {
                joining = JoinState(key, error = error.message.orEmpty())
            }
        }
    }

    fun confirmJoin(user: User) {
        val state = joining ?: return
        joining = state.copy(busy = true, error = "")
        viewModelScope.launch {
            try {
                val chat = app.rooms.join(state.key)
                all = all.filterNot { it.id == chat.id } + chat
                joining = null
                openChat(chat, user)
            } catch (error: ApiError) {
                joining = state.copy(busy = false, error = error.message.orEmpty())
            }
        }
    }

    fun closeJoin() {
        joining = null
    }

    fun restrict(chat: Chat) = act { app.chats.restrict(chat.id) }

    fun unrestrict(chat: Chat) = act { app.chats.unrestrict(chat.id) }

    fun askDelete(chat: Chat) {
        deleting = chat
    }

    fun cancelDelete() {
        deleting = null
    }

    fun confirmDelete() {
        val chat = deleting ?: return
        deleting = null
        act { if (chat.isRoom) app.rooms.delete(chat.id) else app.chats.deleteChat(chat.id) }
    }

    private fun act(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                refresh()
            } catch (_: ApiError) {
            }
        }
    }

    suspend fun searchUsers(query: String): List<Person> = app.chats.searchUsers(query)

    fun startChat(person: Person, user: User) {
        viewModelScope.launch {
            try {
                val chat = app.chats.openDm(person.id)
                all = all.filterNot { it.id == chat.id } + chat
                openChat(chat, user, focusInput = true)
            } catch (_: ApiError) {
                failed = true
            }
        }
    }

    fun chatById(id: String): Chat? = all.firstOrNull { it.id == id }

    fun closeChat() {
        room?.close()
        room = null
    }

    @OptIn(FlowPreview::class)
    private fun watchEvents() {
        realtime.events
            .filter { it.name in listEvents }
            .debounce(REFRESH_WAIT)
            .onEach { refresh() }
            .launchIn(viewModelScope)
        realtime.events
            .filter { it.name == "room.changed" }
            .onEach { roomTick += 1 }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                val list = app.chats.list()
                all = list
                failed = false
                val current = room
                val opened = list.firstOrNull { it.id == current?.chatId }
                if (current != null && opened == null) closeChat()
                if (opened != null) current?.chatUpdated(opened)
            } catch (error: ApiError) {
                if (error.status == 401) sessionLost = true else failed = true
            } finally {
                loading = false
            }
        }
    }
}
