package com.meghnathnews.mironchat.ui.chat

import com.meghnathnews.mironchat.MironApp
import com.meghnathnews.mironchat.data.GroupData
import com.meghnathnews.mironchat.data.MEMBER_LIMIT
import com.meghnathnews.mironchat.data.RoomMember
import com.meghnathnews.mironchat.data.can
import com.meghnathnews.mironchat.data.colorOf
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.Presence
import com.meghnathnews.mironchat.ui.rooms.MemberListScreen
import com.meghnathnews.mironchat.ui.theme.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.Person
import com.meghnathnews.mironchat.data.RoomRepository
import com.meghnathnews.mironchat.data.roomLabel
import com.meghnathnews.mironchat.ui.rooms.RoomInfoScreen
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.chats.formatCount
import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedIcon
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.DoodleInk
import com.meghnathnews.mironchat.ui.theme.Stage
import com.meghnathnews.mironchat.ui.theme.bouncySpring
import com.meghnathnews.mironchat.ui.theme.softSpring
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val LOAD_EDGE = 5
private const val JUMP_SHOW_AFTER = 4
private const val DOODLE_STRENGTH = 1f
private const val FLASH_FOR = 2_000L
private val DoodleTile = 300.dp
private val ChatDoodleTile = 520.dp
private val Neutral = Color(0xFF7C7A84)
private val HeaderSpace = 76.dp

private sealed interface Confirm {
    data class DeleteMessage(val message: Message) : Confirm
    data object DeleteChat : Confirm
}

private sealed interface ListRow {
    val key: String
}

private data class MessageRow(
    val message: Message,
    val groupStart: Boolean,
    val groupEnd: Boolean,
    val firstOfSender: Boolean,
    val runStart: Boolean = false,
) : ListRow {
    override val key get() = message.key
}

private data class DayRow(val label: String, val at: Long) : ListRow {
    override val key get() = "day-$at"
}

private data object UnreadRow : ListRow {
    override val key = "unread-line"
}

@Composable
fun ChatScreen(
    room: ChatRoom,
    chat: Chat,
    channelName: String? = null,
    selfName: String = "",
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onToggleMute: () -> Unit,
    onDeleteChat: () -> Unit,
    userId: String,
    rooms: RoomRepository,
    roomTick: Int,
    knownPeople: List<Person>,
    onSearchPeople: suspend (String) -> List<Person>,
    onRoomChanged: () -> Unit,
    onLeave: () -> Unit,
    presence: Map<String, Presence> = emptyMap(),
    channelPermissions: List<String>? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val listState = rememberLazyListState()
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var positioned by remember(room) { mutableStateOf(false) }
    var infoOpen by remember(room) { mutableStateOf(false) }
    var membersOpen by remember(room) { mutableStateOf(false) }
    val groupOpen = chat.kind == "group" && chat.status == "active"
    var groupData by remember(chat.id) { mutableStateOf<GroupData?>(null) }
    LaunchedEffect(chat.id, room.chatId, roomTick, groupOpen) {
        if (!groupOpen) return@LaunchedEffect
        val roleApi = (context.applicationContext as MironApp).roles
        try {
            val all = mutableListOf<RoomMember>()
            while (all.size < MEMBER_LIMIT) {
                val page = rooms.members(chat.id, all.size, room.chatId)
                all += page
                if (page.size < 100) break
            }
            groupData = GroupData(all.filter { it.status == "active" }.take(MEMBER_LIMIT), roleApi.list(chat.id), System.currentTimeMillis())
        } catch (_: ApiError) {
        }
    }
    val nameColors = groupData?.let { data -> data.members.associate { it.id to colorOf(it.roleIds, data.roles) } }.orEmpty()
    val ownerId = groupData?.members?.firstOrNull { it.role == "owner" }?.id
    var flashId by remember(room) { mutableStateOf<String?>(null) }
    var commentsFor by remember(room) { mutableStateOf<String?>(null) }
    val channel = chat.kind == "channel"
    val canManagePosts = channel && (chat.role == "owner" || chat.role == "admin")
    val focusManager = LocalFocusManager.current
    val inputFocus = remember(room) { FocusRequester() }
    val canType = if (chat.isRoom) chat.canSend else chat.status != "request" && chat.status != "restricted" && !chat.peerDeleted && !chat.peerDisabled
    val sameSide: (Message?, Message) -> Boolean = { other, message ->
        !channel && other != null && other.mine == message.mine && (message.mine || other.senderId == message.senderId)
    }
    val messages = room.messages
    val flat = chat.kind == "group"
    val rows = remember(messages, room.unreadFromId, channel, flat) {
        buildList<ListRow> {
            messages.forEachIndexed { index, message ->
                val previous = messages.getOrNull(index - 1)
                val next = messages.getOrNull(index + 1)
                if (message.id == room.unreadFromId) add(UnreadRow)
                if (flat && (previous == null || !sameDay(previous.createdAt, message.createdAt))) {
                    add(DayRow(dayLabel(message.createdAt), message.createdAt))
                }
                add(
                    MessageRow(
                        message = message,
                        groupStart = !sameSide(previous, message),
                        groupEnd = !sameSide(next, message),
                        firstOfSender = !sameSide(previous, message),
                        runStart = !joins(previous, message) || message.id == room.unreadFromId,
                    ),
                )
            }
        }.asReversed()
    }
    val currentRows by rememberUpdatedState(rows)
    val showJump by remember(room) {
        derivedStateOf { listState.firstVisibleItemIndex >= JUMP_SHOW_AFTER || room.hasNewer }
    }
    val headerSpace = with(LocalDensity.current) { HeaderSpace.roundToPx() }

    BackHandler(onBack = onBack)

    LaunchedEffect(room) {
        if (room.focusInput && canType) inputFocus.requestFocus() else focusManager.clearFocus()
    }

    LaunchedEffect(room.loading) {
        if (room.loading) return@LaunchedEffect
        val line = rows.indexOf(UnreadRow)
        if (line >= 0) {
            listState.scrollToItem(line)
            val info = listState.layoutInfo
            val lineSize = info.visibleItemsInfo.firstOrNull { it.index == line }?.size ?: 0
            val distance = info.viewportSize.height - lineSize - headerSpace
            if (distance > 0) listState.scrollBy(-distance.toFloat())
        }
        withFrameNanos { }
        withFrameNanos { }
        positioned = true
        val target = currentRows.indexOfFirst { (it as? MessageRow)?.message?.id == room.focusId }
        if (target < 0) return@LaunchedEffect
        listState.scrollToItem(target)
        flashId = room.focusId
        delay(FLASH_FOR)
        flashId = null
    }

    LaunchedEffect(room.sentCount) {
        if (room.sentCount > 0) listState.scrollToItem(0)
    }

    LaunchedEffect(messages.lastOrNull()?.key) {
        if (listState.firstVisibleItemIndex <= 1 && !room.hasNewer) listState.animateScrollToItem(0)
    }

    LaunchedEffect(room, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow {
                val visible = listState.layoutInfo.visibleItemsInfo
                val newest = if (!positioned) null else visible.firstNotNullOfOrNull { info ->
                    (currentRows.getOrNull(info.index) as? MessageRow)?.message?.takeIf { !it.temp }
                }
                val first = visible.firstOrNull()?.index ?: 0
                val fromEnd = currentRows.size - (visible.lastOrNull()?.index ?: 0)
                Triple(newest?.number, first, fromEnd)
            }.distinctUntilChanged().collect { (newest, first, fromEnd) ->
                if (newest == null) return@collect
                room.seen(newest)
                if (fromEnd <= LOAD_EDGE) room.loadOlder()
                if (first <= LOAD_EDGE) room.loadNewer()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Stage),
    ) {
        Doodle(DoodleInk, tileSize = ChatDoodleTile, solid = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (room.loading) {
                    CircularProgressIndicator(color = Chroma, modifier = Modifier.align(Alignment.Center))
                }
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(if (flat) 0.dp else 3.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = HeaderSpace, bottom = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(rows, key = { row -> row.key }) { row ->
                        when (row) {
                            UnreadRow -> UnreadLine()
                            is DayRow -> DayLine(row.label)
                            is MessageRow -> {
                                val message = row.message
                                val animateIn = remember(message.key) { room.shouldPlay(message) }
                                Box(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = null,
                                        placementSpec = softSpring(IntOffset.VisibilityThreshold),
                                    ),
                                ) {
                                if (flat) {
                                    GroupMessage(
                                        message = message,
                                        start = row.runStart,
                                        focused = message.id == flashId,
                                        selfName = selfName,
                                        nameColor = nameColors[message.senderId],
                                        canDelete = (channelPermissions?.contains("deleteMessages") ?: chat.can("deleteMessages")) &&
                                            (chat.role == "owner" || message.senderId != ownerId),
                                        onCopy = { copyText(context, message.text) },
                                        onResend = { room.resend(message) },
                                        onDelete = {
                                            if (message.temp) room.delete(message)
                                            else confirm = Confirm.DeleteMessage(message)
                                        },
                                    )
                                } else MessageBubble(
                                    message = message,
                                    read = !message.temp && message.number <= room.peerReadId,
                                    groupStart = row.groupStart,
                                    groupEnd = row.groupEnd,
                                    animateIn = animateIn,
                                    focused = message.id == flashId,
                                    showSender = channel || (chat.kind == "group" && !message.mine),
                                    channelName = if (channel) chat.name else null,
                                    channelPhoto = if (channel) chat.photo else "",
                                    stats = room.postStats[message.id],
                                    canManagePosts = canManagePosts,
                                    onReact = { emoji -> room.react(message.id, emoji) },
                                    onOpenComments = { commentsFor = message.id },
                                    onTogglePostSwitch = { reactions -> room.togglePostSwitch(message.id, reactions) },
                                    firstOfSender = row.firstOfSender,
                                    hideTicks = chat.kind == "channel",
                                    canDelete = !chat.isRoom || chat.can("deleteMessages"),
                                    onCopy = { copyText(context, message.text) },
                                    onResend = { room.resend(message) },
                                    onDelete = {
                                        if (message.temp) room.delete(message)
                                        else confirm = Confirm.DeleteMessage(message)
                                    },
                                )
                                }
                            }
                        }
                    }
                }
                PopVisibility(
                    visible = showJump,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp),
                ) {
                    JumpButton(
                        unread = chat.unreadCount,
                        modifier = Modifier,
                        onClick = {
                            if (room.hasNewer) room.jumpToLatest()
                        },
                        listScroll = { listState.animateScrollToItem(0) },
                    )
                }
            }
            when {
                chat.isRoom && !chat.canSend && chat.kind == "channel" -> WideButton(if (chat.muted) "Unmute" else "Mute", onToggleMute)
                channelPermissions != null && !channelPermissions.contains("sendMessages") -> AccountBar("You cannot send messages in this channel")
                chat.kind != "group" && chat.isRoom && !chat.canSend -> AccountBar("Only admins can send messages")
                chat.status == "request" -> RequestBar(
                    name = chat.name,
                    onAccept = room::accept,
                    onDelete = { confirm = Confirm.DeleteChat },
                )
                chat.status == "restricted" -> WideButton("Unrestrict to chat", room::unrestrict)
                chat.peerDeleted -> AccountBar("This account is deleted")
                chat.peerDisabled -> AccountBar("This account is disabled")
                else -> Column {
                    if (channel) PostToggles(room.switches, room::changeSwitches)
                    InputBar(
                        focus = inputFocus,
                        onTyping = room::typed,
                        onSend = room::send,
                        onAttach = { Toast.makeText(context, "Attachments coming soon", Toast.LENGTH_SHORT).show() },
                    )
                }
            }
        }
        Header(
            chat = chat,
            channelName = channelName,
            typing = room.typing,
            typingName = room.typingName,
            onlineCount = room.onlineCount,
            onLeave = onLeave,
            onBack = onBack,
            onOpenInfo = { infoOpen = true },
            onCall = { Toast.makeText(context, "Calls coming soon", Toast.LENGTH_SHORT).show() },
            onSearch = onSearch,
            onToggleMute = onToggleMute,
            onCopyId = { copyText(context, chat.peerId) },
            onDelete = onDeleteChat,
            onMembers = if (groupOpen) ({ membersOpen = true }) else null,
        )
    }

    AnimatedVisibility(
        visible = infoOpen,
        enter = slideInHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it },
        exit = slideOutHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it },
    ) {
        if (chat.isRoom) {
            RoomInfoScreen(
                chat = chat,
                userId = userId,
                typingName = if (room.typing) room.typingName else null,
                onlineCount = room.onlineCount,
                refreshKey = roomTick,
                known = knownPeople,
                rooms = rooms,
                onSearch = onSearchPeople,
                onBack = { infoOpen = false },
                onChanged = onRoomChanged,
                onToggleMute = onToggleMute,
                onLeave = onLeave,
                onDelete = onDeleteChat,
            )
        } else {
            UserInfoScreen(
                chat = chat,
                typing = room.typing,
                onBack = { infoOpen = false },
                onToggleMute = onToggleMute,
                onRestrict = room::restrict,
                onUnrestrict = room::unrestrict,
                onDelete = onDeleteChat,
            )
        }
    }

    if (membersOpen) {
        MemberListScreen(
            group = chat,
            data = groupData,
            userId = userId,
            presence = presence,
            onBack = { membersOpen = false },
        )
    }

    val commentsPost = commentsFor?.let { id -> messages.firstOrNull { it.id == id } }
    val commentsStats = commentsPost?.let { room.postStats[it.id] }
    if (commentsPost != null && commentsStats?.allowComments != false) {
        CommentsScreen(
            room = room,
            chat = chat,
            post = commentsPost,
            stats = commentsStats,
            userId = userId,
            canModerate = canManagePosts,
            onBack = { commentsFor = null },
        )
    } else if (commentsFor != null) {
        LaunchedEffect(commentsFor) {
            if (commentsPost != null) Toast.makeText(context, "Comments are turned off", Toast.LENGTH_SHORT).show()
            commentsFor = null
        }
    }

    LaunchedEffect(room.notice) {
        val text = room.notice ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        room.notice = null
    }

    confirm?.let { current ->
        val deleting = current as? Confirm.DeleteMessage
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (deleting != null) "Delete message?" else "Delete chat?") },
            text = {
                Text(
                    if (deleting != null) "This message will be deleted for everyone."
                    else "All messages in this chat will be deleted for everyone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (deleting != null) room.delete(deleting.message) else room.deleteChat()
                }) {
                    Text("Yes", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("No", color = Ink) }
            },
        )
    }
}

fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

@Composable
fun Doodle(color: Color, tileSize: Dp = DoodleTile, solid: Boolean = false) {
    val tile: ImageBitmap = ImageBitmap.imageResource(if (solid) R.drawable.doodle_solid else R.drawable.doodle)
    val size = with(LocalDensity.current) { tileSize.roundToPx() }
    Canvas(modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer()) {
        val filter = ColorFilter.tint(color, BlendMode.SrcIn)
        var y = 0
        while (y < this.size.height) {
            var x = 0
            while (x < this.size.width) {
                drawImage(
                    image = tile,
                    dstOffset = IntOffset(x, y),
                    dstSize = IntSize(size, size),
                    alpha = DOODLE_STRENGTH,
                    colorFilter = filter,
                )
                x += size
            }
            y += size
        }
    }
}

@Composable
private fun UnreadLine() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(
            text = "Unread messages",
            color = Chroma,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(Panel)
                .border(1.dp, BubbleBorder, CircleShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PopVisibility(visible: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(bouncySpring()) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
private fun JumpButton(
    unread: Int,
    modifier: Modifier,
    onClick: () -> Unit,
    listScroll: suspend () -> Unit,
) {
    var clicks by remember { mutableStateOf(0) }
    LaunchedEffect(clicks) {
        if (clicks > 0) listScroll()
    }
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Panel)
                .border(1.dp, BubbleBorder, CircleShape)
                .border(1.dp, BubbleBorder, CircleShape)
                .clickable {
                    onClick()
                    clicks += 1
                },
        ) {
            Icon(painterResource(R.drawable.ic_arrow_down), "Scroll to latest", tint = Ink)
        }
        if (unread > 0) {
            Text(
                text = formatCount(unread),
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Chroma)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun Header(
    chat: Chat,
    channelName: String?,
    typing: Boolean,
    typingName: String?,
    onlineCount: Int,
    onLeave: () -> Unit,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onCall: () -> Unit,
    onSearch: () -> Unit,
    onToggleMute: () -> Unit,
    onCopyId: () -> Unit,
    onDelete: () -> Unit,
    onMembers: (() -> Unit)?,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val choose: (() -> Unit) -> () -> Unit = { action ->
        {
            menuOpen = false
            action()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .drawBehind { drawLine(BubbleBorder, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        IconBox(R.drawable.ic_back, "Back", Ink, onBack)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenInfo),
        ) {
        Avatar(chat.name, 44.dp, disabled = chat.peerDisabled, deleted = chat.peerDeleted, photo = chat.photo)
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = if (channelName != null) "# $channelName" else chat.name,
                color = if (chat.peerDisabled) MutedIcon else if (chat.peerDeleted) InkMuted else Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = if (chat.peerDisabled) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!chat.peerDeleted) PeerStatus(chat, typing, typingName, onlineCount, if (channelName != null) chat.name else null)
        }
        }
        if (!chat.isRoom) IconBox(R.drawable.ic_call, "Call", InkMuted, onCall)
        if (onMembers != null) IconBox(R.drawable.ic_groups, "Members", InkMuted, onMembers)
        IconBox(R.drawable.ic_search, "Search", InkMuted, onSearch)
        Box {
            IconBox(R.drawable.ic_more, "More", InkMuted) { menuOpen = true }
            if (menuOpen) DropdownMenu(
                expanded = true,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = Panel,
            ) {
                val label = roomLabel(chat.kind)
                MenuItem(if (chat.isRoom) "$label Info" else "View Profile", onClick = choose(onOpenInfo))
                MenuItem(if (chat.muted) "Unmute" else "Mute", onClick = choose(onToggleMute))
                when {
                    !chat.isRoom -> {
                        MenuItem("Copy ID", onClick = choose(onCopyId))
                        MenuItem("Delete Chat", danger = true, onClick = choose(onDelete))
                    }
                    chat.role == "owner" -> MenuItem("Delete $label", danger = true, onClick = choose(onDelete))
                    else -> MenuItem("Leave $label", danger = true, onClick = choose(onLeave))
                }
            }
        }
    }
}

@Composable
private fun IconBox(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(painterResource(icon), label, tint = tint)
    }
}

private fun lastSeenText(ms: Long?): String {
    if (ms == null || ms == 0L) return "last seen recently"
    val seen = Calendar.getInstance().apply { timeInMillis = ms }
    val now = Calendar.getInstance()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    val today = seen.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        seen.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    if (today) return "last seen at $time"
    return "last seen ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))} at $time"
}

private fun roomStatus(chat: Chat, typingName: String?, onlineCount: Int): String {
    if (typingName != null) return "$typingName is typing"
    val members = "${chat.memberCount} ${if (chat.memberCount == 1) "member" else "members"}"
    return if (onlineCount > 0) "$members, $onlineCount online" else members
}

private fun statusText(chat: Chat, typing: Boolean): String = when {
    chat.peerDisabled -> "account disabled"
    typing -> "typing"
    chat.peerOnline -> "online"
    else -> lastSeenText(chat.peerLastSeenAt)
}

@Composable
fun PeerStatus(chat: Chat, typing: Boolean, typingName: String? = null, onlineCount: Int = 0, prefix: String? = null) {
    val plain = if (chat.isRoom) roomStatus(chat, if (typing) typingName else null, onlineCount) else statusText(chat, typing)
    val text = if (prefix != null && !typing) "$prefix · $plain" else plain
    val active = !chat.peerDisabled && (typing || chat.peerOnline)
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        },
        label = "status",
    ) { shown ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (active && (shown == "typing" || shown.endsWith(" is typing"))) TypingDots()
            if (active && shown == "online") {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Chroma),
                )
            }
            Text(
                text = shown,
                color = if (active) Chroma else InkMuted,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TypingDots() {
    val motion = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val lift by motion.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550, delayMillis = index * 150),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = -lift * 4.dp.toPx() }
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Chroma.copy(alpha = 0.45f + lift * 0.55f)),
            )
        }
    }
}

@Composable
private fun RequestBar(name: String, onAccept: () -> Unit, onDelete: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, BubbleBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text("$name wants to send you a message", color = Ink, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onDelete,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = Danger),
                modifier = Modifier.weight(1f),
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Chroma),
                modifier = Modifier.weight(1f),
            ) {
                Text("Accept", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WideButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Chroma),
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountBar(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(Panel)
            .border(1.dp, BubbleBorder, CircleShape),
    ) {
        Text(text, color = InkMuted, fontSize = 14.sp)
    }
}
