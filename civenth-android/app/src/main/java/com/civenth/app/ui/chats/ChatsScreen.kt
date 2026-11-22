package com.civenth.app.ui.chats

import androidx.compose.animation.slideOutVertically

import androidx.compose.animation.slideInVertically

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.spring

import androidx.compose.animation.scaleOut

import androidx.compose.animation.scaleIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.fadeIn

import androidx.compose.animation.togetherWith

import androidx.compose.animation.AnimatedContent

import com.civenth.app.ui.theme.SoftShadow
import androidx.compose.ui.draw.shadow
import com.civenth.app.ui.theme.pressClickable
import com.civenth.app.ui.theme.BubbleBorder
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.TextUnit
import com.civenth.app.update.AppAlerts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.Chat
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.DockAccent
import com.civenth.app.ui.theme.DockPill
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.riseIn
import com.civenth.app.ui.theme.softSpring
import kotlinx.coroutines.delay

private const val INTRO_FOR = 650L
private const val SNIPPET_LEAD = 24

@Composable
fun ChatsScreen(
    model: ChatsViewModel,
    bottomSpace: PaddingValues,
    actionsFor: (Chat) -> List<ChatAction>,
    onOpenRequests: () -> Unit,
    onOpenChat: (Chat) -> Unit,
    onOpenMessage: (Chat) -> Unit,
) {
    LaunchedEffect(model.wantSearch) {
        if (!model.wantSearch) return@LaunchedEffect
        model.wantSearch = false
        model.openSearch()
    }

    AnimatedContent(
        targetState = model.searchOpen,
        transitionSpec = {
            val move = spring(dampingRatio = 0.86f, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)
            val grow = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)
            if (targetState) {
                (slideInVertically(move) { it / 10 } + fadeIn(tween(240, delayMillis = 40)) + scaleIn(grow, initialScale = 0.98f)) togetherWith
                    (slideOutVertically(move) { -it / 14 } + fadeOut(tween(160)) + scaleOut(grow, targetScale = 0.97f))
            } else {
                (slideInVertically(move) { -it / 14 } + fadeIn(tween(240, delayMillis = 40)) + scaleIn(grow, initialScale = 0.97f)) togetherWith
                    (slideOutVertically(move) { it / 10 } + fadeOut(tween(160)) + scaleOut(grow, targetScale = 0.98f))
            }
        },
        label = "chatSearch",
        modifier = Modifier.fillMaxSize(),
    ) { open ->
        if (open) {
            ChatSearchPage(model, onOpenChat, onOpenMessage)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTitle("Chats", model.unreadChats)
                SearchBar("Search your chats", model::openSearch)
                FolderPill(model.unreadChats)
                RequestsRow(model.requests.size, onOpenRequests)
                when {
                    model.loading -> Centered { CircularProgressIndicator(color = Chroma) }
                    model.failed && model.active.isEmpty() -> Centered {
                        Text("Cannot reach the server. Check your connection.", color = InkMuted, fontSize = 14.sp)
                    }
                    else -> ChatList(
                        chats = model.active,
                        empty = "No chats found",
                        bottomSpace = bottomSpace,
                        actionsFor = actionsFor,
                        onOpenChat = onOpenChat,
                    )
                }
            }
        }
    }
}

private fun snippet(text: String, needle: String): String {
    val index = text.indexOf(needle, ignoreCase = true)
    return if (index > SNIPPET_LEAD) "…${text.substring(index - SNIPPET_LEAD + 4)}" else text
}

@Composable
fun SearchResults(
    model: ChatsViewModel,
    bottomSpace: PaddingValues,
    onOpenChat: (Chat) -> Unit,
    onOpenMessage: (Chat) -> Unit,
    kind: String? = null,
) {
    val needle = model.searchNeedle
    val chats = model.chatResults.filter { kind == null || it.kind == kind }
    val messages = model.messageResults.filter { kind == null || it.kind == kind }
    if (chats.isEmpty() && messages.isEmpty()) {
        Centered { Text("No results", color = InkMuted, fontSize = 14.sp) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp), contentPadding = bottomSpace) {
        if (chats.isNotEmpty()) item { SectionTitle("Chats") }
        items(chats, key = { "chat-${it.id}" }) { chat ->
            val byName = chat.name.contains(needle, ignoreCase = true)
            ChatRow(
                chat = chat,
                actionsFor = null,
                needle = needle,
                preview = if (byName) chat.lastText else "@${chat.username}",
                onClick = { onOpenChat(chat) },
            )
        }
        if (messages.isNotEmpty()) item { SectionTitle("Messages") }
        items(messages, key = { "message-${it.id}" }) { chat ->
            ChatRow(
                chat = chat,
                actionsFor = null,
                needle = needle,
                preview = snippet(chat.lastText.orEmpty(), needle),
                onClick = { onOpenMessage(chat) },
            )
        }
    }
}

@Composable
fun ScreenTitle(title: String, count: Int = 0) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 14.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Text(text = title, color = DockAccent, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        if (count > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DockPill)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(formatCount(count), color = DockAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        AppAlerts()
    }
}

@Composable
fun ChatList(
    chats: List<Chat>,
    empty: String,
    bottomSpace: PaddingValues,
    actionsFor: ((Chat) -> List<ChatAction>)? = null,
    withDate: Boolean = false,
    intro: Boolean = true,
    onOpenChat: (Chat) -> Unit,
) {
    var introDone by remember { mutableStateOf(!intro) }
    LaunchedEffect(Unit) {
        delay(INTRO_FOR)
        introDone = true
    }
    if (chats.isEmpty()) {
        Centered { Text(empty, color = InkMuted, fontSize = 14.sp) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = bottomSpace) {
        itemsIndexed(chats, key = { _, chat -> chat.id }) { index, chat ->
            ChatRow(
                chat = chat,
                actionsFor = actionsFor,
                withDate = withDate,
                modifier = Modifier
                    .animateItem(placementSpec = softSpring(IntOffset.VisibilityThreshold))
                    .riseIn(if (introDone) null else index),
                onClick = { onOpenChat(chat) },
            )
        }
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        content()
    }
}

@Composable
fun SearchBox(value: String, placeholder: String, focus: FocusRequester? = null, onChange: (String) -> Unit) {
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
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(18.dp),
        )
        SearchText(
            value = value,
            placeholder = placeholder,
            onChange = onChange,
            modifier = Modifier
                .weight(1f)
                .then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
        )
    }
}

@Composable
private fun RequestsRow(count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(DockPill),
        ) {
            Icon(painterResource(R.drawable.ic_requests), null, tint = DockAccent, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Message Requests", color = Ink, fontSize = 17.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = when (count) {
                    0 -> "New & Restricted chats"
                    1 -> "1 new request"
                    else -> "$count new requests"
                },
                color = InkMuted,
                fontSize = 15.5.sp,
            )
        }
        if (count > 0) CountBadge(count)
    }
}

@Composable
private fun FolderPill(unreadChats: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
            .shadow(6.dp, CircleShape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(CircleShape)
            .background(Card)
            .border(1.dp, BubbleBorder, CircleShape)
            .padding(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(DockPill)
                .padding(horizontal = 18.dp, vertical = 5.dp),
        ) {
            Text("General", color = DockAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (unreadChats > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Chroma)
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                ) {
                    Text(formatCount(unreadChats), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SearchText(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val style = TextStyle(color = Ink, fontSize = fontSize, lineHeight = fontSize * 1.3f)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(Chroma),
        keyboardOptions = keyboardOptions,
        modifier = modifier,
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = style.copy(color = InkMuted),
                        maxLines = 1,
                    )
                }
                field()
            }
        },
    )
}
