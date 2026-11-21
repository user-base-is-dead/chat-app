package com.mironchat.app.ui.chats

import com.mironchat.app.ui.theme.pressClickable
import com.mironchat.app.ui.theme.BubbleBorder
import androidx.compose.foundation.border
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.R
import com.mironchat.app.data.ApiError
import com.mironchat.app.data.Person
import com.mironchat.app.data.RoomPreview
import com.mironchat.app.data.joinKey
import com.mironchat.app.ui.theme.Card
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.Surface
import kotlinx.coroutines.delay

private const val MIN_QUERY = 2
private const val SEARCH_WAIT = 250L

enum class FindKind(val title: String, val placeholder: String, val hint: String, val empty: String, val roomKind: String?) {
    Users("Search User", "Search by username", "Type a username to find people", "No users found", null),
    Groups("Search Group", "Search groups or paste a link", "Type a group name or @username, or paste an invite link", "No groups found", "group"),
    Channels("Search Channel", "Search channels or paste a link", "Type a channel name or @username, or paste an invite link", "No channels found", "channel"),
}

private data class Found(val query: String, val people: List<Person>, val rooms: List<RoomPreview> = emptyList())

@Composable
fun FindUserScreen(
    kind: FindKind,
    onSearch: suspend (String) -> List<Person>,
    onSearchRooms: suspend (String, String) -> List<RoomPreview>,
    onBack: () -> Unit,
    onStartChat: (Person) -> Unit,
    onOpenRoom: (RoomPreview) -> Unit,
    onOpenKey: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(Found("", emptyList())) }
    val focus = remember { FocusRequester() }
    val needle = query.trim().removePrefix("@")
    val pastedKey = if (kind.roomKind != null) joinKey(query) else null
    val searching = needle.length >= MIN_QUERY && pastedKey == null
    val loading = searching && found.query != needle
    val people = if (kind == FindKind.Users && searching && !loading) found.people else emptyList()
    val rooms = if (kind.roomKind != null && searching && !loading) found.rooms else emptyList()
    val exact = people.filter { it.username.equals(needle, ignoreCase = true) }
    val similar = people - exact.toSet()

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        focus.requestFocus()
    }

    LaunchedEffect(needle) {
        if (!searching) return@LaunchedEffect
        delay(SEARCH_WAIT)
        found = try {
            val roomKind = kind.roomKind
            if (roomKind != null) Found(needle, emptyList(), onSearchRooms(needle, roomKind))
            else Found(needle, onSearch(needle))
        } catch (_: ApiError) {
            Found(needle, emptyList())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
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
            Text(kind.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
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
            Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(18.dp))
            SearchText(
                value = query,
                placeholder = kind.placeholder,
                onChange = { query = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = if (kind == FindKind.Users) KeyboardType.Ascii else KeyboardType.Text,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }
        when {
            pastedKey != null -> Column(modifier = Modifier.padding(top = 8.dp)) {
                LinkRow(pastedKey) { onOpenKey(pastedKey) }
            }
            !searching -> Centered { Text(kind.hint, color = InkMuted, fontSize = 14.sp) }
            loading -> Centered { CircularProgressIndicator(color = Chroma, modifier = Modifier.size(24.dp)) }
            people.isEmpty() && rooms.isEmpty() -> Centered { Text(kind.empty, color = InkMuted, fontSize = 14.sp) }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                if (exact.isNotEmpty()) {
                    item { SectionTitle("Exact match") }
                    items(exact, key = { it.id }) { Box(Modifier.animateItem()) { PersonRow(it, needle, true) { onStartChat(it) } } }
                }
                if (similar.isNotEmpty()) {
                    item { SectionTitle(if (exact.isEmpty()) "Users" else "Similar usernames") }
                    items(similar, key = { it.id }) { Box(Modifier.animateItem()) { PersonRow(it, needle, false) { onStartChat(it) } } }
                }
                items(rooms, key = { it.id }) { RoomResultRow(it, needle) { onOpenRoom(it) } }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = InkMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun LinkRow(key: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Chroma.copy(alpha = 0.1f))
            .pressClickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text("Open invite link", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(key, color = InkMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RoomResultRow(room: RoomPreview, needle: String, onClick: () -> Unit) {
    val members = "${room.memberCount} ${if (room.memberCount == 1) "member" else "members"}"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .pressClickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Avatar(room.title, 52.dp, photo = room.photo)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlighted(room.title, needle),
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = highlighted("@${room.username}, $members", needle),
                color = InkMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PersonRow(person: Person, needle: String, exact: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (exact) Chroma.copy(alpha = 0.1f) else Surface.copy(alpha = 0f))
            .pressClickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Avatar(person.displayName, 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.displayName,
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = highlighted("@${person.username}", needle),
                color = InkMuted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
