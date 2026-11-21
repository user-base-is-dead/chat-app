package com.mironchat.app.ui.chats

import com.mironchat.app.ui.theme.Danger

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.material3.TextButton

import androidx.compose.material3.AlertDialog

import com.mironchat.app.ui.theme.riseIn

import androidx.compose.ui.graphics.TransformOrigin

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.slideOutVertically

import androidx.compose.animation.slideInVertically

import androidx.compose.animation.togetherWith

import androidx.compose.animation.AnimatedContent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.R
import com.mironchat.app.data.Chat
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Card
import com.mironchat.app.ui.theme.DockAccent
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.pressClickable
import com.mironchat.app.ui.theme.softSpring

@Composable
fun SearchBar(placeholder: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .clip(CircleShape)
            .background(Card)
            .border(1.dp, BubbleBorder, CircleShape)
            .pressClickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(18.dp))
        Text(placeholder, color = InkMuted, fontSize = 15.sp)
    }
}

@Composable
fun ChatSearchPage(
    model: ChatsViewModel,
    onOpenChat: (Chat) -> Unit,
    onOpenMessage: (Chat) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        enter.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 380f))
    }
    BackHandler(onBack = model::closeSearch)
    val open: (Chat) -> Unit = { chat ->
        model.remember(chat)
        onOpenChat(chat)
    }
    val openMessage: (Chat) -> Unit = { chat ->
        model.remember(chat)
        onOpenMessage(chat)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = enter.value.coerceIn(0f, 1f)
                        translationX = (1f - enter.value) * -18.dp.toPx()
                        rotationZ = (1f - enter.value) * 45f
                    }
                    .size(44.dp)
                    .clip(CircleShape)
                    .pressClickable(onClick = model::closeSearch),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        scaleX = 0.9f + 0.1f * enter.value
                    }
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Card)
                    .border(1.dp, BubbleBorder, CircleShape)
                    .padding(start = 16.dp, end = 6.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(18.dp))
                SearchText(
                    value = model.search,
                    placeholder = "Search people and messages",
                    onChange = { model.search = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                )
                AnimatedVisibility(
                    visible = model.search.isNotEmpty(),
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .pressClickable { model.search = "" },
                    ) {
                        Icon(painterResource(R.drawable.ic_close), "Clear", tint = InkMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        AnimatedContent(
            targetState = model.search.isNotBlank(),
            transitionSpec = {
                val move = spring(dampingRatio = 0.9f, stiffness = 500f, visibilityThreshold = IntOffset.VisibilityThreshold)
                (slideInVertically(move) { it / 16 } + fadeIn(tween(200, delayMillis = 30))) togetherWith
                    (slideOutVertically(move) { -it / 24 } + fadeOut(tween(120)))
            },
            label = "searchBody",
            modifier = Modifier.fillMaxSize(),
        ) { typing ->
            if (typing) SearchResults(model, PaddingValues(bottom = 24.dp), open, openMessage) else Recents(model, open)
        }
    }
}

@Composable
private fun Recents(model: ChatsViewModel, onOpen: (Chat) -> Unit) {
    val recents = model.recentChats
    var askClear by remember { mutableStateOf(false) }
    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text("Clear recent searches?") },
            text = { Text("All recent searches will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    model.forgetAll()
                }) { Text("Clear all", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { askClear = false }) { Text("Cancel", color = Ink) }
            },
        )
    }
    if (recents.isEmpty()) {
        Centered { Text("Search for people, groups and messages", color = InkMuted, fontSize = 14.sp) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.riseIn(0).fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 6.dp, bottom = 2.dp),
            ) {
                Text("Recent", color = InkMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    text = "Clear all",
                    color = DockAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .pressClickable { askClear = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        itemsIndexed(recents, key = { _, chat -> chat.id }) { index, chat ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .animateItem(placementSpec = softSpring(IntOffset.VisibilityThreshold))
                    .riseIn(index + 1)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .pressClickable { onOpen(chat) }
                    .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Avatar(chat.name, 48.dp, photo = chat.photo)
                Column(modifier = Modifier.weight(1f)) {
                    Text(chat.name, color = Ink, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (chat.username.isNotEmpty()) {
                        Text("@${chat.username}", color = InkMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .pressClickable { model.forget(chat) },
                ) {
                    Icon(painterResource(R.drawable.ic_close), "Remove ${chat.name}", tint = InkMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
