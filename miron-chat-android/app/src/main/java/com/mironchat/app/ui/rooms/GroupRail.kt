package com.mironchat.app.ui.rooms

import com.mironchat.app.ui.theme.Surface

import com.mironchat.app.ui.theme.Ink

import com.mironchat.app.ui.theme.Danger

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.animateDpAsState

import com.mironchat.app.ui.theme.pressClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.R
import com.mironchat.app.data.Chat
import com.mironchat.app.ui.chat.MenuItem
import com.mironchat.app.ui.chats.Avatar
import com.mironchat.app.ui.chats.ChatAction
import com.mironchat.app.ui.chats.formatCount
import com.mironchat.app.ui.theme.BorderStrong
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Canvas
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.Panel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupRail(
    groups: List<Chat>,
    selectedId: String?,
    bottomSpace: PaddingValues,
    actionsFor: (Chat) -> List<ChatAction>,
    onSelect: (Chat) -> Unit,
    onNew: () -> Unit,
) {
    val invites = groups.filter { it.status == "request" }
    val joined = groups.filter { it.status == "active" }
    var menuFor by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = bottomSpace.calculateBottomPadding()),
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight(),
    ) {
        items(invites + joined, key = { it.id }) { group ->
            val invite = group.status == "request"
            val selected = group.id == selectedId
            val unread = !invite && group.unreadCount > 0
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val corner by animateDpAsState(if (selected || pressed) 16.dp else 24.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "corner")
            val marker by animateDpAsState(
                targetValue = when {
                    selected -> 36.dp
                    pressed -> 20.dp
                    unread || invite -> 8.dp
                    else -> 0.dp
                },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                label = "marker",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(72.dp)
                    .height(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .height(marker)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(Ink),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(corner))
                        .combinedClickable(
                            interactionSource = source,
                            indication = null,
                            onClick = { onSelect(group) },
                            onLongClick = { if (!invite) menuFor = group.id },
                        ),
                ) {
                    Avatar(group.name, 48.dp, photo = group.photo, shape = RoundedCornerShape(corner))
                }
                if (invite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 10.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Surface)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Chroma),
                    )
                } else if (unread) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 6.dp)
                            .clip(CircleShape)
                            .background(Surface)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(if (group.muted) InkMuted else Danger)
                            .heightIn(min = 18.dp)
                            .widthIn(min = 18.dp)
                            .padding(horizontal = 5.dp),
                    ) {
                        Text(formatCount(group.unreadCount), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (menuFor == group.id) DropdownMenu(
                    expanded = true,
                    onDismissRequest = { menuFor = null },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = Panel,
                ) {
                    actionsFor(group).forEach { action ->
                        MenuItem(action.label, danger = action.danger) {
                            menuFor = null
                            action.onClick()
                        }
                    }
                }
            }
        }
        item(key = "divider") {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(BorderStrong),
            )
        }
        item(key = "new") {
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val corner by animateDpAsState(if (pressed) 16.dp else 24.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "newCorner")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(if (pressed) Chroma else Panel)
                    .clickable(interactionSource = source, indication = null, onClick = onNew),
            ) {
                Icon(painterResource(R.drawable.ic_plus), "New Group", tint = if (pressed) Color.White else Chroma, modifier = Modifier.size(22.dp))
            }
        }
    }
}
