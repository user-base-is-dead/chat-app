package com.civenth.app.ui.chat

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.ui.chats.formatDateTime
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Canvas
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.pressClickable

@Composable
fun ChatSearchScreen(
    room: ChatRoom,
    title: String,
    nameOf: (Message) -> String,
    onPick: (Message) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler {
        room.search("")
        onBack()
    }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val needle = query.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .pressClickable {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .pressClickable {
                        room.search("")
                        onBack()
                    },
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Card)
                    .border(1.dp, BubbleBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Search in $title", color = InkMuted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            room.search(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                        cursorBrush = SolidColor(Chroma),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
        }
        val hits = room.searchHits
        if (hits.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = when {
                        needle.isEmpty() -> "Type to search messages in this chat"
                        room.searchBusy -> "Searching…"
                        else -> "No matching messages"
                    },
                    color = InkMuted,
                    fontSize = 15.sp,
                )
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(hits, key = { it.id }) { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressClickable {
                            room.search("")
                            onPick(message)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(nameOf(message), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(remember(message.createdAt) { formatDateTime(message.createdAt) }, color = InkMuted, fontSize = 12.5.sp)
                    }
                    Text(
                        text = highlighted(message.text.replace('\n', ' '), needle),
                        color = InkMuted,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (room.searchBusy) {
                item {
                    Text("Searching…", color = InkMuted, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
                }
            }
        }
    }
}

private fun highlighted(text: String, needle: String) = buildAnnotatedString {
    if (needle.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var from = 0
    while (true) {
        val at = text.indexOf(needle, from, ignoreCase = true)
        if (at < 0) {
            append(text.substring(from))
            break
        }
        append(text.substring(from, at))
        withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.SemiBold)) { append(text.substring(at, at + needle.length)) }
        from = at + needle.length
    }
}
