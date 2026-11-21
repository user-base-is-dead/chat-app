package com.mironchat.app.ui.chat

import com.mironchat.app.ui.theme.pressClickable
import com.mironchat.app.ui.theme.BubbleBorder
import androidx.compose.foundation.border
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mironchat.app.R
import com.mironchat.app.data.Chat
import com.mironchat.app.ui.chats.Avatar
import com.mironchat.app.ui.theme.Appearance
import com.mironchat.app.ui.theme.Cover
import com.mironchat.app.ui.theme.Danger
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.MutedIcon
import com.mironchat.app.ui.theme.Panel
import com.mironchat.app.ui.theme.Surface
import com.mironchat.app.ui.theme.bouncySpring
import kotlinx.coroutines.delay

private const val COPIED_FOR = 1_500L
private const val POP_AFTER = 100L
private val NeutralCover = Color(0xFF615F67)
private val CoverHeight = 158.dp
private val AvatarSize = 104.dp

@Composable
fun UserInfoScreen(
    chat: Chat,
    typing: Boolean,
    onBack: () -> Unit,
    onToggleMute: () -> Unit,
    onRestrict: () -> Unit,
    onUnrestrict: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hidden = chat.peerDisabled || chat.peerDeleted
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    var copied by remember { mutableStateOf<String?>(null) }
    val copy: (String, String) -> Unit = { key, value ->
        copyText(context, value)
        copied = key
    }

    val pop = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        delay(POP_AFTER)
        pop.animateTo(1f, bouncySpring())
    }

    BackHandler(onBack = onBack)

    DisposableEffect(view) {
        val bars = WindowCompat.getInsetsController((view.context as Activity).window, view)
        bars.isAppearanceLightStatusBars = false
        onDispose { bars.isAppearanceLightStatusBars = !Appearance.dark }
    }

    LaunchedEffect(copied) {
        if (copied == null) return@LaunchedEffect
        delay(COPIED_FOR)
        copied = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Panel)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoverHeight + statusTop)
                    .clip(RoundedCornerShape(0.dp))
                    .background(if (hidden) NeutralCover else Cover),
            ) {
                Doodle(color = Color.White, tileSize = 260.dp)
            }
            Box(
                modifier = Modifier
                    .offset(y = -AvatarSize / 2)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = pop.value.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(Panel)
                    .border(1.dp, BubbleBorder, CircleShape)
                    .padding(6.dp),
            ) {
                Avatar(
                    name = chat.name,
                    size = AvatarSize,
                    disabled = chat.peerDisabled,
                    deleted = chat.peerDeleted,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .offset(y = -AvatarSize / 2 + 10.dp)
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = chat.name,
                    color = if (chat.peerDisabled) MutedIcon else if (chat.peerDeleted) InkMuted else Ink,
                    fontSize = 27.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    textDecoration = if (chat.peerDisabled) TextDecoration.LineThrough else null,
                )
                if (!chat.peerDeleted) PeerStatus(chat, typing)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .offset(y = -AvatarSize / 2 + 26.dp)
                    .padding(horizontal = 14.dp),
            ) {
                InfoCard {
                    if (hidden) {
                        InfoRow(if (chat.peerDeleted) "Deleted user" else "Disabled user", "Username", muted = true)
                    } else {
                        InfoRow(
                            value = if (copied == "username") "Copied" else "@${chat.username}",
                            label = "Username",
                            onClick = { copy("username", "@${chat.username}") },
                        )
                    }
                    if (!hidden && chat.peerBio.isNotEmpty()) InfoRow(chat.peerBio, "Bio")
                    InfoRow(
                        value = if (copied == "id") "Copied" else chat.peerId,
                        label = "ID",
                        onClick = { copy("id", chat.peerId) },
                    )
                }
                InfoCard {
                    ActionRow(
                        icon = R.drawable.ic_mute,
                        label = if (chat.muted) "Unmute" else "Mute",
                        color = Ink,
                        onClick = onToggleMute,
                    )
                    if (chat.status == "active") {
                        ActionRow(
                            icon = R.drawable.ic_restrict,
                            label = "Restrict",
                            color = Ink,
                            onClick = onRestrict,
                        )
                    }
                    if (chat.status == "restricted") {
                        ActionRow(
                            icon = R.drawable.ic_restrict,
                            label = "Unrestrict",
                            color = Ink,
                            onClick = onUnrestrict,
                        )
                    }
                    ActionRow(
                        icon = R.drawable.ic_delete,
                        label = "Delete Chat",
                        color = Danger,
                        onClick = onDelete,
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .pressClickable(onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Color.White)
            }
            Text("User Info", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(6.dp),
    ) {
        content()
    }
}

@Composable
fun InfoRow(
    value: String,
    label: String,
    muted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.pressClickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            color = if (muted) InkMuted else Ink,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(label, color = InkMuted, fontSize = 12.5.sp)
    }
}

@Composable
fun ActionRow(icon: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressClickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(painterResource(icon), null, tint = color)
        Text(label, color = color, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
    }
}
