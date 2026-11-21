package com.mironchat.app.ui.rooms

import com.mironchat.app.ui.theme.pressClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import com.mironchat.app.ui.theme.softSpring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mironchat.app.MironApp
import com.mironchat.app.R
import com.mironchat.app.data.ApiError
import com.mironchat.app.data.CHANNEL_PERMISSIONS
import com.mironchat.app.data.ChannelOverride
import com.mironchat.app.data.ChannelPermissionState
import com.mironchat.app.data.Chat
import com.mironchat.app.data.Role
import com.mironchat.app.data.RoomMember
import com.mironchat.app.data.can
import com.mironchat.app.ui.chats.Avatar
import com.mironchat.app.ui.theme.BorderStrong
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.Danger
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.Panel
import com.mironchat.app.ui.theme.SurfaceStrong
import com.mironchat.app.ui.theme.roleColor
import kotlinx.coroutines.launch

data class PermissionTarget(val kind: String, val id: String, val name: String)

private enum class PermissionView { List, Add, Edit }

private val AllowGreen = Color(0xFF2B8A3E)

@Composable
fun ChannelPermissionsDialog(group: Chat, target: PermissionTarget, onClose: () -> Unit, onChanged: () -> Unit) {
    val app = LocalContext.current.applicationContext as MironApp
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ChannelPermissionState?>(null) }
    var roles by remember { mutableStateOf(emptyList<Role>()) }
    var members by remember { mutableStateOf(emptyList<RoomMember>()) }
    var view by remember { mutableStateOf(PermissionView.List) }
    var editing by remember { mutableStateOf<ChannelOverride?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val full = group.role == "owner" || group.can("administrator")

    LaunchedEffect(target.id) {
        try {
            state = app.channels.permissions(group.id, target.kind, target.id)
            roles = app.roles.list(group.id)
            val all = mutableListOf<RoomMember>()
            while (all.size < 1000) {
                val page = app.rooms.members(group.id, all.size)
                all += page
                if (page.size < 100) break
            }
            members = all.filter { it.status == "active" }
        } catch (failure: ApiError) {
            error = failure.message.orEmpty()
        }
    }

    val labelOf: (ChannelOverride) -> Pair<String, String?> = { entry ->
        when (entry.targetType) {
            "everyone" -> "Everyone" to null
            "role" -> roles.firstOrNull { it.id == entry.targetId }.let { (it?.name ?: "Deleted role") to it?.color }
            else -> (members.firstOrNull { it.id == entry.targetId }?.name ?: "Former member") to null
        }
    }

    val save: (ChannelOverride) -> Unit = { entry ->
        scope.launch {
            busy = true
            error = ""
            try {
                state = app.channels.setOverride(group.id, target.kind, target.id, entry)
                onChanged()
                view = PermissionView.List
            } catch (failure: ApiError) {
                error = failure.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    Dialog(onDismissRequest = { if (view == PermissionView.List) onClose() else view = PermissionView.List }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Panel)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(0.dp, Color.Transparent))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .pressClickable { if (view == PermissionView.List) onClose() else view = PermissionView.List },
                ) {
                    Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    val title = when (view) {
                        PermissionView.List -> "Permissions"
                        PermissionView.Add -> "Add role or member"
                        PermissionView.Edit -> editing?.let { labelOf(it).first } ?: ""
                    }
                    Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(target.name, color = InkMuted, fontSize = 13.sp)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BubbleBorder))

            val current = state
            val fade = remember(view) { Animatable(0f) }
            LaunchedEffect(view) { fade.animateTo(1f, softSpring()) }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = fade.value.coerceIn(0f, 1f)
                        translationY = (1f - fade.value) * 14.dp.toPx()
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (current == null) {
                    if (error.isNotEmpty()) Text(error, color = Danger, fontSize = 13.5.sp)
                    else Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        CircularProgressIndicator(color = Chroma)
                    }
                    return@Column
                }
                val everyone = current.overrides.firstOrNull { it.targetType == "everyone" } ?: ChannelOverride("everyone", "0", emptyList(), emptyList())
                val roleEntries = current.overrides.filter { it.targetType == "role" }
                    .sortedBy { entry -> roles.firstOrNull { it.id == entry.targetId }?.position ?: 99 }
                val memberEntries = current.overrides.filter { it.targetType == "member" }
                val taken = current.overrides.map { "${it.targetType}:${it.targetId}" }.toSet()

                when (view) {
                    PermissionView.List -> {
                        if (target.kind == "channel" && current.categoryId != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, BubbleBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    if (current.synced) "Synced with its category" else "Not synced with its category",
                                    color = Ink,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (!current.synced) {
                                    OutlinedButton(
                                        enabled = !busy,
                                        onClick = {
                                            scope.launch {
                                                busy = true
                                                try {
                                                    state = app.channels.sync(group.id, target.id)
                                                    onChanged()
                                                } catch (failure: ApiError) {
                                                    error = failure.message.orEmpty()
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderStrong),
                                    ) {
                                        Text("Sync now", color = Chroma, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        (listOf(everyone) + roleEntries + memberEntries).forEach { entry ->
                            EntryRow(entry, labelOf(entry), members) {
                                editing = entry
                                error = ""
                                view = PermissionView.Edit
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderStrong, RoundedCornerShape(12.dp))
                                .pressClickable { view = PermissionView.Add }
                                .padding(12.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_plus), null, tint = Chroma, modifier = Modifier.size(20.dp))
                            Text("Add role or member", color = Chroma, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (error.isNotEmpty()) Text(error, color = Danger, fontSize = 13.5.sp)
                    }
                    PermissionView.Add -> {
                        val addable = roles.filter { "role:${it.id}" !in taken }.map { ChannelOverride("role", it.id, emptyList(), emptyList()) } +
                            members.filter { it.role != "owner" && "member:${it.id}" !in taken }.map { ChannelOverride("member", it.id, emptyList(), emptyList()) }
                        if (addable.isEmpty()) Text("Everyone already has an entry", color = InkMuted, fontSize = 14.sp)
                        addable.forEach { entry ->
                            EntryRow(entry, labelOf(entry), members) {
                                editing = entry
                                error = ""
                                view = PermissionView.Edit
                            }
                        }
                    }
                    PermissionView.Edit -> editing?.let { entry ->
                        CHANNEL_PERMISSIONS.forEach { permission ->
                            val locked = !full && !current.permissions.contains(permission.key)
                            val value = when {
                                entry.allow.contains(permission.key) -> "allow"
                                entry.deny.contains(permission.key) -> "deny"
                                else -> "inherit"
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(permission.label, color = Ink, fontSize = 15.sp)
                                    Text(permission.hint, color = InkMuted, fontSize = 12.5.sp)
                                }
                                Row(
                                    modifier = Modifier
                                        .alpha(if (locked) 0.45f else 1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderStrong, RoundedCornerShape(10.dp)),
                                ) {
                                    listOf("deny" to "✕", "inherit" to "/", "allow" to "✓").forEach { (option, mark) ->
                                        val selected = value == option
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(34.dp)
                                                .background(
                                                    when {
                                                        !selected -> Panel
                                                        option == "deny" -> Danger
                                                        option == "allow" -> AllowGreen
                                                        else -> SurfaceStrong
                                                    },
                                                )
                                                .pressClickable(enabled = !locked) {
                                                    editing = entry.copy(
                                                        allow = entry.allow.filter { it != permission.key } + if (option == "allow") listOf(permission.key) else emptyList(),
                                                        deny = entry.deny.filter { it != permission.key } + if (option == "deny") listOf(permission.key) else emptyList(),
                                                    )
                                                },
                                        ) {
                                            Text(mark, color = if (selected && option != "inherit") Color.White else if (selected) Ink else InkMuted, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        if (error.isNotEmpty()) Text(error, color = Danger, fontSize = 13.5.sp)
                        Button(
                            onClick = { save(entry) },
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Chroma),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        if (entry.targetType != "everyone" && "${entry.targetType}:${entry.targetId}" in taken) {
                            OutlinedButton(
                                onClick = { save(entry.copy(allow = emptyList(), deny = emptyList())) },
                                enabled = !busy,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderStrong),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                            ) {
                                Text("Remove override", color = Danger, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: ChannelOverride, label: Pair<String, String?>, members: List<RoomMember>, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        if (entry.targetType == "member") {
            val member = members.firstOrNull { it.id == entry.targetId }
            Avatar(label.first, 28.dp, disabled = member?.disabled ?: false, deleted = member?.deleted ?: false)
        } else {
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (entry.targetType == "everyone") Chroma else roleColor(label.second) ?: SurfaceStrong),
            )
        }
        Text(
            text = label.first,
            color = roleColor(label.second) ?: Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            when (entry.targetType) {
                "everyone" -> "All members"
                "role" -> "Role"
                else -> "Member"
            },
            color = InkMuted,
            fontSize = 12.5.sp,
        )
    }
}
