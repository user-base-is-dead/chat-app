package com.civenth.app.ui.chats

import androidx.annotation.DrawableRes
import com.civenth.app.ui.theme.AvatarBlankIcon
import com.civenth.app.ui.theme.AvatarBlank
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
import com.civenth.app.R
import com.civenth.app.data.Chat
import com.civenth.app.data.Photos
import com.civenth.app.ui.theme.Canvas
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.DockAccent
import com.civenth.app.ui.theme.Online
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.MutedBadge
import com.civenth.app.ui.theme.MutedIcon
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.Surface
import com.civenth.app.ui.theme.pressScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatAction(
    val label: String,
    val danger: Boolean = false,
    @DrawableRes val icon: Int? = null,
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

private val dateTimeFormat by lazy { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

fun formatDateTime(ms: Long): String = dateTimeFormat.format(Date(ms))

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
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    val marked = disabled || deleted
    val letters = remember(name) { initials(name) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                when {
                    marked -> androidx.compose.ui.graphics.SolidColor(Surface)
                    letters.isNotEmpty() -> androidx.compose.ui.graphics.Brush.verticalGradient(tone(name.trim()))
                    else -> androidx.compose.ui.graphics.SolidColor(AvatarBlank)
                },
            ),
    ) {
        when {
            deleted -> Icon(painterResource(R.drawable.ic_deleted_user), "Deleted account", tint = InkMuted)
            disabled -> Icon(painterResource(R.drawable.ic_disabled_user), "Disabled account", tint = InkMuted)
            letters.isNotEmpty() -> Text(
                text = letters,
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = (size.value * if (letters.length > 1) 0.38f else 0.44f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            else -> Icon(painterResource(R.drawable.ic_person_fill), name, tint = AvatarBlankIcon, modifier = Modifier.fillMaxSize())
        }
        if (photo.isNotEmpty()) RemotePhoto(photo)
    }
}

private val tones = listOf(
    listOf(Color(0xFFF28B6B), Color(0xFFD9573F)),
    listOf(Color(0xFFF5C26B), Color(0xFFE39A3B)),
    listOf(Color(0xFF8FA8F5), Color(0xFF5B6FE0)),
    listOf(Color(0xFF8FD19E), Color(0xFF4FA866)),
    listOf(Color(0xFF6FD3C9), Color(0xFF2FA89B)),
    listOf(Color(0xFF7CC3F0), Color(0xFF3A8FD6)),
    listOf(Color(0xFFD9A0E8), Color(0xFFB067CC)),
    listOf(Color(0xFFF29BB5), Color(0xFFD9628A)),
)

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { String(Character.toChars(it.codePointAt(0))) }.uppercase()

fun tone(name: String): List<Color> {
    var hash = 0
    for (c in name) hash = hash * 31 + c.code
    return tones[Math.floorMod(hash, tones.size)]
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
    withDate: Boolean = false,
    preview: String? = if (chat.kind == "group" && chat.lastSender.isNotEmpty()) "${chat.lastSender}: ${chat.lastText}" else chat.lastText,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
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
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Surface else Color.Transparent)
            .combinedClickable(
                interactionSource = source,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongPress ?: if (actionsFor == null) null else ({ menuOpen = true }),
            )
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        Box {
            Avatar(chat.name, 58.dp, disabled = chat.peerDisabled, deleted = chat.peerDeleted, photo = chat.photo)
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Chroma),
                ) {
                    Icon(painterResource(R.drawable.ic_tick), null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            } else if (chat.peerOnline && !chat.isRoom) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 1.dp, bottom = 1.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Canvas)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Online),
                )
            }
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.SemiBold,
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
                val time = chat.lastAt?.let { if (withDate) formatDateTime(it) else formatTime(it) }.orEmpty()
                if (chat.pinnedAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = InkMuted,
                            modifier = Modifier.size(13.dp),
                        )
                        if (time.isNotEmpty()) Text(time, color = InkMuted, fontSize = 12.5.sp)
                    }
                } else {
                    Text(
                        text = time,
                        color = if (chat.unreadCount > 0 && !chat.muted) DockAccent else InkMuted,
                        fontSize = 13.sp,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = highlighted(preview.orEmpty(), needle),
                    color = InkMuted,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.unreadCount > 0) {
                    CountBadge(chat.unreadCount, muted = chat.muted)
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
