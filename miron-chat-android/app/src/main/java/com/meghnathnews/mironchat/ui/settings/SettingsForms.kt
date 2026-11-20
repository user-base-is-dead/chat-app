package com.meghnathnews.mironchat.ui.settings

import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.MironApp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.User
import com.meghnathnews.mironchat.ui.chat.copyText
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BIO_LIMIT = 140
private const val NAME_LIMIT = 64
private const val USERNAME_LIMIT = 32
private const val PASSWORD_LIMIT = 128
private const val MIN_PASSWORD = 6
private const val COPIED_FOR = 1_500L
private val usernamePattern = Regex("^[A-Za-z0-9_]{3,32}$")

private enum class AccountChoice(val title: String, val text: String, val confirm: String) {
    Disable(
        "Disable account",
        "Your account is hidden from search and you are logged out on every device. " +
            "Log in again any time to turn it back on.",
        "Disable account",
    ),
    Delete(
        "Delete account",
        "Your account and profile are deleted forever and you are logged out on every device. " +
            "People you talked to keep their chats, shown as Deleted Account. This cannot be undone.",
        "Delete account forever",
    ),
}

private fun String.limitedTo(limit: Int): String {
    val count = codePointCount(0, length)
    return if (count <= limit) this else substring(0, offsetByCodePoints(0, limit))
}

@Composable
private fun FormPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
            }
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    secret: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
            autoCorrectEnabled = false,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedBorderColor = Chroma,
            focusedLabelColor = Chroma,
            cursorColor = Chroma,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FormButton(
    label: String,
    busy: Boolean,
    enabled: Boolean = true,
    color: Color = Chroma,
    onClick: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Button(
        onClick = {
            keyboard?.hide()
            onClick()
        },
        enabled = enabled && !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = if (busy) color else MutedBadge,
            disabledContentColor = if (busy) Color.White else InkMuted,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileSettings(user: User, onBack: () -> Unit, onSaved: (User) -> Unit) {
    val context = LocalContext.current
    val auth = (context.applicationContext as MironApp).auth
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(user.displayName) }
    var username by remember { mutableStateOf(user.username) }
    var bio by remember { mutableStateOf(user.bio) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val changed = displayName.trim() != user.displayName || username.trim() != user.username || bio.trim() != user.bio

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(COPIED_FOR)
        copied = false
    }

    FormPage("Profile", onBack) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Avatar(displayName.trim().ifEmpty { user.username }, 88.dp)
            Text("@${user.username}", color = InkMuted, fontSize = 14.sp)
            Text(
                text = if (copied) "Copied" else "ID: ${user.id}",
                color = InkMuted,
                fontSize = 12.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Card)
                    .border(1.dp, BubbleBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        copyText(context, user.id)
                        copied = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        FormField("Name", displayName, {
            displayName = it.limitedTo(NAME_LIMIT)
            saved = false
        })
        FormField("Username", username, {
            username = it.limitedTo(USERNAME_LIMIT)
            saved = false
        })
        FormField("Bio", bio, {
            bio = it.limitedTo(BIO_LIMIT)
            saved = false
        }, singleLine = false)
        Text(
            text = "${bio.codePointCount(0, bio.length)}/$BIO_LIMIT",
            color = InkMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = Danger, fontSize = 14.sp) }
        if (saved && !changed) Text("Profile saved", color = Chroma, fontSize = 14.sp)
        FormButton("Save", busy = saving, enabled = changed) {
            when {
                displayName.isBlank() -> error = "Name cannot be empty"
                !usernamePattern.matches(username.trim()) ->
                    error = "Username must be 3-32 characters: letters, numbers or _"
                else -> {
                    error = null
                    saving = true
                    scope.launch {
                        try {
                            val updated = auth.updateProfile(displayName.trim(), username.trim(), bio.trim())
                            onSaved(updated)
                            displayName = updated.displayName
                            username = updated.username
                            bio = updated.bio
                            saved = true
                        } catch (failure: ApiError) {
                            error = failure.message
                        } finally {
                            saving = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = (context.applicationContext as MironApp).auth
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    FormPage("Change password", onBack) {
        FormField("Current password", current, {
            current = it.take(PASSWORD_LIMIT)
            saved = false
        }, secret = true)
        FormField("New password", next, {
            next = it.take(PASSWORD_LIMIT)
            saved = false
        }, secret = true)
        FormField("Confirm new password", confirm, {
            confirm = it.take(PASSWORD_LIMIT)
            saved = false
        }, secret = true)
        error?.let { Text(it, color = Danger, fontSize = 14.sp) }
        if (saved) Text("Password changed. Other devices are logged out.", color = Chroma, fontSize = 14.sp)
        FormButton("Change password", busy = saving) {
            when {
                current.isEmpty() || next.isEmpty() -> error = "Fill in all fields"
                next.length < MIN_PASSWORD -> error = "New password must be at least 6 characters"
                next != confirm -> error = "Passwords do not match"
                else -> {
                    error = null
                    saving = true
                    scope.launch {
                        try {
                            auth.changePassword(current, next)
                            current = ""
                            next = ""
                            confirm = ""
                            saved = true
                        } catch (failure: ApiError) {
                            error = failure.message
                        } finally {
                            saving = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettings(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val auth = (context.applicationContext as MironApp).auth
    val scope = rememberCoroutineScope()
    var choice by remember { mutableStateOf<AccountChoice?>(null) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val choose: (AccountChoice?) -> Unit = {
        choice = it
        password = ""
        error = null
    }

    FormPage("Disable or delete account", onBack) {
        AccountChoice.entries.forEach { item ->
            val selected = item == choice
            val shape = RoundedCornerShape(20.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Card)
                    .then(if (selected) Modifier.border(2.dp, Chroma, shape) else Modifier)
                    .clickable { choose(item) }
                    .padding(14.dp),
            ) {
                Text(
                    text = item.title,
                    color = if (item == AccountChoice.Delete) Danger else Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(item.text, color = InkMuted, fontSize = 13.5.sp, lineHeight = 19.sp)
            }
        }
        choice?.let { active ->
            FormField("Password", password, { password = it }, secret = true)
            error?.let { Text(it, color = Danger, fontSize = 14.sp) }
            FormButton(active.confirm, busy = busy, color = Danger) {
                if (password.isEmpty()) {
                    error = "Enter your password to continue"
                    return@FormButton
                }
                error = null
                busy = true
                scope.launch {
                    try {
                        if (active == AccountChoice.Delete) auth.deleteAccount(password) else auth.disableAccount(password)
                        onDone()
                    } catch (failure: ApiError) {
                        error = failure.message
                        busy = false
                    }
                }
            }
            Text(
                text = "Cancel",
                color = InkMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { choose(null) }
                    .padding(12.dp),
            )
        }
    }
}
