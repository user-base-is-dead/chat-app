package com.civenth.app.ui.chats

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.civenth.app.CiventhApp
import com.civenth.app.data.ApiError
import com.civenth.app.data.ChannelList
import com.civenth.app.data.Chat
import com.civenth.app.data.Presence
import com.civenth.app.data.RoomChannel
import com.civenth.app.data.Person
import com.civenth.app.data.RoomPreview
import com.civenth.app.data.User
import com.civenth.app.ui.chat.ChatRoom
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.civenth.app.update.APP_PLATFORM
import com.civenth.app.update.AppUpdate

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
    private val app = application as CiventhApp
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
    var searchOpen by mutableStateOf(false)
    private var recentIds by mutableStateOf(SearchHistory.load(application))
    val recentChats by derivedStateOf { recentIds.mapNotNull { id -> all.firstOrNull { it.id == id } } }

    fun openSearch() {
        searchOpen = true
    }

    fun closeSearch() {
        searchOpen = false
        search = ""
    }

    fun remember(chat: Chat) {
        recentIds = listOf(chat.id) + recentIds.filter { it != chat.id }
        SearchHistory.save(app, recentIds)
    }

    fun forget(chat: Chat) {
        recentIds = recentIds.filter { it != chat.id }
        SearchHistory.save(app, recentIds)
    }

    fun forgetAll() {
        recentIds = emptyList()
        SearchHistory.save(app, recentIds)
    }
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
    val presence = mutableStateMapOf<String, Presence>()

    var selectedGroupId by mutableStateOf<String?>(null)
        private set
    var openGroupId by mutableStateOf<String?>(null)
        private set
    var openChannelName by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
    val channelLists = mutableStateMapOf<String, ChannelList>()

    val rooms get() = app.rooms
    val channelStore get() = app.channels

    val openedChat by derivedStateOf { all.firstOrNull { it.id == (openGroupId ?: room?.chatId) } }
    val active by derivedStateOf { all.filter { !it.isRoom && it.status == "active" }.sortedWith(byPinned) }
    val requests by derivedStateOf { all.filter { !it.isRoom && it.status == "request" } }
    val restricted by derivedStateOf { all.filter { !it.isRoom && it.status == "restricted" }.sortedWith(byRecent) }
    val groups by derivedStateOf { all.filter { it.kind == "group" }.sortedWith(byPinned) }
    val shownGroup by derivedStateOf {
        groups.firstOrNull { it.id == selectedGroupId } ?: groups.firstOrNull { it.status == "active" } ?: groups.firstOrNull()
    }
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
        refreshJob?.cancel()
        realtime.stop()
    }

    fun selectGroup(group: Chat) {
        selectedGroupId = group.id
        if (group.status == "active") loadChannels(group.id)
    }

    fun loadChannels(roomId: String) {
        viewModelScope.launch {
            try {
                val list = app.channels.list(roomId)
                channelLists[roomId] = list
                val current = room
                val inGroup = openGroupId == roomId || current?.chatId == roomId
                if (current != null && inGroup && list.channels.none { it.id == current.chatId }) {
                    closeChat()
                    notice = "You no longer have access to this channel"
                }
            } catch (_: ApiError) {
            }
        }
    }

    fun manageChannels(roomId: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (error: ApiError) {
                notice = error.message
            }
            loadChannels(roomId)
        }
    }

    fun openChannel(group: Chat, channel: RoomChannel, user: User) {
        app.channels.saveOpenChannel(group.id, channel.id)
        if (channel.id == group.id) {
            openChat(group, user)
            return
        }
        room?.close()
        selectedGroupId = group.id
        openGroupId = group.id
        openChannelName = channel.name
        room = ChatRoom(
            opened = group.copy(
                id = channel.id,
                lastReadId = channel.lastReadId,
                peerLastReadId = channel.readUpTo,
                unreadCount = channel.unreadCount,
            ),
            userId = user.id,
            repository = app.chats,
            rooms = app.rooms,
            posts = app.posts,
            realtime = realtime,
            parent = viewModelScope,
            onChatGone = {
                closeChat()
                refresh()
            },
            onChatChanged = ::refresh,
            groupId = group.id,
        )
    }

    fun openChat(chat: Chat, user: User, focusInput: Boolean = false, focusId: String? = null) {
        room?.close()
        openGroupId = null
        openChannelName = if (chat.kind == "group") channelLists[chat.id]?.channels?.firstOrNull { it.id == chat.id }?.name ?: "general" else null
        if (chat.kind == "group") selectedGroupId = chat.id
        room = ChatRoom(
            opened = chat,
            userId = user.id,
            repository = app.chats,
            rooms = app.rooms,
            posts = app.posts,
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
        openGroupId = null
        openChannelName = null
    }

    @OptIn(FlowPreview::class)
    private fun watchEvents() {
        realtime.events
            .filter { it.name in listEvents }
            .debounce(REFRESH_WAIT)
            .onEach { refresh() }
            .launchIn(viewModelScope)
        realtime.events
            .filter { it.name == "presence" }
            .onEach { event ->
                val data = event.data ?: return@onEach
                presence[data.optString("userId")] = Presence(data.optBoolean("online"), data.optBoolean("hidden"), System.currentTimeMillis())
            }
            .launchIn(viewModelScope)
        realtime.events
            .filter { it.name == "ready" || (it.name == "app.version" && it.data?.optString("platform") == APP_PLATFORM) }
            .onEach { viewModelScope.launch { AppUpdate.check(app.http) } }
            .launchIn(viewModelScope)
        realtime.events
            .filter { it.name == "room.changed" }
            .onEach { roomTick += 1 }
            .launchIn(viewModelScope)
        realtime.events
            .filter { it.name == "room.channels" }
            .onEach { event -> event.data?.optString("chatId")?.takeIf { it.isNotEmpty() }?.let(::loadChannels) }
            .launchIn(viewModelScope)
    }

    var syncError by mutableStateOf("")
        private set
    private var refreshJob: kotlinx.coroutines.Job? = null

    private suspend fun loadWithRetry(onPhase: (String) -> Unit): List<Chat> {
        var retries = 0
        while (true) {
            try {
                return app.chats.list { onPhase("Updating…") }
            } catch (error: ApiError) {
                if (retries >= 3 || (error.code != "network" && error.status !in 500..599)) throw error
                onPhase("Reconnecting…")
                kotlinx.coroutines.delay(1000L shl retries)
                retries++
            }
        }
    }

    private var refreshSequence = 0
    var syncLabel by mutableStateOf("")
        private set

    private fun refresh() {
        val request = ++refreshSequence
        refreshJob?.cancel()
        syncError = ""
        failed = false
        refreshJob = viewModelScope.launch {
            var phase = "Loading…"
            val statusTimer = launch { kotlinx.coroutines.delay(220); if (request == refreshSequence) syncLabel = phase }
            try {
                val list = loadWithRetry { nextPhase ->
                    phase = nextPhase
                    if (request == refreshSequence && statusTimer.isCompleted) syncLabel = phase
                }
                if (request != refreshSequence) return@launch
                syncLabel = ""
                all = list
                failed = false
                val current = room
                val opened = list.firstOrNull { it.id == (openGroupId ?: current?.chatId) }
                if (current != null && opened == null) closeChat()
                if (opened != null && openGroupId == null) current?.chatUpdated(opened)
                list.firstOrNull { it.id == shownGroup?.id && it.status == "active" }?.let { loadChannels(it.id) }
            } catch (error: ApiError) {
                if (request != refreshSequence) return@launch
                syncLabel = ""
                if (error.status != 401) syncError = if (error.code == "network") "Cannot reach the server." else "Could not update your chats."
                if (error.status == 401) sessionLost = true else failed = true
            } finally {
                statusTimer.cancel()
                if (request == refreshSequence) loading = false
            }
        }
    }
}
