package com.civenth.app.ui.rooms

import com.civenth.app.ui.theme.DockPill

import com.civenth.app.ui.theme.DockAccent

import androidx.compose.ui.graphics.lerp

import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.offset

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.animateColorAsState

import com.civenth.app.ui.theme.pressClickable
import com.civenth.app.ui.theme.pressCombined
import com.civenth.app.data.can
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.ChannelList
import com.civenth.app.data.ChannelRepository
import com.civenth.app.data.Chat
import com.civenth.app.data.RoomCategory
import com.civenth.app.data.RoomChannel
import com.civenth.app.data.cleanChannelName
import com.civenth.app.ui.chat.MenuItem
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.chats.Centered
import com.civenth.app.ui.chats.formatCount
import com.civenth.app.ui.theme.BorderStrong
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel

private data class Removal(val title: String, val text: String, val action: suspend () -> Unit)

@Composable
fun GroupInvite(group: Chat, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        Avatar(group.name, 88.dp, photo = group.photo)
        Text(group.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Text("${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}", color = InkMuted, fontSize = 13.5.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
            Pill("Decline", filled = false, onClick = onDecline)
            Pill("Accept", filled = true, onClick = onAccept)
        }
    }
}

@Composable
private fun Pill(label: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(if (filled) Chroma else Panel)
            .border(1.dp, if (filled) Chroma else BorderStrong, CircleShape)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Text(label, color = if (filled) Color.White else Ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelColumn(
    group: Chat,
    list: ChannelList?,
    openId: String?,
    channels: ChannelRepository,
    bottomSpace: PaddingValues,
    onOpen: (RoomChannel) -> Unit,
    onManage: (suspend () -> Unit) -> Unit,
) {
    val canManage = group.can("manageChannels")
    var collapsed by remember(group.id) { mutableStateOf(channels.collapsed(group.id)) }
    var menu by remember { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf<RoomChannel?>(null) }
    var naming by remember { mutableStateOf<NameRequest?>(null) }
    var removing by remember { mutableStateOf<Removal?>(null) }
    var permissionsFor by remember { mutableStateOf<PermissionTarget?>(null) }
    val newChannel: (String?) -> Unit = { categoryId ->
        naming = NameRequest("New channel", "Channel name", clean = ::cleanChannelName) { name ->
            onManage { channels.create(group.id, name, categoryId) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp))
            .background(Panel)
            .border(1.dp, BubbleBorder, RoundedCornerShape(topStart = 24.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Chroma, lerp(Chroma, Color.Black, 0.35f))))
                .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(2.dp),
                ) {
                    Avatar(group.name, 46.dp, photo = group.photo, shape = RoundedCornerShape(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
                        Text(
                            text = "${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                        )
                    }
                }
            if (canManage) Box {
                PlusButton("Add to group", Color.White) { menu = "group" }
                if (menu == "group") DropdownMenu(expanded = true, onDismissRequest = { menu = null }, shape = RoundedCornerShape(18.dp), containerColor = Panel) {
                    MenuItem("New channel") {
                        menu = null
                        newChannel(null)
                    }
                    MenuItem("New category") {
                        menu = null
                        naming = NameRequest("New category", "Category name") { name -> onManage { channels.createCategory(group.id, name) } }
                    }
                }
            }
            }
        }
        if (list == null) {
            Centered { CircularProgressIndicator(color = Chroma) }
            return@Column
        }
        LazyColumn(contentPadding = bottomSpace, modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            val row: @Composable (RoomChannel) -> Unit = { channel ->
                val open = channel.id == openId
                val unread = channel.unreadCount > 0
                val tint by animateColorAsState(if (open) DockPill else Color.Transparent, tween(180), label = "channelTint")
                Box {
                    if (unread && !open) Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-8).dp)
                            .width(4.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(Ink),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(vertical = 1.dp)
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tint)
                            .pressCombined(
                                onClick = { onOpen(channel) },
                                onLongClick = {
                                    if (channel.permissions.contains("manageChannel") || channel.permissions.contains("managePermissions")) menu = channel.id
                                },
                            )
                            .padding(horizontal = 10.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (channel.isPrivate) R.drawable.ic_lock else R.drawable.ic_hash),
                            contentDescription = if (channel.isPrivate) "Private channel" else null,
                            tint = if (open) DockAccent else if (unread) Ink else InkMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = channel.name,
                            color = if (open) DockAccent else if (unread) Ink else InkMuted,
                            fontSize = 15.sp,
                            fontWeight = if (unread || open) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (unread && !open) Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clip(CircleShape).background(Danger).heightIn(min = 20.dp).widthIn(min = 20.dp).padding(horizontal = 6.dp),
                        ) {
                            Text(formatCount(channel.unreadCount), color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (menu == channel.id) DropdownMenu(expanded = true, onDismissRequest = { menu = null }, shape = RoundedCornerShape(18.dp), containerColor = Panel) {
                        if (channel.permissions.contains("managePermissions")) MenuItem("Permissions") {
                            menu = null
                            permissionsFor = PermissionTarget("channel", channel.id, "# ${channel.name}")
                        }
                        if (channel.permissions.contains("manageChannel")) MenuItem("Rename") {
                            menu = null
                            naming = NameRequest("Rename channel", "Channel name", channel.name, ::cleanChannelName) { name ->
                                onManage { channels.rename(group.id, channel.id, name) }
                            }
                        }
                        if (channel.permissions.contains("manageChannel")) MenuItem("Move up") {
                            menu = null
                            onManage { channels.move(group.id, channel.id, true) }
                        }
                        if (channel.permissions.contains("manageChannel")) MenuItem("Move down") {
                            menu = null
                            onManage { channels.move(group.id, channel.id, false) }
                        }
                        if (channel.permissions.contains("manageChannel") && list.categories.isNotEmpty()) MenuItem("Move to category") {
                            menu = null
                            moving = channel
                        }
                        if (channel.permissions.contains("manageChannel") && channel.id != group.id) MenuItem("Delete", danger = true) {
                            menu = null
                            removing = Removal("Delete channel?", "All messages in #${channel.name} will be deleted for everyone.") {
                                channels.delete(group.id, channel.id)
                            }
                        }
                    }
                }
            }
            if (list.channels.any { it.categoryId == null }) item(key = "loose-title") {
                Text(
                    text = "CHANNELS",
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 4.dp),
                )
            }
            list.channels.filter { it.categoryId == null }.forEach { channel ->
                item(key = channel.id) { Box(Modifier.animateItem()) { row(channel) } }
            }
            list.categories.forEach { category ->
                val closed = category.id in collapsed
                val inside = list.channels.filter { it.categoryId == category.id }
                item(key = "category-${category.id}") {
                    CategoryRow(
                        category = category,
                        closed = closed,
                        canManage = canManage,
                        menuOpen = menu == category.id,
                        onToggle = {
                            collapsed = if (closed) collapsed - category.id else collapsed + category.id
                            channels.saveCollapsed(group.id, collapsed)
                        },
                        onMenu = { menu = if (it) category.id else null },
                        onNewChannel = { newChannel(category.id) },
                        onRename = {
                            naming = NameRequest("Rename category", "Category name", category.name) { name ->
                                onManage { channels.renameCategory(group.id, category.id, name) }
                            }
                        },
                        onMove = { up -> onManage { channels.moveCategory(group.id, category.id, up) } },
                        onPermissions = if (category.permissions.contains("managePermissions")) {
                            { permissionsFor = PermissionTarget("category", category.id, category.name) }
                        } else {
                            null
                        },
                        onDelete = {
                            removing = Removal("Delete category?", "${category.name} will be removed. Its channels stay in the group.") {
                                channels.deleteCategory(group.id, category.id)
                            }
                        },
                    )
                }
                (if (closed) inside.filter { it.unreadCount > 0 || it.id == openId } else inside).forEach { channel ->
                    item(key = channel.id) { Box(Modifier.animateItem()) { row(channel) } }
                }
            }
        }
    }

    moving?.let { channel ->
        AlertDialog(
            onDismissRequest = { moving = null },
            containerColor = Panel,
            title = { Text("Move #${channel.name}", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    (listOf<RoomCategory?>(null) + (list?.categories ?: emptyList())).forEach { category ->
                        Text(
                            text = category?.name ?: "No category",
                            color = Ink,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .pressClickable {
                                    moving = null
                                    onManage { channels.setCategory(group.id, channel.id, category?.id) }
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { moving = null }) { Text("Cancel", color = Ink) } },
        )
    }

    naming?.let { request -> NameDialog(request) { naming = null } }

    permissionsFor?.let { target ->
        ChannelPermissionsDialog(group = group, target = target, onClose = { permissionsFor = null }, onChanged = { onManage {} })
    }

    removing?.let { removal ->
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = Panel,
            title = { Text(removal.title, color = Ink) },
            text = { Text(removal.text, color = InkMuted) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    onManage(removal.action)
                }) {
                    Text("Yes", color = Danger)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("No", color = Ink) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRow(
    category: RoomCategory,
    closed: Boolean,
    canManage: Boolean,
    menuOpen: Boolean,
    onToggle: () -> Unit,
    onMenu: (Boolean) -> Unit,
    onNewChannel: () -> Unit,
    onRename: () -> Unit,
    onMove: (Boolean) -> Unit,
    onPermissions: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().height(36.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onToggle, onLongClick = { if (canManage) onMenu(true) })
                    .padding(vertical = 8.dp, horizontal = 2.dp),
            ) {
                val turn by animateFloatAsState(if (closed) 0f else 90f, spring(dampingRatio = 0.75f, stiffness = 500f), label = "chevron")
                Icon(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = if (closed) "Expand" else "Collapse",
                    tint = InkMuted,
                    modifier = Modifier.size(14.dp).rotate(turn),
                )
                Text(
                    text = category.name.uppercase(),
                    color = InkMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canManage) PlusButton("New channel in ${category.name}", onClick = onNewChannel)
        }
        if (menuOpen) DropdownMenu(expanded = true, onDismissRequest = { onMenu(false) }, shape = RoundedCornerShape(18.dp), containerColor = Panel) {
            val choose: (() -> Unit) -> () -> Unit = { action ->
                {
                    onMenu(false)
                    action()
                }
            }
            MenuItem("New channel", onClick = choose(onNewChannel))
            MenuItem("Rename", onClick = choose(onRename))
            MenuItem("Move up", onClick = choose { onMove(true) })
            MenuItem("Move down", onClick = choose { onMove(false) })
            if (onPermissions != null) MenuItem("Permissions", onClick = choose(onPermissions))
            MenuItem("Delete", danger = true, onClick = choose(onDelete))
        }
    }
}

@Composable
private fun PlusButton(label: String, tint: Color = InkMuted, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clip(CircleShape).pressClickable(onClick = onClick),
    ) {
        Icon(painterResource(R.drawable.ic_plus), label, tint = tint, modifier = Modifier.size(18.dp))
    }
}
