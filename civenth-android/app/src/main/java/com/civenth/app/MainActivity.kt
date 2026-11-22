package com.civenth.app

import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.civenth.app.alerts.AlertRoute
import com.civenth.app.alerts.AppState
import com.civenth.app.alerts.EXTRA_CHAT_ID
import com.civenth.app.ui.SessionState
import com.civenth.app.ui.SessionViewModel
import com.civenth.app.ui.auth.AuthScreen
import com.civenth.app.ui.main.MainScreen
import com.civenth.app.ui.theme.DragOnlyOverscroll
import com.civenth.app.ui.theme.CiventhTheme
import androidx.lifecycle.lifecycleScope
import com.civenth.app.update.AppUpdate
import com.civenth.app.update.UpdateRequiredScreen
import com.civenth.app.update.NotificationsPage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val launchCount = androidx.compose.runtime.mutableIntStateOf(0)

    override fun onStart() {
        super.onStart()
        launchCount.intValue++
        AppState.visible = true
        lifecycleScope.launch { AppUpdate.check((application as CiventhApp).http) }
    }

    override fun onStop() {
        AppState.visible = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFrom(intent)
    }

    private fun routeFrom(intent: Intent?) {
        intent?.getStringExtra(EXTRA_CHAT_ID)?.let { AlertRoute.chatId = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUpdate.attach(this)
        routeFrom(intent)
        val clearBar = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = clearBar, navigationBarStyle = clearBar)
        setContent {
            CiventhTheme {
                DragOnlyOverscroll {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (AppUpdate.required) {
                            UpdateRequiredScreen()
                        } else {
                            AppRoot()
                            NotificationsPage()
                            com.civenth.app.ui.main.MenuScreen()
                        }
                        androidx.compose.runtime.key(launchCount.intValue) { LaunchLogo() }
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchLogo() {
    val fade = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { fade.animateTo(0f, androidx.compose.animation.core.tween(340, delayMillis = 100)) }
    if (fade.value > 0f) Box(
        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher_foreground), null,
            Modifier.size(96.dp).graphicsLayer { scaleX = 1f + (1f - fade.value) * 0.08f; scaleY = scaleX })
    }
}

@Composable
private fun AppRoot(session: SessionViewModel = viewModel()) {
    val state by session.state.collectAsStateWithLifecycle()

    Crossfade(targetState = state::class, label = "session") { screen ->
        val current = state
        when {
        screen == SessionState.Loading::class -> Centered { CircularProgressIndicator() }
        screen == SessionState.LoggedOut::class -> AuthScreen(onLogin = session::loggedIn)
        screen == SessionState.Offline::class -> Centered {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Cannot reach the server. Check your connection.")
                Button(onClick = session::restore) { Text("Retry") }
            }
        }
        current is SessionState.LoggedIn -> MainScreen(current.user, session::restore, session::loggedIn, session::accountGone, session::logout)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}
