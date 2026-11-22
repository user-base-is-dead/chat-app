package com.civenth.app.ui.main

import com.civenth.app.ui.theme.DockPill
import com.civenth.app.ui.theme.SoftShadow
import com.civenth.app.ui.theme.CanvasTop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civenth.app.R
import com.civenth.app.alerts.AlertRoute
import com.civenth.app.alerts.Push
import com.civenth.app.data.Chat
import com.civenth.app.data.User
import com.civenth.app.data.roomLabel
import com.civenth.app.ui.chat.ChatScreen
import com.civenth.app.ui.chat.ForwardOptions
import com.civenth.app.ui.chats.ChatAction
import com.civenth.app.ui.chats.ChatsScreen
import com.civenth.app.ui.chats.ChatsViewModel
import com.civenth.app.ui.chats.FindKind
import com.civenth.app.ui.chats.FindUserScreen
import com.civenth.app.ui.chats.RequestsScreen
import com.civenth.app.ui.rooms.JoinPrompt
import com.civenth.app.ui.rooms.RoomCreateScreen
import com.civenth.app.ui.rooms.RoomsScreen
import com.civenth.app.ui.settings.SettingsScreen
import com.civenth.app.ui.settings.SettingsView
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Dock
import com.civenth.app.ui.theme.DockAccent
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.Fab
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.OnFab
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.bouncySpring
import com.civenth.app.ui.theme.pressScale
import com.civenth.app.ui.theme.softSpring

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Chats("Chats", R.drawable.ic_chats),
    Groups("Groups", R.drawable.ic_groups),
    Channels("Channels", R.drawable.ic_channels),
    Settings("Settings", R.drawable.ic_settings),
}

private enum class Pane(val order: Int) { Chats(0), Requests(0), Find(0), Create(0), Groups(1), Channels(2), Settings(3) }

private val DockSpace = 96.dp
private val FindButtonSpace = 72.dp

@Composable
fun MainScreen(
    user: User,
    onSessionLost: () -> Unit,
    onUserChange: (User) -> Unit,
    onAccountGone: () -> Unit,
    onLogout: () -> Unit,
) {
    val model: ChatsViewModel = viewModel(key = "chats-${user.id}")
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tab by rememberSaveable { mutableStateOf(Tab.Chats) }
    var requestsOpen by rememberSaveable { mutableStateOf(false) }
    var findKind by rememberSaveable { mutableStateOf<FindKind?>(null) }
    var createKind by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsView by rememberSaveable { mutableStateOf(SettingsView.Main) }
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomSpace = PaddingValues(bottom = DockSpace + navigationInset)
    val chatsBottomSpace = PaddingValues(bottom = DockSpace + navigationInset + FindButtonSpace)
    val openChat: (Chat) -> Unit = remember(model, user.id) { { model.openChat(it, user) } }
    val copyId: (Chat) -> ChatAction = remember(context) { { chat ->
        ChatAction("Copy ID") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("id", chat.peerId))
        }
    } }
    val deleteAction: (Chat) -> ChatAction = remember(model) { { chat ->
        ChatAction("Delete Chat", danger = true) { model.askDelete(chat) }
    } }
    val chatActions: (Chat) -> List<ChatAction> = remember(model) { { chat ->
        buildList {
            val pinned = chat.pinnedAt != null
            if (pinned || model.canPin) add(ChatAction(if (pinned) "Unpin" else "Pin", icon = R.drawable.ic_pin) { model.togglePin(chat) })
            add(ChatAction(if (chat.muted) "Unmute" else "Mute", icon = if (chat.muted) R.drawable.ic_bell else R.drawable.ic_mute) { model.toggleMute(chat) })
            add(ChatAction("Restrict", icon = R.drawable.ic_restrict) { model.restrict(chat) })
            add(copyId(chat))
            add(deleteAction(chat))
        }
    } }
    val roomActions: (Chat) -> List<ChatAction> = remember(model) { { chat ->
        buildList {
            val pinned = chat.pinnedAt != null
            if (pinned || model.canPin) add(ChatAction(if (pinned) "Unpin" else "Pin", icon = R.drawable.ic_pin) { model.togglePin(chat) })
            add(ChatAction(if (chat.muted) "Unmute" else "Mute", icon = if (chat.muted) R.drawable.ic_bell else R.drawable.ic_mute) { model.toggleMute(chat) })
            val label = roomLabel(chat.kind)
            if (chat.role == "owner") add(ChatAction("Delete $label", danger = true) { model.askDelete(chat) })
            else add(ChatAction("Leave $label", danger = true) { model.askLeave(chat) })
        }
    } }
    val restrictedActions: (Chat) -> List<ChatAction> = remember(model) { { chat ->
        listOf(ChatAction("Unrestrict") { model.unrestrict(chat) }, copyId(chat), deleteAction(chat))
    } }

    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) model.start()
            if (event == Lifecycle.Event.ON_STOP) model.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            model.stop()
        }
    }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(user.id) {
        Push.register(context)
        val missing = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && missing) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(AlertRoute.chatId, model.loading) {
        val chat = AlertRoute.chatId?.let(model::chatById) ?: return@LaunchedEffect
        AlertRoute.chatId = null
        tab = Tab.Chats
        model.openChat(chat, user)
    }

    LaunchedEffect(model.sessionLost) {
        if (model.sessionLost) onSessionLost()
    }

    val covered by animateFloatAsState(
        targetValue = if (model.room != null) 1f else 0f,
        animationSpec = softSpring(),
        label = "covered",
    )
    val pane = when {
        createKind != null -> Pane.Create
        findKind != null -> Pane.Find
        tab == Tab.Groups -> Pane.Groups
        tab == Tab.Channels -> Pane.Channels
        tab == Tab.Settings -> Pane.Settings
        requestsOpen -> Pane.Requests
        else -> Pane.Chats
    }

    BackHandler(enabled = requestsOpen || tab != Tab.Chats) {
        if (requestsOpen && tab == Tab.Chats) requestsOpen = false else tab = Tab.Chats
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to CanvasTop, 0.4f to Canvas, 1f to Canvas))
            .statusBarsPadding()
            .graphicsLayer {
                translationX = -size.width * 0.18f * covered
                alpha = 1f - 0.3f * covered
            }
            .then(if (model.room != null) Modifier.clearAndSetSemantics {} else Modifier),
    ) {
        AnimatedContent(
            targetState = pane,
            transitionSpec = {
                val deeper = targetState == Pane.Requests || targetState == Pane.Find || targetState == Pane.Create
                val shallower = initialState == Pane.Requests || initialState == Pane.Find || initialState == Pane.Create
                val motion = softSpring(IntOffset.VisibilityThreshold)
                when {
                    deeper -> (slideInHorizontally(motion) { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally(motion) { -it / 3 } + fadeOut())
                    shallower -> (slideInHorizontally(motion) { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally(motion) { it / 3 } + fadeOut())
                    else -> {
                        val forward = initialState.order < targetState.order
                        val side = if (forward) 1 else -1
                        val move = spring(dampingRatio = 0.86f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)
                        val grow = spring<Float>(dampingRatio = 0.86f, stiffness = 380f)
                        (slideInHorizontally(move) { it / 4 * side } + fadeIn(tween(240, delayMillis = 40)) + scaleIn(grow, initialScale = 0.94f)) togetherWith
                            (slideOutHorizontally(move) { -it / 6 * side } + fadeOut(tween(180)) + scaleOut(grow, targetScale = 0.92f))
                    }
                }
            },
            label = "pane",
        ) { shown ->
            when (shown) {
                Pane.Find -> FindUserScreen(
                    kind = findKind ?: FindKind.Users,
                    onSearch = model::searchUsers,
                    onSearchRooms = { query, kind -> model.rooms.find(query, kind) },
                    onBack = { findKind = null },
                    onStartChat = { model.startChat(it, user) },
                    onOpenRoom = { model.openPreview(it, user) },
                    onOpenKey = { model.openKey(it, user) },
                )
                Pane.Create -> RoomCreateScreen(
                    kind = createKind ?: "group",
                    known = model.knownPeople,
                    rooms = model.rooms,
                    onSearch = model::searchUsers,
                    onBack = { createKind = null },
                    onCreated = { chat, result ->
                        createKind = null
                        tab = if (chat.kind == "channel") Tab.Channels else Tab.Groups
                        model.roomCreated(chat, user)
                        if (result.skipped > 0) {
                            Toast.makeText(context, "${result.skipped} could not be added", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                Pane.Requests -> RequestsScreen(
                    model,
                    bottomSpace,
                    restrictedActions,
                    onBack = { requestsOpen = false },
                    onOpenChat = openChat,
                )
                Pane.Chats -> ChatsScreen(
                    model,
                    chatsBottomSpace,
                    chatActions,
                    onOpenRequests = { requestsOpen = true },
                    onOpenChat = openChat,
                    onOpenMessage = { model.openChat(it, user, focusId = it.lastId) },
                )
                Pane.Groups -> RoomsScreen(
                    title = "Groups",
                    kind = "group",
                    icon = R.drawable.ic_groups,
                    emptyText = "No groups yet",
                    rooms = model.groups,
                    model = model,
                    bottomSpace = chatsBottomSpace,
                    actionsFor = roomActions,
                    onOpenChat = openChat,
                    onOpenMessage = { model.openChat(it, user, focusId = it.lastId) },
                    onOpenChannel = { group, channel -> model.openChannel(group, channel, user) },
                    onNewGroup = { createKind = "group" },
                )
                Pane.Channels -> RoomsScreen(
                    title = "Channels",
                    kind = "channel",
                    icon = R.drawable.ic_channels,
                    emptyText = "No channels yet",
                    rooms = model.channels,
                    model = model,
                    bottomSpace = chatsBottomSpace,
                    actionsFor = roomActions,
                    onOpenChat = openChat,
                    onOpenMessage = { model.openChat(it, user, focusId = it.lastId) },
                )
                Pane.Settings -> SettingsScreen(
                    user = user,
                    view = settingsView,
                    bottomSpace = bottomSpace,
                    onViewChange = { settingsView = it },
                    onUserChange = onUserChange,
                    onAccountGone = onAccountGone,
                    onLogout = onLogout,
                )
            }
        }
        AnimatedVisibility(
            visible = (pane == Pane.Chats && !model.searchOpen) || pane == Pane.Groups || pane == Pane.Channels,
            enter = scaleIn(bouncySpring()) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = DockSpace),
        ) {
            FindButton(onChoose = { findKind = it }, onCreate = { createKind = it })
        }
        AnimatedVisibility(
            visible = findKind == null && createKind == null && !(pane == Pane.Chats && model.searchOpen) && (tab != Tab.Settings || settingsView == SettingsView.Main),
            enter = slideInVertically(softSpring(IntOffset.VisibilityThreshold)) { it } + fadeIn(),
            exit = slideOutVertically(softSpring(IntOffset.VisibilityThreshold)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Dock(active = tab, onChange = { tab = it })
        }
    }

    if (model.deleting != null) {
        AlertDialog(
            onDismissRequest = model::cancelDelete,
            title = { Text(model.deleting?.takeIf { it.isRoom }?.let { "Delete ${roomLabel(it.kind).lowercase()}?" } ?: "Delete chat?") },
            text = { Text(if (model.deleting?.isRoom == true) "All messages will be deleted for everyone." else "All messages in this chat will be deleted for everyone.") },
            confirmButton = {
                TextButton(onClick = model::confirmDelete) { Text("Yes", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = model::cancelDelete) { Text("No", color = Ink) }
            },
        )
    }

    model.leaving?.let { leaving ->
        AlertDialog(
            onDismissRequest = model::cancelLeave,
            title = { Text("Leave ${roomLabel(leaving.kind).lowercase()}?") },
            text = { Text("You will leave ${leaving.name}.") },
            confirmButton = {
                TextButton(onClick = model::confirmLeave) { Text("Yes", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = model::cancelLeave) { Text("No", color = Ink) }
            },
        )
    }

    LaunchedEffect(model.notice) {
        val text = model.notice ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        model.notice = null
    }

    model.joining?.let { state ->
        JoinPrompt(state, onJoin = { model.confirmJoin(user) }, onClose = model::closeJoin)
    }

    val room = model.room
    val opened = model.openedChat
    AnimatedVisibility(
        visible = room != null && opened != null,
        enter = slideInHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it },
        exit = slideOutHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it },
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
        ),
    ) {
        var shown by remember { mutableStateOf(room to opened) }
        if (room != null && opened != null) shown = room to opened
        val (shownRoom, shownChat) = shown
        if (shownRoom != null && shownChat != null) {
            ChatScreen(
                room = shownRoom,
                chat = shownChat,
                channelName = if (shownChat.kind == "group") model.openChannelName ?: "general" else null,
                selfName = user.displayName.ifEmpty { user.username },
                onBack = model::closeChat,
                onSearch = {
                    model.closeChat()
                    findKind = null
                    requestsOpen = false
                    tab = Tab.Chats
                    model.wantSearch = true
                },
                onToggleMute = { model.toggleMute(shownChat) },
                onDeleteChat = { model.askDelete(shownChat) },
                userId = user.id,
                rooms = model.rooms,
                roomTick = model.roomTick,
                knownPeople = model.knownPeople,
                onSearchPeople = model::searchUsers,
                onRoomChanged = model::refreshNow,
                onLeave = { model.askLeave(shownChat) },
                presence = model.presence,
                channelPermissions = if (shownChat.kind == "group") {
                    model.channelLists[shownChat.id]?.channels?.firstOrNull { it.id == shownRoom.chatId }?.permissions
                } else {
                    null
                },
                forward = ForwardOptions(
                    chats = model.active.filter { !it.peerDeleted && !it.peerDisabled },
                    groups = model.groups.filter { it.status == "active" },
                    channels = model.channels.filter { it.status == "active" && it.canSend },
                    channelLists = model.channelLists,
                    loadChannels = model::loadChannels,
                ),
            )
        }
    }
}

@Composable
private fun FindButton(onChoose: (FindKind) -> Unit, onCreate: (String) -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .pressScale(source, pressed = 0.94f)
                .shadow(16.dp, CircleShape, ambientColor = Fab.copy(alpha = 0.45f), spotColor = Fab.copy(alpha = 0.7f))
                .clip(CircleShape)
                .background(Fab)
                .clickable(interactionSource = source, indication = null) { menuOpen = true },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Global search",
                tint = OnFab,
                modifier = Modifier.size(26.dp),
            )
        }
        if (menuOpen) DropdownMenu(
            expanded = true,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Panel,
        ) {
            listOf("New Group" to "group", "New Channel" to "channel").forEach { (label, kind) ->
                DropdownMenuItem(
                    text = { Text(label, color = Ink) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_plus), null, tint = DockAccent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onCreate(kind)
                    },
                )
            }
            HorizontalDivider(color = BubbleBorder, modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "Global Search",
                color = InkMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
            )
            FindKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.title, color = Ink) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_search), null, tint = DockAccent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onChoose(kind)
                    },
                )
            }
        }
    }
}

private val DockHeight = 56.dp
private val DockIconHeight = 30.dp
private val DockPillInset = 2.dp

@Composable
private fun Dock(active: Tab, onChange: (Tab) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .shadow(14.dp, RoundedCornerShape(30.dp), ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(RoundedCornerShape(30.dp))
            .background(Dock)
            .border(1.dp, BubbleBorder, RoundedCornerShape(30.dp))
            .padding(6.dp),
    ) {
        val itemWidth = maxWidth / Tab.entries.size
        val pillWidth = itemWidth * 0.82f
        val pillX by animateDpAsState(
            targetValue = itemWidth * active.ordinal + (itemWidth - pillWidth) / 2,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 480f),
            label = "dockPill",
        )
        Box(
            modifier = Modifier
                .offset(x = pillX + DockPillInset, y = DockPillInset)
                .width(pillWidth - DockPillInset * 2)
                .height(DockHeight - DockPillInset * 2)
                .clip(CircleShape)
                .background(DockPill),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Tab.entries.forEach { item ->
                val selected = item == active
                val tint by animateColorAsState(if (selected) DockAccent else InkMuted, tween(220), label = "tint")
                val lift by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
                    label = "lift",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .weight(1f)
                        .height(DockHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onChange(item) },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(DockIconHeight)) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.graphicsLayer {
                                val scale = 1f + 0.08f * lift
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                    }
                    Text(
                        text = item.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
