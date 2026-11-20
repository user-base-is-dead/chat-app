package com.meghnathnews.mironchat.ui.rooms

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
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.ui.chat.MenuItem
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.chats.ChatAction
import com.meghnathnews.mironchat.ui.chats.formatCount
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import com.meghnathnews.mironchat.ui.theme.Canvas
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel

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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = bottomSpace,
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .drawBehind { drawLine(BubbleBorder, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(top = 4.dp),
    ) {
        items(invites + joined, key = { it.id }) { group ->
            val invite = group.status == "request"
            val selected = group.id == selectedId
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(64.dp)
                    .height(50.dp)
                    .drawBehind {
                        if (selected) drawRoundRect(
                            color = Chroma,
                            topLeft = Offset(0f, 8.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height - 16.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        )
                    }
                    .combinedClickable(
                        onClick = { onSelect(group) },
                        onLongClick = { if (!invite) menuFor = group.id },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, if (selected) Chroma else Color.Transparent, CircleShape)
                        .padding(2.dp),
                ) {
                    Avatar(group.name, 44.dp, photo = group.photo)
                }
                if (invite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Canvas)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Chroma),
                    )
                } else if (group.unreadCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(Canvas)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (group.muted) InkMuted else Chroma)
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
        item(key = "new") {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Panel)
                    .border(1.dp, BorderStrong, CircleShape)
                    .clickable(onClick = onNew),
            ) {
                Icon(painterResource(R.drawable.ic_plus), "New Group", tint = Chroma, modifier = Modifier.size(20.dp))
            }
        }
    }
}
