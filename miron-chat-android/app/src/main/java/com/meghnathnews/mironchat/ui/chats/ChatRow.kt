package com.meghnathnews.mironchat.ui.chats

import com.meghnathnews.mironchat.ui.theme.AvatarBlankIcon
import com.meghnathnews.mironchat.ui.theme.AvatarBlank
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.Photos
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedBadge
import com.meghnathnews.mironchat.ui.theme.MutedIcon
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.Surface
import com.meghnathnews.mironchat.ui.theme.pressScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatAction(
    val label: String,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

private val avatarColors = listOf(
    Color(0xFFD02670),
    Color(0xFFC2410C),
    Color(0xFF6D4AFF),
    Color(0xFF0E7490),
    Color(0xFF1F6FEB),
    Color(0xFFA21CAF),
    Color(0xFF2E8540),
)

private fun avatarColor(name: String): Color {
    var sum = 0
    var index = 0
    while (index < name.length) {
        sum += name[index].code
        index += Character.charCount(name.codePointAt(index))
    }
    return avatarColors[sum % avatarColors.size]
}

private fun firstLetter(name: String): String =
    if (name.isEmpty()) "" else name.substring(0, name.offsetByCodePoints(0, 1)).uppercase()

private val timeFormat by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }

fun formatTime(ms: Long): String = timeFormat.format(Date(ms))

private fun shorten(value: Double, unit: String): String {
    val rounded = kotlin.math.floor(value * 10) / 10
    val number = if (value < 10) rounded else kotlin.math.floor(value)
    return if (number % 1.0 == 0.0) "${number.toLong()}$unit" else "$number$unit"
}

fun formatCount(count: Int): String = when {
    count < 1_000 -> count.toString()
    count < 1_000_000 -> shorten(count / 1_000.0, "K")
    else -> shorten(count / 1_000_000.0, "M")
}

fun highlighted(text: String, needle: String): AnnotatedString = buildAnnotatedString {
    val start = if (needle.isEmpty()) -1 else text.indexOf(needle, ignoreCase = true)
    if (start < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text.substring(0, start))
    withStyle(SpanStyle(color = Chroma, fontWeight = FontWeight.Bold)) {
        append(text.substring(start, start + needle.length))
    }
    append(text.substring(start + needle.length))
}

@Composable
fun Avatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    deleted: Boolean = false,
    photo: String = "",
) {
    val marked = disabled || deleted
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (marked) Surface else AvatarBlank),
    ) {
        when {
            deleted -> Icon(painterResource(R.drawable.ic_deleted_user), "Deleted account", tint = InkMuted)
            disabled -> Icon(painterResource(R.drawable.ic_disabled_user), "Disabled account", tint = InkMuted)
            else -> Icon(painterResource(R.drawable.ic_person_fill), name, tint = AvatarBlankIcon, modifier = Modifier.fillMaxSize())
        }
        if (photo.isNotEmpty()) RemotePhoto(photo)
    }
}

@Composable
private fun RemotePhoto(path: String) {
    val image by produceState<ImageBitmap?>(null, path) { value = Photos.load(path) }
    image?.let {
        Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(
    chat: Chat,
    actionsFor: ((Chat) -> List<ChatAction>)?,
    modifier: Modifier = Modifier,
    needle: String = "",
    preview: String? = if (chat.kind == "group" && chat.lastSender.isNotEmpty()) "${chat.lastSender}: ${chat.lastText}" else chat.lastText,
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .pressScale(source)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = source,
                indication = ripple(),
                onClick = onClick,
                onLongClick = if (actionsFor == null) null else ({ menuOpen = true }),
            )
            .padding(8.dp),
    ) {
        Box {
            Avatar(chat.name, 52.dp, disabled = chat.peerDisabled, deleted = chat.peerDeleted, photo = chat.photo)
            if (menuOpen && actionsFor != null) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = Panel,
                ) {
                    actionsFor(chat).forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.label, color = if (action.danger) Danger else Ink) },
                            onClick = {
                                menuOpen = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = highlighted(chat.name, needle),
                        color = if (chat.peerDisabled) MutedIcon else if (chat.peerDeleted) InkMuted else Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (chat.peerDisabled) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (chat.muted) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mute),
                            contentDescription = "Muted",
                            tint = MutedIcon,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp),
                        )
                    }
                }
                Text(
                    text = chat.lastAt?.let(::formatTime).orEmpty(),
                    color = InkMuted,
                    fontSize = 12.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = highlighted(preview.orEmpty(), needle),
                    color = InkMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unreadCount > 0) {
                    CountBadge(chat.unreadCount, muted = chat.muted)
                } else if (chat.pinnedAt != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = "Pinned",
                        tint = InkMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CountBadge(count: Int, muted: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(24.dp)
            .defaultMinSize(minWidth = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (muted) MutedBadge else Chroma)
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = formatCount(count),
            color = if (muted) InkMuted else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
