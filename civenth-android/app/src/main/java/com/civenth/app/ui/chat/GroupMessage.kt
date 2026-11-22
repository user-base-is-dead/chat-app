package com.civenth.app.ui.chat

import androidx.compose.ui.graphics.lerp

import androidx.compose.ui.graphics.graphicsLayer



import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.text.selection.SelectionContainer

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.fadeOut

import androidx.compose.animation.fadeIn



import androidx.compose.animation.AnimatedVisibility

import com.civenth.app.ui.theme.roleColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.SoftShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.chats.formatTime
import com.civenth.app.ui.chats.formatDateTime
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel
import androidx.compose.foundation.layout.Spacer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val RUN_GAP = 5 * 60 * 1000L

fun sameDay(a: Long, b: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = a }
    val second = Calendar.getInstance().apply { timeInMillis = b }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

fun dayLabel(ms: Long): String {
    val now = System.currentTimeMillis()
    if (sameDay(ms, now)) return "Today"
    if (sameDay(ms, now - 24 * 60 * 60 * 1000L)) return "Yesterday"
    val sameYear = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    return SimpleDateFormat(if (sameYear) "d MMM" else "d MMM yyyy", Locale.getDefault()).format(ms)
}

fun joins(a: Message?, b: Message?): Boolean {
    if (a == null || b == null) return false
    val sameSender = if (a.mine) b.mine else !b.mine && a.senderId == b.senderId
    return sameSender && b.createdAt - a.createdAt <= RUN_GAP && sameDay(a.createdAt, b.createdAt)
}

@Composable
fun DayLine(label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 6.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BubbleBorder))
        FloatingDay(label)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BubbleBorder))
    }
}

@Composable
fun FloatingDay(label: String) {
    Text(
        text = label,
        color = Ink,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(50), ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(RoundedCornerShape(50))
            .background(Card)
            .border(1.dp, BubbleBorder, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 5.dp),
    )
}

@Composable
private fun GroupTick(state: SendState) {
    val icon = if (state == SendState.Sending) R.drawable.ic_clock else R.drawable.ic_failed
    Icon(
        painter = painterResource(icon),
        contentDescription = if (state == SendState.Sending) "Sending" else "Not sent",
        tint = if (state == SendState.Failed) Danger else InkMuted,
        modifier = Modifier.size(if (state == SendState.Sending) 13.dp else 16.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessage(
    message: Message,
    start: Boolean,
    focused: Boolean,
    selfName: String,
    selfPhoto: String = "",
    peerName: String = "",
    peerPhoto: String = "",
    nameColor: String? = null,
    canDelete: Boolean,
    selecting: Boolean = false,
    selected: Boolean = false,
    onSelect: () -> Unit = {},
    onCopy: () -> Unit,
    onResend: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val mine = message.mine
    val mark by animateFloatAsState(if (selected) 1f else 0f, tween(160), label = "mark")
    val picking by animateFloatAsState(if (selecting) 1f else 0f, tween(160), label = "picking")
    val name = if (mine) selfName else message.senderName.ifEmpty { peerName.ifEmpty { "Deleted Account" } }
    val photo = if (mine) selfPhoto else peerPhoto
    val pending = mine && message.state != SendState.Sent
    val time = remember(message.createdAt) { formatDateTime(message.createdAt) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (start) 17.dp else 0.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        focused -> Chroma.copy(alpha = 0.18f)
                        else -> Chroma.copy(alpha = 0.16f * mark)
                    },
                )
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (selecting) onSelect() },
                    onLongClick = { if (!selecting) menuOpen = true },
                )
                .padding(horizontal = 8.dp, vertical = 1.dp),
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .width(40.dp)
                    .height(if (start) 40.dp else 22.dp),
            ) {
                if (start) {
                    Box(modifier = Modifier.graphicsLayer { alpha = 1f - picking }) {
                        Avatar(name, 40.dp, photo = photo)
                    }
                }
                if (picking > 0f) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = if (start) 9.dp else 0.dp)
                            .size(24.dp)
                            .graphicsLayer { alpha = picking }
                            .clip(CircleShape)
                            .background(Chroma.copy(alpha = mark))
                            .border(1.6.dp, lerp(InkMuted, Chroma, mark), CircleShape),
                    ) {
                        if (selected) Icon(painterResource(R.drawable.ic_tick), null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (start) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = name,
                            color = roleColor(nameColor) ?: Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(text = time, color = InkMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = if (message.state == SendState.Sending) InkMuted else Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (pending) Box(modifier = Modifier.padding(bottom = 4.dp)) { GroupTick(message.state) }
                }
            }
        }
        if (menuOpen && !selecting) DropdownMenu(
            expanded = true,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Panel,
        ) {
            if (message.state == SendState.Failed) {
                Text(
                    text = "Message not sent",
                    color = InkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            when (message.state) {
                SendState.Failed -> MenuItem("Resend") {
                    menuOpen = false
                    onResend()
                }
                SendState.Sending -> Unit
                SendState.Sent -> MenuItem("Copy") {
                    menuOpen = false
                    onCopy()
                }
            }
            if (message.mine || canDelete) {
                MenuItem(if (message.state == SendState.Sending) "Cancel Sending" else "Delete", danger = true) {
                    menuOpen = false
                    onDelete()
                }
            }
        }
    }
}
