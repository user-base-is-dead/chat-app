package com.meghnathnews.mironchat.ui.rooms

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.meghnathnews.mironchat.BuildConfig
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.MEMBER_PAGE_SIZE
import com.meghnathnews.mironchat.data.Person
import com.meghnathnews.mironchat.data.RoomMember
import com.meghnathnews.mironchat.data.RoomRepository
import com.meghnathnews.mironchat.data.roomLabel
import com.meghnathnews.mironchat.ui.chat.ActionRow
import com.meghnathnews.mironchat.ui.chat.Doodle
import com.meghnathnews.mironchat.ui.chat.InfoCard
import com.meghnathnews.mironchat.ui.chat.InfoRow
import com.meghnathnews.mironchat.ui.chat.MenuItem
import com.meghnathnews.mironchat.ui.chat.PeerStatus
import com.meghnathnews.mironchat.ui.chat.copyText
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.theme.Appearance
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Cover
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COPIED_FOR = 1_500L
private val CoverHeight = 158.dp
private val AvatarSize = 104.dp

private enum class InfoView { Main, Edit, Add }

private data class Ask(val title: String, val text: String, val action: suspend () -> Unit)

fun roomLink(chat: Chat): String {
    val key = if (chat.isPublic) chat.username else chat.inviteCode
    return if (key.isEmpty()) "" else "${BuildConfig.WEB_URL}/join/$key"
}

@Composable
fun RoomInfoScreen(
    chat: Chat,
    userId: String,
    typingName: String?,
    onlineCount: Int,
    refreshKey: Int,
    known: List<Person>,
    rooms: RoomRepository,
    onSearch: suspend (String) -> List<Person>,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onToggleMute: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    var shown by remember { mutableStateOf(InfoView.Main) }
    var members by remember { mutableStateOf(emptyList<RoomMember>()) }
    var hasMore by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(RoomDraft()) }
    var picked by remember { mutableStateOf(emptyList<Person>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val label = roomLabel(chat.kind)
    val isOwner = chat.role == "owner"
    val isAdmin = isOwner || chat.role == "admin"
    val link = roomLink(chat)

    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val run: (suspend () -> Unit) -> Unit = { work ->
        scope.launch {
            try {
                work()
                onChanged()
            } catch (failure: ApiError) {
                toast(failure.message.orEmpty())
            }
        }
    }

    BackHandler { if (shown == InfoView.Main) onBack() else shown = InfoView.Main }

    DisposableEffect(view, shown) {
        val bars = WindowCompat.getInsetsController((view.context as Activity).window, view)
        bars.isAppearanceLightStatusBars = shown != InfoView.Main && !Appearance.dark
        onDispose { bars.isAppearanceLightStatusBars = !Appearance.dark }
    }

    LaunchedEffect(chat.id, refreshKey) {
        try {
            val list = rooms.members(chat.id)
            members = list
            hasMore = list.size == MEMBER_PAGE_SIZE
        } catch (_: ApiError) {
        }
    }

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(COPIED_FOR)
        copied = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        if (shown != InfoView.Main) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                ScreenHeader(if (shown == InfoView.Edit) "Edit $label" else "Add Members") { shown = InfoView.Main }
                if (shown == InfoView.Edit) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        RoomForm(chat.kind, draft, rooms) { draft = it }
                    }
                } else {
                    MemberPicker(
                        known = known,
                        selected = picked,
                        excludeIds = members.map { it.id }.toSet(),
                        onSearch = onSearch,
                        onChange = { picked = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (error.isNotEmpty()) {
                        Text(error, color = Danger, fontSize = 13.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
                    }
                    PrimaryButton(if (shown == InfoView.Edit) "Save" else "Add", busy) {
                        scope.launch {
                            busy = true
                            error = ""
                            try {
                                if (shown == InfoView.Edit) {
                                    val changes = JSONObject()
                                        .put("title", draft.title)
                                        .put("about", draft.about)
                                        .put("isPublic", draft.isPublic)
                                        .put("username", if (draft.isPublic) draft.username else "")
                                    if (draft.photoId.isNotEmpty()) changes.put("photoId", draft.photoId)
                                    rooms.update(chat.id, changes)
                                } else if (picked.isNotEmpty()) {
                                    val result = rooms.addMembers(chat.id, picked.map { it.id })
                                    val parts = listOfNotNull(
                                        "${result.added} added".takeIf { result.added > 0 },
                                        "${result.invited} invited".takeIf { result.invited > 0 },
                                        "${result.skipped} skipped".takeIf { result.skipped > 0 },
                                    )
                                    toast(parts.joinToString(", "))
                                    picked = emptyList()
                                }
                                onChanged()
                                shown = InfoView.Main
                            } catch (failure: ApiError) {
                                error = failure.message.orEmpty()
                            } finally {
                                busy = false
                            }
                        }
                    }
                }
            }
            return@Box
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoverHeight + statusTop)
                    .background(Cover),
            ) {
                Doodle(color = Color.White, tileSize = 260.dp)
            }
            Box(
                modifier = Modifier
                    .offset(y = -AvatarSize / 2)
                    .clip(CircleShape)
                    .background(Panel)
                    .padding(6.dp),
            ) {
                Avatar(chat.name, AvatarSize, photo = chat.photo)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .offset(y = -AvatarSize / 2 + 10.dp)
                    .padding(horizontal = 20.dp),
            ) {
                Text(chat.name, color = Ink, fontSize = 27.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                PeerStatus(chat, typingName != null, typingName, onlineCount)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .offset(y = -AvatarSize / 2 + 26.dp)
                    .padding(horizontal = 14.dp),
            ) {
                if (chat.about.isNotEmpty() || link.isNotEmpty()) {
                    InfoCard {
                        if (chat.about.isNotEmpty()) InfoRow(chat.about, "Description")
                        if (link.isNotEmpty()) {
                            InfoRow(
                                value = if (copied) "Copied" else link,
                                label = if (chat.isPublic) "Link" else "Invite Link",
                                onClick = {
                                    copyText(context, link)
                                    copied = true
                                },
                            )
                        }
                    }
                }
                InfoCard {
                    ActionRow(R.drawable.ic_mute, if (chat.muted) "Unmute" else "Mute", Ink, onToggleMute)
                    if (isAdmin) {
                        ActionRow(R.drawable.ic_edit, "Edit $label", Ink) {
                            draft = RoomDraft(chat.name, chat.about, chat.isPublic, chat.username, "", chat.photo)
                            error = ""
                            shown = InfoView.Edit
                        }
                    }
                    if (isAdmin && !chat.isPublic) {
                        ActionRow(R.drawable.ic_link, "Reset Invite Link", Ink) {
                            ask = Ask("Reset invite link?", "The old link will stop working.") { rooms.resetInvite(chat.id) }
                        }
                    }
                    if (chat.canAdd) {
                        ActionRow(R.drawable.ic_plus, "Add Members", Ink) {
                            error = ""
                            shown = InfoView.Add
                        }
                    }
                }
                if (isAdmin && chat.kind == "group") {
                    InfoCard {
                        CardTitle("Permissions")
                        SwitchRow("Members can send messages", chat.membersCanSend) { value ->
                            run { rooms.update(chat.id, JSONObject().put("membersCanSend", value)) }
                        }
                        SwitchRow("Members can add members", chat.membersCanAdd) { value ->
                            run { rooms.update(chat.id, JSONObject().put("membersCanAdd", value)) }
                        }
                    }
                }
                InfoCard {
                    CardTitle("Members")
                    members.forEach { member ->
                        MemberRow(
                            member = member,
                            self = member.id == userId,
                            menuOpen = menuFor == member.id,
                            isOwner = isOwner,
                            isAdmin = isAdmin,
                            onOpen = { menuFor = member.id },
                            onClose = { menuFor = null },
                            onRole = { role -> run { rooms.setRole(chat.id, member.id, role) } },
                            onTransfer = {
                                ask = Ask("Transfer ownership?", "${member.name} will become the owner. You will stay as an admin.") {
                                    rooms.transfer(chat.id, member.id)
                                }
                            },
                            onRemove = {
                                ask = Ask("Remove ${member.name}?", "They cannot join again until an admin adds them.") {
                                    rooms.removeMember(chat.id, member.id)
                                }
                            },
                        )
                    }
                    if (hasMore) {
                        ActionRow(R.drawable.ic_plus, "Show more", Ink) {
                            scope.launch {
                                try {
                                    val list = rooms.members(chat.id, members.size)
                                    members = members + list
                                    hasMore = list.size == MEMBER_PAGE_SIZE
                                } catch (_: ApiError) {
                                }
                            }
                        }
                    }
                }
                InfoCard {
                    if (isOwner) ActionRow(R.drawable.ic_delete, "Delete $label", Danger, onDelete)
                    else ActionRow(R.drawable.ic_logout, "Leave $label", Danger, onLeave)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Color.White)
            }
            Text("$label Info", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    ask?.let { current ->
        AlertDialog(
            onDismissRequest = { ask = null },
            title = { Text(current.title) },
            text = { Text(current.text) },
            confirmButton = {
                TextButton(onClick = {
                    ask = null
                    run(current.action)
                }) {
                    Text("Yes", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { ask = null }) { Text("No", color = Ink) }
            },
        )
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        color = InkMuted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Chroma, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun MemberRow(
    member: RoomMember,
    self: Boolean,
    menuOpen: Boolean,
    isOwner: Boolean,
    isAdmin: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onRole: (String) -> Unit,
    onTransfer: () -> Unit,
    onRemove: () -> Unit,
) {
    val canRemove = !self && member.role != "owner" && (isOwner || (isAdmin && member.role == "member"))
    val canPromote = !self && isOwner && member.role != "owner" && member.status == "active"
    val roleName = when (member.role) {
        "owner" -> "Owner"
        "admin" -> "Admin"
        else -> ""
    }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = canRemove || canPromote, onClick = onOpen)
                .padding(8.dp),
        ) {
            Avatar(member.name, 40.dp, disabled = member.disabled, deleted = member.deleted)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (self) "${member.name} (You)" else member.name,
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = if (member.status == "request") "Invited" else if (member.username.isNotEmpty()) "@${member.username}" else ""
                if (detail.isNotEmpty()) Text(detail, color = InkMuted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (roleName.isNotEmpty()) Text(roleName, color = Chroma, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        if (menuOpen) DropdownMenu(
            expanded = true,
            onDismissRequest = onClose,
            shape = RoundedCornerShape(18.dp),
            containerColor = Panel,
        ) {
            if (canPromote) {
                val admin = member.role == "admin"
                MenuItem(if (admin) "Remove Admin" else "Make Admin") {
                    onClose()
                    onRole(if (admin) "member" else "admin")
                }
                MenuItem("Transfer Ownership") {
                    onClose()
                    onTransfer()
                }
            }
            if (canRemove) {
                MenuItem(if (member.status == "request") "Cancel Invite" else "Remove", danger = true) {
                    onClose()
                    onRemove()
                }
            }
        }
    }
}
