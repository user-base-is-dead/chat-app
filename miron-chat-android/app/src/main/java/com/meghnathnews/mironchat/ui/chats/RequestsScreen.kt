package com.meghnathnews.mironchat.ui.chats

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.ui.theme.SurfaceStrong
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted

@Composable
fun RequestsScreen(
    model: ChatsViewModel,
    bottomSpace: PaddingValues,
    restrictedActions: (Chat) -> List<ChatAction>,
    onBack: () -> Unit,
    onOpenChat: (Chat) -> Unit,
) {
    var showRestricted by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
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
            Text("New & Restricted", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        RequestTabs(showRestricted) { showRestricted = it }
        if (showRestricted) {
            ChatList(model.restricted, "No restricted chats", bottomSpace, restrictedActions, onOpenChat)
        } else {
            ChatList(model.requests, "No message requests", bottomSpace, onOpenChat = onOpenChat)
        }
    }
}

@Composable
private fun RequestTabs(showRestricted: Boolean, onChange: (Boolean) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceStrong)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / 2
        val indicatorX by animateDpAsState(if (showRestricted) tabWidth else 0.dp, label = "tab")
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(Card),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            listOf(false to "Message Requests", true to "Restricted").forEach { (restricted, label) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onChange(restricted) },
                ) {
                    Text(
                        text = label,
                        color = if (restricted == showRestricted) Ink else InkMuted,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
