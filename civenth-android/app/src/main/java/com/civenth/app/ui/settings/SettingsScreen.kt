package com.civenth.app.ui.settings

import com.civenth.app.alerts.MessageAlerts
import com.civenth.app.alerts.Push
import androidx.core.app.NotificationManagerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import android.os.Build
import android.provider.Settings
import android.content.Intent
import com.civenth.app.ui.theme.SoftShadow
import androidx.compose.ui.draw.shadow
import com.civenth.app.ui.theme.pressClickable
import androidx.compose.runtime.rememberCoroutineScope
import com.civenth.app.CiventhApp
import kotlinx.coroutines.launch
import com.civenth.app.ui.theme.BubbleBorder
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.alpha
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
import com.civenth.app.R
import com.civenth.app.data.User
import com.civenth.app.ui.chat.Doodle
import com.civenth.app.ui.chat.copyText
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.theme.SurfaceStrong
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.Appearance
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Cover
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.MutedIcon
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.ThemeChoice
import com.civenth.app.ui.theme.appColors
import com.civenth.app.ui.theme.softSpring
import com.civenth.app.ui.theme.SmoothSwitch
import kotlinx.coroutines.delay

private const val COPIED_FOR = 1_500L
private val IconBlue = Color(0xFF1F6FEB)
private val IconPurple = Color(0xFF6D4AFF)
private val IconGrey = Color(0xFF7C7A84)
private val IconOrange = Color(0xFFC2410C)

enum class SettingsView(val depth: Int, val parent: String) {
    Main(0, "Main"),
    Profile(1, "Main"),
    Privacy(1, "Main"),
    Security(1, "Main"),
    Password(2, "Security"),
    Account(2, "Security"),
}

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
    val back = { onViewChange(SettingsView.valueOf(view.parent)) }
    AnimatedContent(
        targetState = view,
        transitionSpec = {
            val forward = targetState.depth > initialState.depth
            val motion = softSpring(IntOffset.VisibilityThreshold)
            (slideInHorizontally(motion) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                (slideOutHorizontally(motion) { if (forward) -it / 3 else it / 3 } + fadeOut())
        },
        label = "settings",
    ) { shown ->
        when (shown) {
            SettingsView.Main -> SettingsMain(user, bottomSpace, onViewChange, onLogout)
            SettingsView.Profile -> ProfileSettings(user, back, onUserChange)
            SettingsView.Privacy -> PrivacySettings(back)
            SettingsView.Security -> SecuritySettings(back, onViewChange)
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
    var confirmLogout by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    if (confirmLogout) LogoutSheet(user, onConfirm = onLogout, onClose = { confirmLogout = false })
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
                hint = "Your username. Tap to copy",
                onClick = { copy("username", "@${user.username}") },
            )
            if (user.bio.isNotEmpty()) {
                SettingsRow(
                    icon = R.drawable.ic_info,
                    iconColor = IconOrange,
                    label = if (copied == "bio") "Copied" else user.bio,
                    hint = "Your bio. Tap to copy",
                    onClick = { copy("bio", user.bio) },
                )
            }
            SettingsRow(
                icon = R.drawable.ic_hash,
                iconColor = IconPurple,
                label = if (copied == "id") "Copied" else user.id,
                hint = "Your account ID. Tap to copy",
                onClick = { copy("id", user.id) },
            )
        }
        Group {
            Text(
                text = "Appearance",
                color = InkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, top = 6.dp),
            )
            Text(
                text = "Pick light or dark, or follow your phone",
                color = InkMuted,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
            )
            ThemePicker()
        }
        Group {
            NotificationsRow()
        }
        Group {
            SettingsRow(R.drawable.ic_eye, IconBlue, "Privacy", chevron = true) {
                onViewChange(SettingsView.Privacy)
            }
            SettingsRow(R.drawable.ic_lock, IconGrey, "Security", chevron = true) {
                onViewChange(SettingsView.Security)
            }
        }
        Group {
            SettingsRow(R.drawable.ic_logout, Danger, "Log Out", hint = "Sign out of Civenth on this phone", labelColor = Danger, onClick = { confirmLogout = true })
        }
    }
}

@Composable
private fun Hero(user: User, onEdit: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .border(1.dp, BubbleBorder, RoundedCornerShape(22.dp))
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
                        .pressClickable(onClick = onEdit)
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
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .border(1.dp, BubbleBorder, RoundedCornerShape(22.dp))
            .padding(8.dp),
    ) {
        content()
    }
}

@Composable
private fun SecuritySettings(onBack: () -> Unit, onViewChange: (SettingsView) -> Unit) {
    FormPage("Security", onBack) {
        Group {
            SettingsRow(R.drawable.ic_lock, IconGrey, "Change Password", hint = "Set a new password for signing in", chevron = true) {
                onViewChange(SettingsView.Password)
            }
            SettingsRow(R.drawable.ic_disabled_user, IconOrange, "Disable or Delete Account", hint = "Pause your account for a while or remove it", chevron = true) {
                onViewChange(SettingsView.Account)
            }
        }
    }
}

@Composable
private fun PrivacySettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = (context.applicationContext as CiventhApp).auth
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        show = runCatching { auth.privacy() }.getOrDefault(true)
    }

    FormPage("Privacy", onBack) {
        Group {
            Text(
                text = "Last seen and online",
                color = InkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 4.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show my last seen and online", color = Ink, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                    Text("When on, people can see when you are online and when you were last active", color = InkMuted, fontSize = 12.5.sp)
                }
                SmoothSwitch(
                    checked = show ?: true,
                    enabled = show != null,
                    onChange = { next ->
                        val before = show
                        show = next
                        scope.launch { show = runCatching { auth.setPrivacy(next) }.getOrDefault(before) }
                    },
                )
            }
        }
    }
}


@Composable
private fun NotificationsRow() {
    val context = LocalContext.current
    val ready = Push.configured
    var on by remember { mutableStateOf(ready && MessageAlerts.enabled(context)) }
    val openSystem = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) openSystem()
    }
    val change: (Boolean) -> Unit = { next ->
        on = next
        MessageAlerts.setEnabled(context, next)
        if (next && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ask.launch(Manifest.permission.POST_NOTIFICATIONS) else openSystem()
        }
    }
    SettingsRow(
        icon = if (on) R.drawable.ic_bell else R.drawable.ic_mute,
        iconColor = IconOrange,
        label = "Notifications",
        hint = if (on) "Get alerts for new messages" else "No message alerts on this phone",
        trailing = { SmoothSwitch(on, enabled = ready, onChange = change) },
        onClick = { if (ready) change(!on) },
    )
}

@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    iconColor: Color,
    label: String,
    hint: String? = null,
    labelColor: Color = Ink,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressClickable(onClick = onClick)
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
        trailing?.invoke()
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
            .height(46.dp)
            .clip(CircleShape)
            .background(SurfaceStrong)
            .padding(4.dp),
    ) {
        val slot = maxWidth / ThemeChoice.entries.size
        val sliderX by animateDpAsState(
            targetValue = slot * ThemeChoice.entries.indexOf(active),
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "theme",
        )
        Box(
            modifier = Modifier
                .offset(x = sliderX)
                .width(slot)
                .fillMaxHeight()
                .shadow(4.dp, CircleShape, ambientColor = SoftShadow, spotColor = SoftShadow)
                .clip(CircleShape)
                .background(Chroma),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            ThemeChoice.entries.forEach { item ->
                val label by animateColorAsState(
                    if (item == active) Color.White else InkMuted,
                    tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "themeLabel",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { Appearance.chooseTheme(context, item) },
                ) {
                    Text(
                        text = item.label,
                        color = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
