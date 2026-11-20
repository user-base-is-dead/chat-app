package com.meghnathnews.mironchat

import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.ExperimentalComposeUiApi
import android.view.View
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
import com.meghnathnews.mironchat.alerts.AlertRoute
import com.meghnathnews.mironchat.alerts.AppState
import com.meghnathnews.mironchat.alerts.EXTRA_CHAT_ID
import com.meghnathnews.mironchat.ui.SessionState
import com.meghnathnews.mironchat.ui.SessionViewModel
import com.meghnathnews.mironchat.ui.auth.AuthScreen
import com.meghnathnews.mironchat.ui.main.MainScreen
import com.meghnathnews.mironchat.ui.theme.MironTheme

private object StillMotion : MotionDurationScale {
    override val scaleFactor: Float = 0f
}

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        AppState.visible = true
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
        routeFrom(intent)
        val clearBar = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = clearBar, navigationBarStyle = clearBar)
        val content = findViewById<View>(android.R.id.content)
        content.compositionContext = content.createLifecycleAwareWindowRecomposer(StillMotion, lifecycle)
        setContent {
            MironTheme {
                AppRoot()
            }
        }
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
