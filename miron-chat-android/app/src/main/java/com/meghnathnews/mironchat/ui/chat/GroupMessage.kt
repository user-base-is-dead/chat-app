package com.meghnathnews.mironchat.ui.chat

import com.meghnathnews.mironchat.ui.theme.roleColor
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.chats.formatTime
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel
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
fun DayLine(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderStrong))
        Text(
            text = label,
            color = InkMuted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Panel)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderStrong))
    }
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
    nameColor: String? = null,
    canDelete: Boolean,
    onCopy: () -> Unit,
    onResend: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val name = if (message.mine) selfName else message.senderName.ifEmpty { "Deleted Account" }
    val shape = RoundedCornerShape(12.dp)
    val pending = message.mine && message.state != SendState.Sent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (start) 10.dp else 2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (focused) Chroma.copy(alpha = 0.18f) else Panel.copy(alpha = 0.55f))
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                .padding(start = 10.dp, end = 12.dp, top = if (start) 8.dp else 6.dp, bottom = if (start) 8.dp else 6.dp),
        ) {
            Box(modifier = Modifier.width(40.dp)) {
                if (start) Avatar(name, 40.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                if (start) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = name,
                            color = roleColor(nameColor) ?: Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(remember(message.createdAt) { formatTime(message.createdAt) }, color = InkMuted, fontSize = 12.sp)
                        if (pending) GroupTick(message.state)
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = message.text,
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!start && pending) GroupTick(message.state)
                }
            }
        }
        if (menuOpen) DropdownMenu(
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
