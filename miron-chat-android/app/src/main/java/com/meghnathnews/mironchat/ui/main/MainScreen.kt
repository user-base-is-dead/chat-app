package com.meghnathnews.mironchat.ui.main

import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import com.meghnathnews.mironchat.ui.theme.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.alerts.AlertRoute
import com.meghnathnews.mironchat.alerts.MessageAlerts
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.User
import com.meghnathnews.mironchat.data.roomLabel
import com.meghnathnews.mironchat.ui.chat.ChatScreen
import com.meghnathnews.mironchat.ui.chats.ChatAction
import com.meghnathnews.mironchat.ui.chats.ChatsScreen
import com.meghnathnews.mironchat.ui.chats.ChatsViewModel
import com.meghnathnews.mironchat.ui.chats.FindKind
import com.meghnathnews.mironchat.ui.chats.FindUserScreen
import com.meghnathnews.mironchat.ui.chats.RequestsScreen
import com.meghnathnews.mironchat.ui.rooms.JoinPrompt
import com.meghnathnews.mironchat.ui.rooms.RoomCreateScreen
import com.meghnathnews.mironchat.ui.rooms.RoomsScreen
import com.meghnathnews.mironchat.ui.settings.SettingsScreen
import com.meghnathnews.mironchat.ui.settings.SettingsView
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Dock
import com.meghnathnews.mironchat.ui.theme.DockAccent
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.Fab
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.OnFab
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.bouncySpring
import com.meghnathnews.mironchat.ui.theme.pressScale
import com.meghnathnews.mironchat.ui.theme.softSpring

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Chats("Chats", R.drawable.ic_chats),
    Groups("Groups", R.drawable.ic_groups),
    Channels("Channels", R.drawable.ic_channels),
    Settings("Settings", R.drawable.ic_settings),
}

private enum class Pane { Chats, Requests, Find, Create, Groups, Channels, Settings }

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
    val bottomSpace = PaddingValues(bottom = DockSpace)
    val chatsBottomSpace = PaddingValues(bottom = DockSpace + FindButtonSpace)
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
            if (pinned || model.canPin) add(ChatAction(if (pinned) "Unpin" else "Pin") { model.togglePin(chat) })
            add(ChatAction(if (chat.muted) "Unmute" else "Mute") { model.toggleMute(chat) })
            add(ChatAction("Restrict") { model.restrict(chat) })
            add(copyId(chat))
            add(deleteAction(chat))
        }
    } }
    val roomActions: (Chat) -> List<ChatAction> = remember(model) { { chat ->
        buildList {
            val pinned = chat.pinnedAt != null
            if (pinned || model.canPin) add(ChatAction(if (pinned) "Unpin" else "Pin") { model.togglePin(chat) })
            add(ChatAction(if (chat.muted) "Unmute" else "Mute") { model.toggleMute(chat) })
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
        MessageAlerts.start(context)
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
            .background(Canvas)
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
                    else -> (fadeIn() + scaleIn(initialScale = 0.97f)) togetherWith fadeOut()
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
            visible = pane == Pane.Chats || pane == Pane.Groups || pane == Pane.Channels,
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
            visible = findKind == null && createKind == null && (tab != Tab.Settings || settingsView == SettingsView.Main),
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
                .size(56.dp)
                .pressScale(source, pressed = 0.94f)
                .clip(CircleShape)
                .background(Fab)
                .clickable(interactionSource = source, indication = null) { menuOpen = true },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search or create",
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

private val DockHeight = 50.dp
private val DockIconHeight = 30.dp

@Composable
private fun Dock(active: Tab, onChange: (Tab) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Dock)
            .drawBehind { drawLine(BubbleBorder, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Tab.entries.forEach { item ->
                val selected = item == active
                val tint by animateColorAsState(if (selected) DockAccent else InkMuted, label = "tint")
                val labelTint by animateColorAsState(if (selected) DockAccent else InkMuted, label = "labelTint")
                val source = remember { MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.Top),
                    modifier = Modifier
                        .weight(1f)
                        .height(DockHeight)
                        .pressScale(source, pressed = 0.9f)
                        .clickable(
                            interactionSource = source,
                            indication = null,
                            onClick = { onChange(item) },
                        ),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(DockIconHeight)) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null,
                            tint = tint,
                        )
                    }
                    Text(
                        text = item.label,
                        color = labelTint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
