package com.civenth.app.ui.chat

import com.civenth.app.data.REACTIONS
import com.civenth.app.data.PostStats
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.chats.formatTime
import com.civenth.app.ui.theme.BubbleMine
import com.civenth.app.ui.theme.BubbleMineFailed
import com.civenth.app.ui.theme.BubbleMineMuted
import com.civenth.app.ui.theme.BubbleMineText
import com.civenth.app.ui.theme.BubbleMuted
import com.civenth.app.ui.theme.BubbleOther
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.bouncySpring

private val tailFill = PathParser()
    .parsePathString("M10.5 0C10.5 6 7.5 10.5 2 13.2C0.6 13.9 1 15.5 2.6 15.5H12V0Z")
    .toPath()
private fun ContentDrawScope.drawTail(mine: Boolean) {
    val unit = 1.dp.toPx()
    val top = size.height - 16 * unit
    val left = if (mine) size.width + 10 * unit else -10 * unit
    translate(left, top) {
        scale(if (mine) -unit else unit, unit, pivot = Offset.Zero) {
            drawPath(tailFill, if (mine) BubbleMine else BubbleOther)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    read: Boolean,
    groupStart: Boolean,
    groupEnd: Boolean,
    animateIn: Boolean,
    focused: Boolean,
    showSender: Boolean = false,
    firstOfSender: Boolean = false,
    hideTicks: Boolean = false,
    actions: () -> List<MessageAction> = { emptyList() },
    channelName: String? = null,
    channelPhoto: String = "",
    stats: PostStats? = null,
    canManagePosts: Boolean = false,
    onReact: (String) -> Unit = {},
    onOpenComments: () -> Unit = {},
    onTogglePostSwitch: (Boolean) -> Unit = {},
    onEditPost: () -> Unit = {},
) {
    val post = channelName != null && message.state == SendState.Sent
    val mine = message.mine && channelName == null
    val senderLabel = channelName ?: message.senderName.ifEmpty { "Deleted Account" }
    var menuOpen by remember { mutableStateOf(false) }
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    val ring by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(if (focused) 120 else 1_200),
        label = "ring",
    )
    val big = 18.dp
    val small = 18.dp
    val shape = remember(mine, groupStart, groupEnd) {
    if (mine) {
        RoundedCornerShape(
            topStart = big,
            topEnd = if (groupStart) big else small,
            bottomEnd = if (groupEnd) 0.dp else small,
            bottomStart = big,
        )
    } else {
        RoundedCornerShape(
            topStart = if (groupStart) big else small,
            topEnd = big,
            bottomEnd = big,
            bottomStart = if (groupEnd) 0.dp else small,
        )
    }
    }
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.76f).dp

    LaunchedEffect(Unit) {
        if (animateIn) appear.animateTo(1f, bouncySpring())
    }

    Box(
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (groupStart) 10.dp else 0.dp)
            .graphicsLayer {
                val progress = appear.value
                alpha = progress.coerceIn(0f, 1f)
                scaleX = 0.82f + 0.18f * progress
                scaleY = 0.82f + 0.18f * progress
                translationY = (1f - progress) * 18.dp.toPx()
                transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
            },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
        if (showSender) {
            Box(modifier = Modifier.padding(end = 10.dp).size(32.dp)) {
                if (groupEnd) Avatar(senderLabel, 32.dp, photo = channelPhoto)
            }
        }
        Box {
            Column(
                modifier = Modifier
                    .then(if (post) Modifier.width(maxWidth) else Modifier.widthIn(max = maxWidth))
                    .drawWithContent {
                        drawContent()
                        if (groupEnd) drawTail(mine)
                    }
                    .background(if (mine) BubbleMine else BubbleOther, shape)
                    .drawBehind {
                        if (ring == 0f) return@drawBehind
                        drawOutline(shape.createOutline(size, layoutDirection, this), Chroma.copy(alpha = 0.28f * ring))
                    }
                    .combinedClickable(onClick = { menuOpen = true }, onLongClick = { menuOpen = true })
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            ) {
                if (showSender && firstOfSender) {
                    Text(
                        text = senderLabel,
                        color = Chroma,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                message.forwardedFrom?.let { ForwardedLabel(it.name, if (mine) BubbleMineMuted else BubbleMuted) }
                if (post) {
                    Text(text = withEdited(message.text, message.editedAt != null, BubbleMuted), color = Ink, fontSize = 18.sp, lineHeight = 25.sp)
                    PostFooter(
                        stats = stats,
                        createdAt = message.createdAt,
                        onReact = onReact,
                        onOpenComments = onOpenComments,
                        onEditPost = if (canManagePosts) onEditPost else null,
                        onToggleSwitch = onTogglePostSwitch,
                    )
                } else Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Text(
                    text = withEdited(message.text, message.editedAt != null, if (mine) BubbleMineMuted else BubbleMuted),
                    color = if (mine) BubbleMineText else Ink,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = remember(message.createdAt) { formatTime(message.createdAt) },
                        color = if (mine) BubbleMineMuted else BubbleMuted,
                        fontSize = 13.5.sp,
                    )
                    if (mine && !(hideTicks && message.state == SendState.Sent)) StatusMark(message.state, read)
                }
                }
            }
            if (menuOpen) {
                ActionMenu(
                    actions = actions(),
                    title = if (message.state == SendState.Failed) "Message not sent" else null,
                    header = if (post && stats?.allowReactions == true) {
                        {
                            Row(modifier = Modifier.padding(horizontal = 6.dp)) {
                                REACTIONS.forEach { emoji ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (stats.myReaction == emoji) Chroma.copy(alpha = 0.22f) else Color.Transparent)
                                            .clickable {
                                                menuOpen = false
                                                onReact(emoji)
                                            },
                                    ) {
                                        Text(emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    } else null,
                    onDismiss = { menuOpen = false },
                )
            }
        }
        }
    }
}

@Composable
fun MenuItem(label: String, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (danger) Danger else Ink) },
        onClick = onClick,
    )
}

@Composable
private fun StatusMark(state: SendState, read: Boolean) {
    val icon = when {
        state == SendState.Sending -> R.drawable.ic_clock
        state == SendState.Failed -> R.drawable.ic_failed
        read -> R.drawable.ic_double_tick
        else -> R.drawable.ic_tick
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = when {
            state == SendState.Sending -> "Sending"
            state == SendState.Failed -> "Not sent"
            read -> "Read"
            else -> "Sent"
        },
        tint = if (state == SendState.Failed) BubbleMineFailed else BubbleMineMuted,
        modifier = Modifier.size(if (state == SendState.Sending) 13.dp else 16.dp),
    )
}
