package com.mironchat.app.update

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.offset

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.MironApp
import com.mironchat.app.R
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Canvas
import com.mironchat.app.ui.theme.Card
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.Danger
import com.mironchat.app.ui.theme.DockPill
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.SurfaceStrong
import com.mironchat.app.ui.theme.pressClickable
import kotlinx.coroutines.launch

@Composable
internal fun UpdateButton(label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val http = (context.applicationContext as MironApp).http
    val busy = AppUpdate.download == DownloadState.Downloading
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(if (busy) SurfaceStrong else Chroma)
            .pressClickable(enabled = !busy) { scope.launch { AppUpdate.start(context, http) } }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = when (AppUpdate.download) {
                DownloadState.Downloading -> "Downloading ${(AppUpdate.progress * 100).toInt()}%"
                DownloadState.Ready -> "Install"
                DownloadState.Failed -> "Try again"
                DownloadState.Idle -> label
            },
            color = if (busy) InkMuted else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun UpdateRequiredScreen() {
    BackHandler {}
    val release = AppUpdate.latest
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .pressClickable {}
            .padding(28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Miron Chat",
                modifier = Modifier.size(88.dp),
            )
            Text("Update required", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "This version of Miron Chat is no longer supported. Update to keep chatting.",
                color = InkMuted,
                fontSize = 15.5.sp,
                textAlign = TextAlign.Center,
            )
            if (release != null && release.notes.isNotEmpty()) {
                Text(
                    text = release.notes,
                    color = Ink,
                    fontSize = 14.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Card)
                        .border(1.dp, BubbleBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                )
            }
            if (AppUpdate.download == DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { AppUpdate.progress },
                    color = Chroma,
                    trackColor = SurfaceStrong,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                )
            }
            if (AppUpdate.download == DownloadState.Failed) {
                Text("Download failed. Check your connection.", color = Danger, fontSize = 13.5.sp)
            }
            UpdateButton(if (release == null) "Get the latest version" else "Update to ${release.name}", Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AppAlerts(modifier: Modifier = Modifier) {
    val update = AppUpdate.available && AppUpdate.latest != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        AnimatedVisibility(visible = update, enter = fadeIn(), exit = fadeOut()) {
            UpdateButton("Update")
        }
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Card)
                    .border(1.dp, BubbleBorder, CircleShape)
                    .pressClickable { AppUpdate.pageOpen = true },
            ) {
                Icon(painterResource(R.drawable.ic_bell), "Notifications", tint = Ink, modifier = Modifier.size(22.dp))
            }
            if (AppUpdate.unread > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .heightIn(min = 22.dp)
                        .widthIn(min = 22.dp)
                        .border(2.dp, Card, CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Danger)
                        .padding(horizontal = 5.dp),
                ) {
                    Text(
                        text = if (AppUpdate.unread > 9) "9+" else AppUpdate.unread.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
