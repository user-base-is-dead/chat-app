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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.ChannelList
import com.civenth.app.data.Chat
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Canvas
import com.civenth.app.ui.theme.Card
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.pressClickable

data class ForwardOptions(
    val chats: List<Chat>,
    val groups: List<Chat>,
    val channels: List<Chat>,
    val channelLists: Map<String, ChannelList>,
    val loadChannels: (String) -> Unit,
)

private sealed interface ForwardRow {
    val key: String

    data class Title(val label: String) : ForwardRow {
        override val key get() = "title-$label"
    }

    data class Target(val chat: Chat, val subtitle: String) : ForwardRow {
        override val key get() = "target-${chat.id}"
    }

    data class Group(val group: Chat, val open: Boolean) : ForwardRow {
        override val key get() = "group-${group.id}"
    }

    data class Channel(val group: Chat, val id: String, val name: String) : ForwardRow {
        override val key get() = "channel-$id"
    }

    data class Note(val parent: String, val text: String) : ForwardRow {
        override val key get() = "note-$parent"
    }
}

@Composable
fun ForwardScreen(options: ForwardOptions, count: Int, onPick: (String, String) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }
    var openGroups by remember { mutableStateOf(emptySet<String>()) }
    val needle = query.trim().removePrefix("@")
    val matches: (Chat) -> Boolean = { it.name.contains(needle, true) || it.username.contains(needle, true) }
    val rows = buildList {
        val chats = options.chats.filter(matches)
        if (chats.isNotEmpty()) {
            add(ForwardRow.Title("Chats"))
            chats.forEach { add(ForwardRow.Target(it, if (it.username.isNotEmpty()) "@${it.username}" else "")) }
        }
        val groups = options.groups.filter(matches)
        if (groups.isNotEmpty()) {
            add(ForwardRow.Title("Groups"))
            groups.forEach { group ->
                val open = group.id in openGroups
                add(ForwardRow.Group(group, open))
                if (!open) return@forEach
                val list = options.channelLists[group.id]
                when {
                    list == null -> add(ForwardRow.Note(group.id, "Loading channels"))
                    else -> {
                        val writable = list.channels.filter { "sendMessages" in it.permissions }
                        if (writable.isEmpty()) add(ForwardRow.Note(group.id, "You cannot send messages here"))
                        writable.forEach { add(ForwardRow.Channel(group, it.id, it.name)) }
                    }
                }
            }
        }
        val channels = options.channels.filter(matches)
        if (channels.isNotEmpty()) {
            add(ForwardRow.Title("Channels"))
            channels.forEach { add(ForwardRow.Target(it, "Channel")) }
        }
    }

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
                    .pressClickable(onClick = onBack),
            ) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
            }
            Text(
                text = if (count == 1) "Forward message" else "Forward $count messages",
                color = Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Card)
                .border(1.dp, BubbleBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
        ) {
            Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text("Search", color = InkMuted, fontSize = 16.sp)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                    cursorBrush = SolidColor(Chroma),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (rows.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("Nothing found", color = InkMuted, fontSize = 15.sp)
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is ForwardRow.Title -> Text(
                        text = row.label,
                        color = InkMuted,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp),
                    )
                    is ForwardRow.Target -> TargetRow(row.chat.name, row.subtitle, row.chat.photo) { onPick(row.chat.id, row.chat.name) }
                    is ForwardRow.Group -> TargetRow(row.group.name, if (row.open) "Pick a channel" else "Group", row.group.photo, chevron = true) {
                        if (row.open) openGroups = openGroups - row.group.id
                        else {
                            openGroups = openGroups + row.group.id
                            if (options.channelLists[row.group.id] == null) options.loadChannels(row.group.id)
                        }
                    }
                    is ForwardRow.Channel -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickable { onPick(row.id, "${row.group.name} #${row.name}") }
                            .padding(start = 76.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_hash), null, tint = InkMuted, modifier = Modifier.size(18.dp))
                        Text(row.name, color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    is ForwardRow.Note -> Text(
                        text = row.text,
                        color = InkMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 76.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetRow(name: String, subtitle: String, photo: String, chevron: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Avatar(name, 46.dp, photo = photo)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = Ink, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) Text(subtitle, color = InkMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (chevron) Icon(painterResource(R.drawable.ic_chevron), null, tint = InkMuted, modifier = Modifier.size(20.dp))
    }
}
