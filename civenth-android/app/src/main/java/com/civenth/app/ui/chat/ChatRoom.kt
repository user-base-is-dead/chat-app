package com.civenth.app.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.civenth.app.data.ApiError
import com.civenth.app.data.Chat
import com.civenth.app.data.ChatRepository
import com.civenth.app.data.ForwardRef
import com.civenth.app.data.ReplyRef
import com.civenth.app.data.MESSAGE_PAGE_SIZE
import com.civenth.app.data.PostRepository
import com.civenth.app.data.PostStats
import com.civenth.app.data.PostSwitches
import com.civenth.app.data.VIEW_BATCH
import com.civenth.app.data.toReactions
import com.civenth.app.data.RealtimeClient
import com.civenth.app.data.RealtimeEvent
import com.civenth.app.data.RoomRepository
import com.civenth.app.data.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TYPING_SEND_GAP = 3_000L
private const val TYPING_SHOW_FOR = 6_000L
private const val READ_DELAY = 300L
private const val ONLINE_REFRESH = 30_000L
private const val SEARCH_WAIT = 300L

enum class SendState { Sending, Sent, Failed }

data class Message(
    val key: String,
    val id: String,
    val text: String,
    val createdAt: Long,
    val mine: Boolean,
    val state: SendState,
    val live: Boolean = false,
    val senderId: String = "",
    val senderName: String = "",
    val editedAt: Long? = null,
    val replyTo: ReplyRef? = null,
    val forwardedFrom: ForwardRef? = null,
) {
    val temp: Boolean get() = id.startsWith("temp-")
    val number: Long get() = id.toLongOrNull() ?: Long.MAX_VALUE
}

class ChatRoom(
    opened: Chat,
    private val userId: String,
    private val repository: ChatRepository,
    private val rooms: RoomRepository,
    val posts: PostRepository,
    private val realtime: RealtimeClient,
    parent: CoroutineScope,
    private val onChatGone: () -> Unit,
    private val onChatChanged: () -> Unit,
    val focusInput: Boolean = false,
    val focusId: String? = null,
    private val groupId: String? = null,
) {
    private val scope = CoroutineScope(parent.coroutineContext + Job(parent.coroutineContext[Job]))
    private var tempCount = 0
    private var lastTypingSent = 0L
    private var lastRead = opened.lastReadId
    private var readJob: Job? = null
    private var typingJob: Job? = null
    private val played = mutableSetOf<String>()
    private var searchJob: Job? = null
    private val asked = mutableSetOf<String>()

    val isChannel = opened.kind == "channel"
    val postStats = mutableStateMapOf<String, PostStats>()
    var commentEvent by mutableStateOf<RealtimeEvent?>(null)
        private set
    var notice by mutableStateOf<String?>(null)

    val chatId = opened.id

    var messages by mutableStateOf(emptyList<Message>())
        private set
    var loading by mutableStateOf(true)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var loadingOlder by mutableStateOf(false)
        private set
    var hasNewer by mutableStateOf(false)
        private set
    var loadingNewer by mutableStateOf(false)
        private set
    var unreadFromId by mutableStateOf<String?>(null)
        private set
    var peerReadId by mutableStateOf(opened.peerLastReadId)
        private set
    var typing by mutableStateOf(false)
        private set
    var sentCount by mutableStateOf(0)
        private set
    var typingName by mutableStateOf<String?>(null)
        private set
    var onlineCount by mutableStateOf(0)
        private set
    var pinned by mutableStateOf<Message?>(null)
        private set
    var searchHits by mutableStateOf(emptyList<Message>())
        private set
    var searchBusy by mutableStateOf(false)
        private set
    var jump by mutableStateOf<Jump?>(null)
        private set
    private var jumpCount = 0

    init {
        realtime.events.onEach(::handleEvent).launchIn(scope)
        scope.launch {
            try {
                if (opened.unreadCount > 0) fetchUnread(opened) else fetchLatest()
            } catch (_: ApiError) {
            } finally {
                loading = false
            }
        }
        scope.launch {
            try {
                pinned = repository.pinned(chatId)?.let(::toMessage)
            } catch (_: ApiError) {
            }
        }
        if (opened.isRoom) {
            scope.launch {
                while (true) {
                    loadOnline()
                    delay(ONLINE_REFRESH)
                }
            }
        }
    }

    private suspend fun loadOnline() {
        try {
            onlineCount = rooms.detail(groupId ?: chatId).onlineCount
        } catch (_: ApiError) {
        }
    }

    fun close() {
        scope.cancel()
    }

    fun shouldPlay(message: Message): Boolean = message.live && played.add(message.key)

    fun chatUpdated(chat: Chat) {
        if (chat.peerLastReadId > peerReadId) peerReadId = chat.peerLastReadId
        if (chat.lastReadId > lastRead) lastRead = chat.lastReadId
    }

    fun loadOlder() {
        if (!hasMore || loadingOlder) return
        val oldest = messages.firstOrNull { !it.temp } ?: return
        loadingOlder = true
        scope.launch {
            try {
                val list = repository.messagesBefore(chatId, oldest.number)
                merge(list)
                hasMore = list.size == MESSAGE_PAGE_SIZE
            } catch (_: ApiError) {
            } finally {
                loadingOlder = false
            }
        }
    }

    fun loadNewer() {
        if (!hasNewer || loadingNewer) return
        val newest = messages.lastOrNull { !it.temp } ?: return
        loadingNewer = true
        scope.launch {
            try {
                val list = repository.messagesAfter(chatId, newest.number)
                merge(list)
                hasNewer = list.size == MESSAGE_PAGE_SIZE
            } catch (_: ApiError) {
            } finally {
                loadingNewer = false
            }
        }
    }

    fun jumpToLatest() {
        scope.launch {
            try {
                showLatest()
            } catch (_: ApiError) {
            }
        }
    }

    fun seen(messageId: Long) {
        if (messageId <= lastRead) return
        lastRead = messageId
        readJob?.cancel()
        readJob = scope.launch {
            delay(READ_DELAY)
            try {
                repository.markRead(chatId, messageId)
            } catch (_: ApiError) {
            }
        }
    }

    fun typed() {
        val now = System.currentTimeMillis()
        if (now - lastTypingSent < TYPING_SEND_GAP) return
        lastTypingSent = now
        sendTyping(true)
    }

    fun send(text: String, switches: PostSwitches? = null, replyTo: ReplyRef? = null) {
        lastTypingSent = 0
        sendTyping(false)
        scope.launch {
            if (hasNewer) {
                try {
                    showLatest()
                } catch (_: ApiError) {
                }
            }
            tempCount += 1
            val id = "temp-$tempCount"
            messages = messages + Message(id, id, text, System.currentTimeMillis(), true, SendState.Sending, live = true, replyTo = replyTo)
            sentCount += 1
            deliver(id, text, switches, replyTo?.id)
        }
    }

    fun edit(message: Message, text: String) {
        if (text == message.text || message.temp) return
        val before = message.text
        update(message.id) { it.copy(text = text, editedAt = System.currentTimeMillis()) }
        scope.launch {
            try {
                val saved = repository.edit(chatId, message.id, text)
                update(message.id) { it.copy(text = saved.text, editedAt = saved.editedAt ?: it.editedAt) }
            } catch (error: ApiError) {
                update(message.id) { it.copy(text = before, editedAt = message.editedAt) }
                notice = error.message
            }
        }
    }

    fun pin(message: Message) {
        val before = pinned
        pinned = message
        scope.launch {
            try {
                repository.pin(chatId, message.id)?.let { pinned = toMessage(it) }
            } catch (error: ApiError) {
                pinned = before
                notice = error.message
            }
        }
    }

    fun unpin() {
        val before = pinned ?: return
        pinned = null
        scope.launch {
            try {
                repository.unpin(chatId)
            } catch (error: ApiError) {
                pinned = before
                notice = error.message
            }
        }
    }

    fun forward(items: List<Message>, targetId: String, targetName: String) {
        val chosen = items.filter { !it.temp }.sortedBy { it.number }
        if (chosen.isEmpty()) return
        scope.launch {
            try {
                chosen.forEach { repository.forward(targetId, chatId, it.id) }
                notice = "Forwarded to $targetName"
                onChatChanged()
            } catch (error: ApiError) {
                notice = error.message
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchHits = emptyList()
        val needle = query.trim()
        searchBusy = needle.isNotEmpty()
        if (needle.isEmpty()) return
        searchJob = scope.launch {
            delay(SEARCH_WAIT)
            var before: Long? = null
            try {
                while (true) {
                    val page = repository.messagesBefore(chatId, before)
                    val hits = page.filter { it.text.contains(needle, ignoreCase = true) }.map(::toMessage).sortedByDescending { it.number }
                    if (hits.isNotEmpty()) searchHits = searchHits + hits
                    if (page.size < MESSAGE_PAGE_SIZE) break
                    before = page.minOf { it.id.toLong() }
                }
            } catch (_: ApiError) {
            }
            searchBusy = false
        }
    }

    fun jumpTo(messageId: String) {
        if (messages.any { it.id == messageId }) {
            showJump(messageId)
            return
        }
        val target = messageId.toLongOrNull() ?: return
        scope.launch {
            try {
                val olderCall = scope.async { repository.messagesBefore(chatId, target + 1) }
                val newer = repository.messagesAfter(chatId, target)
                val older = olderCall.await()
                if (older.none { it.id == messageId }) {
                    notice = "Message not found"
                    return@launch
                }
                messages = (older + newer).distinctBy { it.id }.map(::toMessage).sortedBy { it.number }
                hasMore = older.size == MESSAGE_PAGE_SIZE
                hasNewer = newer.size == MESSAGE_PAGE_SIZE
                askStats()
                showJump(messageId, reloaded = true)
            } catch (error: ApiError) {
                notice = error.message
            }
        }
    }

    private fun showJump(messageId: String, reloaded: Boolean = false) {
        jumpCount += 1
        jump = Jump(messageId, jumpCount, reloaded)
    }

    fun editPost(messageId: String, text: String, switches: PostSwitches) {
        val current = postStats[messageId] ?: return
        val before = messages.firstOrNull { it.id == messageId } ?: return
        scope.launch {
            try {
                if (text != before.text) {
                    val saved = repository.edit(chatId, messageId, text)
                    update(messageId) { it.copy(text = saved.text) }
                }
                if (switches.allowReactions != current.allowReactions || switches.allowComments != current.allowComments) {
                    postStats[messageId] = posts.setSwitches(chatId, messageId, switches.allowReactions, switches.allowComments)
                }
            } catch (error: ApiError) {
                notice = error.message
            }
        }
    }

    fun react(messageId: String, emoji: String) {
        val current = postStats[messageId] ?: return
        scope.launch {
            try {
                postStats[messageId] =
                    if (current.myReaction == emoji) posts.unreact(chatId, messageId) else posts.react(chatId, messageId, emoji)
            } catch (error: ApiError) {
                notice = error.message
            }
        }
    }

    fun togglePostSwitch(messageId: String, reactions: Boolean) {
        val current = postStats[messageId] ?: return
        scope.launch {
            try {
                postStats[messageId] = posts.setSwitches(
                    chatId,
                    messageId,
                    if (reactions) !current.allowReactions else current.allowReactions,
                    if (reactions) current.allowComments else !current.allowComments,
                )
            } catch (error: ApiError) {
                notice = error.message
            }
        }
    }

    private fun askStats(only: List<String>? = null) {
        if (!isChannel) return
        val ids = only ?: messages.filter { !it.temp && it.id !in asked }.map { it.id }
        if (ids.isEmpty()) return
        asked.addAll(ids)
        scope.launch {
            ids.chunked(VIEW_BATCH).forEach { batch ->
                try {
                    posts.view(chatId, batch).forEach { postStats[it.id] = it }
                } catch (_: ApiError) {
                    asked.removeAll(batch.toSet())
                }
            }
        }
    }

    fun resend(message: Message) {
        update(message.id) { it.copy(state = SendState.Sending) }
        scope.launch { deliver(message.id, message.text, if (isChannel) PostSwitches() else null, message.replyTo?.id) }
    }

    fun delete(message: Message) {
        if (message.temp) {
            messages = messages.filterNot { it.id == message.id }
            return
        }
        scope.launch {
            try {
                val chatDeleted = repository.deleteMessage(chatId, message.id)
                if (chatDeleted) onChatGone() else drop(message.id)
            } catch (_: ApiError) {
            }
        }
    }

    fun accept() = changeChat { repository.accept(chatId) }

    fun restrict() = changeChat { repository.restrict(chatId) }

    fun unrestrict() = changeChat { repository.unrestrict(chatId) }

    fun deleteChat() {
        scope.launch {
            try {
                repository.deleteChat(chatId)
                onChatGone()
            } catch (_: ApiError) {
            }
        }
    }

    private fun changeChat(change: suspend () -> Unit) {
        scope.launch {
            try {
                change()
                onChatChanged()
            } catch (_: ApiError) {
            }
        }
    }

    private suspend fun fetchLatest() {
        val list = repository.messagesBefore(chatId)
        merge(list)
        hasMore = list.size == MESSAGE_PAGE_SIZE
    }

    private suspend fun showLatest() {
        val list = repository.messagesBefore(chatId)
        messages = list.map(::toMessage).sortedBy { it.number }
        hasMore = list.size == MESSAGE_PAGE_SIZE
        hasNewer = false
        askStats()
    }

    private suspend fun fetchUnread(opened: Chat) {
        val neverRead = opened.lastReadId == 0L
        val olderCall = scope.async {
            if (neverRead) emptyList() else repository.messagesBefore(chatId, opened.lastReadId + 1)
        }
        val newer = repository.messagesAfter(chatId, opened.lastReadId)
        val older = olderCall.await()
        merge(older + newer)
        hasMore = !neverRead && older.size == MESSAGE_PAGE_SIZE
        hasNewer = newer.size == MESSAGE_PAGE_SIZE
        unreadFromId = newer.firstOrNull { it.senderId != userId }?.id
    }

    private suspend fun deliver(tempId: String, text: String, switches: PostSwitches?, replyToId: String? = null) {
        try {
            val saved = toMessage(repository.send(chatId, text, switches, replyToId))
            messages = messages
                .filterNot { it.id == saved.id }
                .map { if (it.id == tempId) saved.copy(key = it.key, live = true) else it }
            askStats()
            onChatChanged()
        } catch (_: ApiError) {
            update(tempId) { it.copy(state = SendState.Failed) }
        }
    }

    private fun sendTyping(active: Boolean) {
        realtime.send("typing", JSONObject().put("chatId", chatId).put("active", active))
    }

    private fun handleEvent(event: RealtimeEvent) {
        val data = event.data
        if (event.name == "ready") {
            if (!loading && !hasNewer) refreshLatest()
            return
        }
        if (data == null || data.optString("chatId") != chatId) return
        when (event.name) {
            "message.new" -> receive(repository.toMessage(data))
            "message.edited" -> {
                val edited = repository.toMessage(data)
                messages = messages.map { message ->
                    val reply = message.replyTo
                    when {
                        message.id == edited.id -> message.copy(text = edited.text, editedAt = edited.editedAt)
                        reply?.id == edited.id -> message.copy(replyTo = reply.copy(text = edited.text))
                        else -> message
                    }
                }
                if (pinned?.id == edited.id) pinned = pinned?.copy(text = edited.text, editedAt = edited.editedAt)
            }
            "message.deleted" -> {
                val id = data.optString("id")
                drop(id)
                messages = messages.map { message ->
                    val reply = message.replyTo
                    if (reply?.id == id) message.copy(replyTo = reply.copy(deleted = true, text = "")) else message
                }
                if (pinned?.id == id) pinned = null
            }
            "message.pinned" -> pinned = data.optJSONObject("message")?.let { toMessage(repository.toMessage(it)) }
            "message.read" -> {
                val upTo = data.optString("upToMessageId").toLongOrNull() ?: return
                if (data.optString("userId") != userId && upTo > peerReadId) peerReadId = upTo
            }
            "typing" -> if (data.optString("userId") != userId) {
                typingName = data.optString("name").ifEmpty { null }
                showTyping(data.optBoolean("active"))
            }
            "room.changed" -> scope.launch { loadOnline() }
            "post.reactions" -> {
                val id = data.optString("messageId")
                val current = postStats[id] ?: return
                postStats[id] = current.copy(reactions = data.optJSONArray("reactions")?.toReactions() ?: emptyList())
            }
            "post.comment", "post.commentDeleted" -> {
                val id = data.optString("messageId")
                postStats[id]?.let { postStats[id] = it.copy(comments = data.optInt("comments")) }
                commentEvent = event
            }
            "post.settings" -> askStats(listOf(data.optString("messageId")))
        }
    }

    private fun refreshLatest() {
        scope.launch {
            try {
                fetchLatest()
            } catch (_: ApiError) {
            }
        }
    }

    private fun receive(message: ServerMessage) {
        if (message.senderId != userId) showTyping(false)
        if (hasNewer) return
        val mine = message.senderId == userId
        val known = messages.any { it.id == message.id }
        val pending = messages.any { it.temp && it.state == SendState.Sending && it.text == message.text }
        if (mine && !known && pending) return
        merge(listOf(message), live = !known)
    }

    private fun showTyping(active: Boolean) {
        typingJob?.cancel()
        typing = active
        if (!active) return
        typingJob = scope.launch {
            delay(TYPING_SHOW_FOR)
            typing = false
        }
    }

    private fun drop(id: String) {
        messages = messages.filterNot { it.id == id }
        postStats.remove(id)
    }

    private fun update(id: String, change: (Message) -> Message) {
        messages = messages.map { if (it.id == id) change(it) else it }
    }

    private fun merge(incoming: List<ServerMessage>, live: Boolean = false) {
        val fresh = incoming.map { toMessage(it).copy(live = live) }
        val ids = fresh.map { it.id }.toSet()
        val keys = messages.associate { it.id to it.key }
        val kept = messages.filterNot { it.id in ids }
        val merged = fresh.map { it.copy(key = keys[it.id] ?: it.key) }
        messages = (kept + merged).sortedBy { it.number }
        askStats()
    }

    private fun toMessage(message: ServerMessage) = Message(
        key = message.id,
        id = message.id,
        text = message.text,
        createdAt = message.createdAt,
        mine = message.senderId == userId,
        state = SendState.Sent,
        senderId = message.senderId,
        senderName = message.senderName,
        editedAt = message.editedAt,
        replyTo = message.replyTo,
        forwardedFrom = message.forwardedFrom,
    )
}

data class Jump(val messageId: String, val count: Int, val reloaded: Boolean)
