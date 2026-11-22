package com.civenth.app.ui.chats

import com.civenth.app.ui.theme.Appearance

import com.civenth.app.ui.theme.SoftShadow

import androidx.compose.ui.draw.shadow

import androidx.compose.runtime.remember

import androidx.compose.foundation.interaction.MutableInteractionSource


import androidx.compose.animation.core.spring

import androidx.compose.animation.core.tween

import androidx.compose.animation.fadeOut

import androidx.compose.animation.fadeIn

import androidx.compose.animation.slideOutHorizontally

import androidx.compose.animation.slideInHorizontally

import androidx.compose.animation.togetherWith

import androidx.compose.animation.AnimatedContent

import com.civenth.app.ui.theme.pressClickable
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
import com.civenth.app.R
import com.civenth.app.data.Chat
import com.civenth.app.ui.theme.SurfaceStrong
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted

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
                    .pressClickable(onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
            }
            Text("New & Restricted", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        RequestTabs(showRestricted) { showRestricted = it }
        AnimatedContent(
            targetState = showRestricted,
            transitionSpec = {
                val direction = if (targetState) 1 else -1
                (slideInHorizontally(tween(280)) { it / 4 * direction } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(280)) { -it / 4 * direction } + fadeOut(tween(180)))
            },
            label = "requests",
            modifier = Modifier.fillMaxSize(),
        ) { restricted ->
            if (restricted) {
                ChatList(model.restricted, "No restricted chats", bottomSpace, restrictedActions, withDate = true, intro = false, onOpenChat = onOpenChat)
            } else {
                ChatList(model.requests, "No message requests", bottomSpace, withDate = true, intro = false, onOpenChat = onOpenChat)
            }
        }
    }
}

@Composable
private fun RequestTabs(showRestricted: Boolean, onChange: (Boolean) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .height(46.dp)
            .clip(CircleShape)
            .background(if (Appearance.dark) Card else SurfaceStrong)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / 2
        val indicatorX by animateDpAsState(
            targetValue = if (showRestricted) tabWidth else 0.dp,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 520f),
            label = "tab",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(3.dp, CircleShape, ambientColor = SoftShadow, spotColor = SoftShadow)
                .clip(CircleShape)
                .background(if (Appearance.dark) SurfaceStrong else Card),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            listOf(false to "Message Requests", true to "Restricted").forEach { (restricted, label) ->
                val selected = restricted == showRestricted
                val mix by androidx.compose.animation.core.animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "tabText")
                val color = androidx.compose.ui.graphics.lerp(InkMuted, Ink, mix)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onChange(restricted) },
                ) {
                    Text(
                        text = label,
                        color = color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
