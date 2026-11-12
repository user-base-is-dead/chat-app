package com.meghnathnews.mironchat.ui.chats

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
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.DockAccent
import com.meghnathnews.mironchat.ui.theme.DockPill
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.riseIn
import com.meghnathnews.mironchat.ui.theme.softSpring
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
    val searching = model.search.isNotBlank()
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(model.wantSearch) {
        if (!model.wantSearch) return@LaunchedEffect
        model.wantSearch = false
        searchFocus.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle("Chats")
        SearchBox(model.search, searchFocus) { model.search = it }
        if (searching) {
            SearchResults(model, bottomSpace, onOpenChat, onOpenMessage)
            return@Column
        }
        RequestsRow(model.requests.size, onOpenRequests)
        FolderPill(model.unreadChats)
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
fun ScreenTitle(title: String) {
    Text(
        text = title,
        color = Ink,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
    )
}

@Composable
fun ChatList(
    chats: List<Chat>,
    empty: String,
    bottomSpace: PaddingValues,
    actionsFor: ((Chat) -> List<ChatAction>)? = null,
    onOpenChat: (Chat) -> Unit,
) {
    var introDone by remember { mutableStateOf(false) }
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
fun SearchBox(value: String, focus: FocusRequester? = null, onChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .padding(horizontal = 14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = InkMuted,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search", color = InkMuted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Chroma),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
            )
        }
    }
}

@Composable
private fun RequestsRow(count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DockPill),
        ) {
            Icon(painterResource(R.drawable.ic_requests), null, tint = DockAccent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Message Requests", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = when (count) {
                    0 -> "New & Restricted chats"
                    1 -> "1 new request"
                    else -> "$count new requests"
                },
                color = InkMuted,
                fontSize = 13.sp,
            )
        }
        if (count > 0) CountBadge(count)
    }
}

@Composable
private fun FolderPill(unreadChats: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(start = 12.dp, bottom = 8.dp)
            .clip(CircleShape)
            .background(Chroma)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text("General", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (unreadChats > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(formatCount(unreadChats), color = Chroma, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
