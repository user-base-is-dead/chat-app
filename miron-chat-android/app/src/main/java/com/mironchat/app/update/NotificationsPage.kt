package com.mironchat.app.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.BuildConfig
import com.mironchat.app.R
import com.mironchat.app.ui.chat.dayLabel
import com.mironchat.app.ui.chat.sameDay
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Canvas
import com.mironchat.app.ui.theme.Card
import com.mironchat.app.ui.theme.DockAccent
import com.mironchat.app.ui.theme.DockPill
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.SurfaceStrong
import com.mironchat.app.ui.theme.pressClickable
import com.mironchat.app.ui.theme.softSpring
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface AlertRow {
    val key: String
}

private data class AlertDay(val label: String, val at: Long) : AlertRow {
    override val key get() = "day-$at"
}

private data class AlertItem(val alert: AppAlert, val fresh: Boolean) : AlertRow {
    override val key get() = alert.id
}

@Composable
fun NotificationsPage() {
    AnimatedVisibility(
        visible = AppUpdate.pageOpen,
        enter = slideInHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it } + fadeIn(),
        exit = slideOutHorizontally(softSpring(IntOffset.VisibilityThreshold)) { it } + fadeOut(),
    ) {
        val seenBefore = remember { AppUpdate.seenAt }
        LaunchedEffect(Unit) { AppUpdate.markSeen() }
        BackHandler { AppUpdate.pageOpen = false }
        val rows = remember(AppUpdate.alerts) {
            buildList<AlertRow> {
                var previous: Long? = null
                AppUpdate.alerts.forEach { alert ->
                    if (previous == null || !sameDay(previous!!, alert.at)) add(AlertDay(dayLabel(alert.at), alert.at))
                    add(AlertItem(alert, AppUpdate.isNew(alert, seenBefore)))
                    previous = alert.at
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Canvas)
                .pressClickable {}
                .statusBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .pressClickable { AppUpdate.pageOpen = false },
                ) {
                    Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
                }
                Text("Notifications", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            if (rows.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text("No notifications yet", color = InkMuted, fontSize = 15.sp)
                }
                return@Column
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            ) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is AlertDay -> Text(
                            text = row.label,
                            color = InkMuted,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                        )
                        is AlertItem -> AlertCard(row.alert, row.fresh)
                    }
                }
            }
        }
    }
}

private val stampFormat by lazy { SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()) }

@Composable
private fun AlertCard(alert: AppAlert, fresh: Boolean) {
    val newer = alert.kind == AlertKind.Update && alert.versionCode > BuildConfig.VERSION_CODE
    val isLatest = newer && alert.versionCode == AppUpdate.latest?.code
    val installed = alert.kind == AlertKind.Update && alert.versionCode == BuildConfig.VERSION_CODE
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Card)
            .border(1.dp, if (fresh) DockAccent.copy(alpha = 0.5f) else BubbleBorder, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DockPill),
            ) {
                Icon(
                    painter = painterResource(if (alert.kind == AlertKind.Update) R.drawable.ic_arrow_down else R.drawable.ic_bell),
                    contentDescription = null,
                    tint = DockAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = alert.title,
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (fresh) Tag("New", DockAccent)
                    else if (installed) Tag("Installed", InkMuted)
                }
                Text(
                    text = (if (alert.kind == AlertKind.Update) "Update · " else "Notice · ") + remember(alert.at) { stampFormat.format(Date(alert.at)) },
                    color = InkMuted,
                    fontSize = 13.sp,
                )
            }
        }
        Text(alert.body, color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
        if (isLatest) UpdateButton("Update now", Modifier.fillMaxWidth())
    }
}

@Composable
private fun Tag(label: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceStrong)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}
