package com.civenth.app.ui.chat

import androidx.compose.ui.graphics.lerp

import androidx.compose.ui.graphics.graphicsLayer



import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect

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
private fun FailedMark(onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.ic_failed),
        contentDescription = "Not sent",
        tint = Danger,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(2.dp).size(18.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessage(
    message: Message,
    start: Boolean,
    focused: Boolean,
    selfName: String,
    actions: () -> List<MessageAction>,
    selfPhoto: String = "",
    peerName: String = "",
    peerPhoto: String = "",
    nameColor: String? = null,
    replyColor: String? = null,
    selecting: Boolean = false,
    selected: Boolean = false,
    onSelect: () -> Unit = {},
    onSwipeReply: (() -> Unit)? = null,
    onOpenReply: (String) -> Unit = {},
    askMenu: Boolean = false,
    onMenuAsked: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(askMenu) {
        if (!askMenu) return@LaunchedEffect
        menuOpen = true
        onMenuAsked()
    }
    var flash by remember(message.key, selected) { mutableStateOf(0) }
    val shadow = remember(message.key, selected) { Animatable(0f) }
    LaunchedEffect(shadow, flash) {
        if (selected || flash != 0) {
            shadow.animateTo(0.12f, tween(65))
            shadow.animateTo(0f, tween(205))
        }
    }
    val shadowColor = Ink
    val mine = message.mine
    val mark by animateFloatAsState(if (selected) 1f else 0f, tween(160), label = "mark")
    val picking by animateFloatAsState(if (selecting) 1f else 0f, tween(160), label = "picking")
    val name = if (mine) selfName else message.senderName.ifEmpty { peerName.ifEmpty { "Deleted Account" } }
    val photo = if (mine) selfPhoto else peerPhoto
    val failed = mine && message.state == SendState.Failed
    val faded by animateFloatAsState(if (mine && message.state != SendState.Sent) 0.45f else 1f, tween(220), label = "faded")
    val time = remember(message.createdAt) { formatDateTime(message.createdAt) }
    val body = withEdited(message.text, message.editedAt != null, InkMuted)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (start) 17.dp else 0.dp),
    ) {
        SwipeReply(enabled = onSwipeReply != null && !selecting, onReply = { onSwipeReply?.invoke() }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(when {
                        selected -> Ink.copy(alpha = 0.16f)
                        focused -> Chroma.copy(alpha = 0.18f)
                        else -> Color.Transparent
                    })
                    .drawBehind {
                        if (shadow.value > 0f) drawRect(shadowColor.copy(alpha = shadow.value))
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (selecting) onSelect()
                        else {
                            flash++
                            menuOpen = true
                        }
                    },
            ) {
                message.replyTo?.let { reply ->
                    ReplyQuote(reply, roleColor(replyColor) ?: Ink) { onOpenReply(reply.id) }
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .width(40.dp)
                            .height(if (start) 40.dp else 24.dp),
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
                                Icon(painterResource(R.drawable.ic_tick), null, tint = Color.White, modifier = Modifier.size(15.dp).graphicsLayer { alpha = mark; scaleX = 0.8f + 0.2f * mark; scaleY = scaleX })
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
                        message.forwardedFrom?.let { ForwardedLabel(it.name) }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = body,
                                color = Ink.copy(alpha = faded),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 26.sp,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (failed) Box(modifier = Modifier.padding(bottom = 2.dp)) { FailedMark { menuOpen = true } }
                        }
                    }
                }
            }
        }
        if (menuOpen && !selecting) {
            val list = actions()
            if (list.isNotEmpty()) ActionMenu(list, title = if (failed) "Message not sent" else null) { menuOpen = false }
        }
    }
}
