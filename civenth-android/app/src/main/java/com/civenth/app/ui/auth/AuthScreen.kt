package com.civenth.app.ui.auth

import com.civenth.app.ui.theme.pressClickable
import com.civenth.app.ui.theme.BubbleBorder
import androidx.compose.foundation.border
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civenth.app.R
import com.civenth.app.data.User
import com.civenth.app.CiventhApp
import com.civenth.app.update.AppUpdate
import com.civenth.app.update.NotificationsPage
import com.civenth.app.update.AppAlerts
import com.civenth.app.ui.theme.SurfaceStrong
import com.civenth.app.ui.theme.Appearance
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Danger
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.Surface
import com.civenth.app.ui.theme.riseIn

private const val MAX_NAME = 64

@Composable
fun AuthScreen(onLogin: (User) -> Unit, model: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        AppUpdate.refresh((context.applicationContext as CiventhApp).http)
        onDispose {}
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AuthForm(onLogin, model)
        AppAlerts(modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 8.dp, end = 16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(SurfaceStrong),
        )
    }
}

@Composable
private fun AuthForm(onLogin: (User) -> Unit, model: AuthViewModel) {
    val registering = model.mode == AuthMode.Register
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        model.submit(onLogin)
    }
    val view = LocalView.current

    DisposableEffect(view) {
        val bars = WindowCompat.getInsetsController((view.context as Activity).window, view)
        bars.isAppearanceLightStatusBars = !Appearance.dark
        onDispose { bars.isAppearanceLightStatusBars = !Appearance.dark }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Brand()
        Column(
            modifier = Modifier
                .riseIn(1)
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Panel)
                .border(1.dp, BubbleBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (registering) "Create a new account" else "Sign in to continue",
                color = InkMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ModeTabs(model.mode, model::switchMode)
            AnimatedVisibility(visible = registering) {
                Field(
                    label = "Name",
                    value = model.displayName,
                    onChange = { model.displayName = it.take(MAX_NAME) },
                )
            }
            Field(
                label = "Username",
                value = model.username,
                onChange = { model.username = it },
                keyboardType = KeyboardType.Ascii,
                contentType = ContentType.Username,
            )
            Field(
                label = "Password",
                value = model.password,
                onChange = { model.password = it },
                secret = true,
                contentType = if (registering) ContentType.NewPassword else ContentType.Password,
                last = !registering,
                onDone = submit,
            )
            AnimatedVisibility(visible = registering) {
                Field(
                    label = "Confirm Password",
                    value = model.confirmPassword,
                    onChange = { model.confirmPassword = it },
                    secret = true,
                    contentType = ContentType.NewPassword,
                    last = true,
                    onDone = submit,
                )
            }
            model.error?.let { Text(it, color = Danger, fontSize = 14.sp) }
            model.notice?.let { Text(it, color = Chroma, fontSize = 14.sp) }
            Button(
                onClick = submit,
                enabled = !model.submitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Chroma,
                    disabledContainerColor = Chroma,
                    disabledContentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (model.submitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = if (registering) "REGISTER" else "LOGIN",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
        Box(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Brand() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceStrong)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Civenth logo",
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = buildAnnotatedString {
                append("Simple chats\nwith your\n")
                withStyle(SpanStyle(color = Chroma)) { append("people.") }
            },
            color = Ink,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ModeTabs(mode: AuthMode, onChange: (AuthMode) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(Surface)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / 2
        val indicatorX by animateDpAsState(
            targetValue = if (mode == AuthMode.Login) 0.dp else tabWidth,
            label = "tab",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Panel)
                .border(1.dp, BubbleBorder, CircleShape),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            AuthMode.entries.forEach { item ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .pressClickable { onChange(item) },
                ) {
                    Text(
                        text = item.name,
                        color = if (item == mode) Ink else InkMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    contentType: ContentType? = null,
    last: Boolean = false,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) KeyboardType.Password else keyboardType,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Chroma,
            focusedLabelColor = Chroma,
            cursorColor = Chroma,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (contentType != null) this.contentType = contentType },
    )
}
