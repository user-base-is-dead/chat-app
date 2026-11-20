package com.meghnathnews.mironchat.ui.settings

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import com.meghnathnews.mironchat.MironApp
import kotlinx.coroutines.launch
import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.User
import com.meghnathnews.mironchat.ui.chat.Doodle
import com.meghnathnews.mironchat.ui.chat.copyText
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.theme.SurfaceStrong
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.Appearance
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Cover
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedIcon
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.ThemeChoice
import com.meghnathnews.mironchat.ui.theme.appColors
import com.meghnathnews.mironchat.ui.theme.bouncySpring
import com.meghnathnews.mironchat.ui.theme.softSpring
import kotlinx.coroutines.delay

private const val COPIED_FOR = 1_500L
private val IconBlue = Color(0xFF1F6FEB)
private val IconPurple = Color(0xFF6D4AFF)
private val IconGrey = Color(0xFF7C7A84)
private val IconOrange = Color(0xFFC2410C)

enum class SettingsView { Main, Profile, Password, Account }

@Composable
fun SettingsScreen(
    user: User,
    view: SettingsView,
    bottomSpace: PaddingValues,
    onViewChange: (SettingsView) -> Unit,
    onUserChange: (User) -> Unit,
    onAccountGone: () -> Unit,
    onLogout: () -> Unit,
) {
    val back = { onViewChange(SettingsView.Main) }
    AnimatedContent(
        targetState = view,
        transitionSpec = {
            val forward = targetState != SettingsView.Main
            val motion = softSpring(IntOffset.VisibilityThreshold)
            (slideInHorizontally(motion) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                (slideOutHorizontally(motion) { if (forward) -it / 3 else it / 3 } + fadeOut())
        },
        label = "settings",
    ) { shown ->
        when (shown) {
            SettingsView.Main -> SettingsMain(user, bottomSpace, onViewChange, onLogout)
            SettingsView.Profile -> ProfileSettings(user, back, onUserChange)
            SettingsView.Password -> PasswordSettings(back)
            SettingsView.Account -> AccountSettings(back, onAccountGone)
        }
    }
}

@Composable
private fun SettingsMain(
    user: User,
    bottomSpace: PaddingValues,
    onViewChange: (SettingsView) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf<String?>(null) }
    val copy: (String, String) -> Unit = { key, value ->
        copyText(context, value)
        copied = key
    }

    LaunchedEffect(copied) {
        if (copied == null) return@LaunchedEffect
        delay(COPIED_FOR)
        copied = null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 8.dp)
            .padding(bottomSpace),
    ) {
        Hero(user) { onViewChange(SettingsView.Profile) }
        Group {
            SettingsRow(
                icon = R.drawable.ic_at,
                iconColor = IconBlue,
                label = if (copied == "username") "Copied" else "@${user.username}",
                hint = "Username",
                onClick = { copy("username", "@${user.username}") },
            )
            if (user.bio.isNotEmpty()) {
                SettingsRow(
                    icon = R.drawable.ic_info,
                    iconColor = IconOrange,
                    label = if (copied == "bio") "Copied" else user.bio,
                    hint = "Bio",
                    onClick = { copy("bio", user.bio) },
                )
            }
            SettingsRow(
                icon = R.drawable.ic_hash,
                iconColor = IconPurple,
                label = if (copied == "id") "Copied" else user.id,
                hint = "ID",
                onClick = { copy("id", user.id) },
            )
        }
        Group {
            Text(
                text = "Appearance",
                color = InkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 8.dp),
            )
            ThemePicker()
        }
        PrivacyGroup()
        Group {
            SettingsRow(R.drawable.ic_lock, IconGrey, "Change Password", chevron = true) {
                onViewChange(SettingsView.Password)
            }
            SettingsRow(R.drawable.ic_disabled_user, IconOrange, "Disable or Delete Account", chevron = true) {
                onViewChange(SettingsView.Account)
            }
        }
        Group {
            SettingsRow(R.drawable.ic_logout, Danger, "Log Out", labelColor = Danger, onClick = onLogout)
        }
    }
}

@Composable
private fun Hero(user: User, onEdit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, BubbleBorder, RoundedCornerShape(14.dp))
            .padding(bottom = 16.dp),
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(Cover),
            ) {
                Doodle(color = Color.White, tileSize = 260.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(Panel)
                        .border(1.dp, BubbleBorder, CircleShape)
                        .clickable(onClick = onEdit)
                        .padding(start = 10.dp, end = 13.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_edit), null, tint = Ink, modifier = Modifier.size(16.dp))
                    Text("Edit", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 46.dp)
                    .clip(CircleShape)
                    .background(Card)
                    .border(1.dp, BubbleBorder, CircleShape)
                    .padding(5.dp),
            ) {
                Avatar(user.displayName.ifEmpty { user.username }, 92.dp)
            }
        }
        Text(
            text = user.displayName,
            color = Ink,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "online",
            color = Chroma,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, BubbleBorder, RoundedCornerShape(14.dp))
            .padding(6.dp),
    ) {
        content()
    }
}

@Composable
private fun PrivacyGroup() {
    val context = LocalContext.current
    val auth = (context.applicationContext as MironApp).auth
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        show = runCatching { auth.privacy() }.getOrDefault(true)
    }

    Group {
        Text(
            text = "Privacy",
            color = InkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Show my last seen and online", color = Ink, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Switch(
                checked = show ?: true,
                enabled = show != null,
                onCheckedChange = { next ->
                    val before = show
                    show = next
                    scope.launch { show = runCatching { auth.setPrivacy(next) }.getOrDefault(before) }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Chroma, checkedThumbColor = Color.White),
            )
        }
    }
}

@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    iconColor: Color,
    label: String,
    hint: String? = null,
    labelColor: Color = Ink,
    chevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor),
        ) {
            Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = labelColor, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
            if (hint != null) Text(hint, color = InkMuted, fontSize = 12.5.sp)
        }
        if (chevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                tint = MutedIcon,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ThemePicker() {
    val context = LocalContext.current
    val active = Appearance.theme
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceStrong)
            .padding(4.dp),
    ) {
        val slot = maxWidth / ThemeChoice.entries.size
        val sliderX by animateDpAsState(
            targetValue = slot * ThemeChoice.entries.indexOf(active),
            animationSpec = bouncySpring(Dp.VisibilityThreshold),
            label = "theme",
        )
        Box(
            modifier = Modifier
                .offset(x = sliderX)
                .width(slot)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(Chroma),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            ThemeChoice.entries.forEach { item ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { Appearance.chooseTheme(context, item) },
                ) {
                    Text(
                        text = item.label,
                        color = if (item == active) Color.White else InkMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
