package com.civenth.app.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.ReplyRef
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.SoftShadow
import com.civenth.app.ui.theme.pressClickable
import com.civenth.app.ui.theme.softSpring
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ReplyReach = 76.dp
private val ReplyTrigger = 58.dp

data class MessageAction(
    @param:DrawableRes val icon: Int,
    val label: String,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

sealed interface InputMode {
    val message: Message

    data class Reply(override val message: Message) : InputMode
    data class Edit(override val message: Message) : InputMode
}

fun Message.asReply(name: String): ReplyRef = ReplyRef(id, senderId, name, text, deleted = false)

fun withEdited(text: String, edited: Boolean, color: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    if (edited) {
        withStyle(SpanStyle(color = color, fontSize = 12.5.sp)) { append("  (edited)") }
    }
}

@Composable
fun ActionMenu(
    actions: List<MessageAction>,
    title: String? = null,
    header: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Panel,
        modifier = Modifier.widthIn(min = 210.dp),
    ) {
        header?.invoke()
        if (title != null) {
            Text(
                text = title,
                color = InkMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 4.dp),
            )
        }
        actions.forEach { action ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        action.onClick()
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    contentDescription = null,
                    tint = if (action.danger) Danger else Ink,
                    modifier = Modifier.size(22.dp),
                )
                Text(action.label, color = if (action.danger) Danger else Ink, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SwipeReply(enabled: Boolean, onReply: () -> Unit, content: @Composable () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val reach = with(density) { ReplyReach.toPx() }
    val trigger = with(density) { ReplyTrigger.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val reply by rememberUpdatedState(onReply)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var leftward = false
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over < 0f) {
                            leftward = true
                            change.consume()
                        }
                    }
                    if (start == null || !leftward) return@awaitEachGesture
                    var armed = false
                    var position = 0f
                    horizontalDrag(start.id) { change ->
                        position = (position + change.positionChange().x).coerceIn(-reach, 0f)
                        change.consume()
                        scope.launch { offset.snapTo(position) }
                        val ready = -position >= trigger
                        if (ready && !armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        armed = ready
                    }
                    if (armed) reply()
                    scope.launch { offset.animateTo(0f, softSpring()) }
                }
            },
    ) {
        val shown = (-offset.value / trigger).coerceIn(0f, 1f)
        if (shown > 0f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .size(34.dp)
                    .graphicsLayer {
                        alpha = shown
                        scaleX = 0.6f + 0.4f * shown
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(Chroma.copy(alpha = 0.16f)),
            ) {
                Icon(painterResource(R.drawable.ic_reply), "Reply", tint = Chroma, modifier = Modifier.size(20.dp))
            }
        }
        Box(modifier = Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }) { content() }
    }
}

@Composable
fun ReplyQuote(reply: ReplyRef, nameColor: Color, onOpen: () -> Unit) {
    val line = InkMuted.copy(alpha = 0.55f)
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 27.dp, end = 8.dp, top = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 3.dp)
                .width(26.dp)
                .height(13.dp)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val path = Path().apply {
                        moveTo(stroke / 2, size.height)
                        quadraticTo(stroke / 2, stroke / 2, size.width * 0.55f, stroke / 2)
                        lineTo(size.width, stroke / 2)
                    }
                    drawPath(path, line, style = Stroke(width = stroke, cap = StrokeCap.Round))
                },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpen)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            if (reply.deleted) {
                Text(
                    text = "Original message was deleted",
                    color = InkMuted,
                    fontSize = 13.5.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = reply.senderName.ifEmpty { "Deleted Account" },
                    color = nameColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = reply.text.replace('\n', ' '),
                    color = InkMuted,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ForwardedLabel(name: String, color: Color = InkMuted) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        Icon(painterResource(R.drawable.ic_forward), null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = "Forwarded from ${name.ifEmpty { "Deleted Account" }}",
            color = color,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InputStrip(mode: InputMode, name: String, onClose: () -> Unit) {
    val editing = mode is InputMode.Edit
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
    ) {
        Icon(
            painter = painterResource(if (editing) R.drawable.ic_edit else R.drawable.ic_reply),
            contentDescription = null,
            tint = Chroma,
            modifier = Modifier.size(20.dp),
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(Chroma),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (editing) "Edit message" else "Reply to ${name.ifEmpty { "Deleted Account" }}",
                color = Chroma,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mode.message.text.replace('\n', ' '),
                color = InkMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
        ) {
            Icon(painterResource(R.drawable.ic_close), "Cancel", tint = InkMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PinnedBar(message: Message, canUnpin: Boolean, onOpen: () -> Unit, onUnpin: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(8.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(shape)
            .background(Panel.copy(alpha = 0.97f))
            .border(1.dp, BubbleBorder, shape)
            .pressClickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_pin), null, tint = Chroma, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Pinned message", color = Chroma, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = message.text.replace('\n', ' '),
                color = Ink,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canUnpin) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onUnpin),
            ) {
                Icon(painterResource(R.drawable.ic_close), "Unpin", tint = InkMuted, modifier = Modifier.size(19.dp))
            }
        }
    }
}
