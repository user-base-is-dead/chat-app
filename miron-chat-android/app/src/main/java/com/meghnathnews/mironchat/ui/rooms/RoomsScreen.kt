package com.meghnathnews.mironchat.ui.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.roomLabel
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.chats.Centered
import com.meghnathnews.mironchat.ui.chats.ChatAction
import com.meghnathnews.mironchat.ui.chats.ChatRow
import com.meghnathnews.mironchat.ui.chats.ChatsViewModel
import com.meghnathnews.mironchat.ui.chats.JoinState
import com.meghnathnews.mironchat.ui.chats.ScreenTitle
import com.meghnathnews.mironchat.ui.chats.SearchBox
import com.meghnathnews.mironchat.ui.chats.SearchResults
import com.meghnathnews.mironchat.ui.chats.SectionTitle
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted

private fun memberText(count: Int) = "$count ${if (count == 1) "member" else "members"}"

@Composable
fun RoomsScreen(
    title: String,
    kind: String,
    icon: Int,
    emptyText: String,
    rooms: List<Chat>,
    model: ChatsViewModel,
    bottomSpace: PaddingValues,
    actionsFor: (Chat) -> List<ChatAction>,
    onOpenChat: (Chat) -> Unit,
    onOpenMessage: (Chat) -> Unit,
) {
    val invites = rooms.filter { it.status == "request" }
    val joined = rooms.filter { it.status == "active" }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(title)
        SearchBox(model.search) { model.search = it }
        when {
            model.search.isNotBlank() -> SearchResults(model, bottomSpace, onOpenChat, onOpenMessage, kind)
            model.loading -> Centered { CircularProgressIndicator(color = Chroma) }
            rooms.isEmpty() -> Centered {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(icon), null, tint = InkMuted, modifier = Modifier.size(40.dp))
                    Text(emptyText, color = InkMuted, fontSize = 14.sp)
                }
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp), contentPadding = bottomSpace) {
                if (invites.isNotEmpty()) item { SectionTitle("Invites") }
                items(invites, key = { "invite-${it.id}" }) { room ->
                    InviteRow(room, { model.acceptInvite(room) }, { model.declineInvite(room) })
                }
                if (invites.isNotEmpty() && joined.isNotEmpty()) item { SectionTitle("Joined") }
                items(joined, key = { it.id }) { room ->
                    ChatRow(chat = room, actionsFor = actionsFor, onClick = { onOpenChat(room) })
                }
            }
        }
    }
}

@Composable
private fun InviteRow(room: Chat, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Card)
            .padding(10.dp),
    ) {
        Avatar(room.name, 48.dp, photo = room.photo)
        Column(modifier = Modifier.weight(1f)) {
            Text(room.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(memberText(room.memberCount), color = InkMuted, fontSize = 13.sp)
        }
        Text(
            text = "Decline",
            color = InkMuted,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDecline)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )
        Text(
            text = "Accept",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Chroma)
                .clickable(onClick = onAccept)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun JoinPrompt(state: JoinState, onJoin: () -> Unit, onClose: () -> Unit) {
    val room = state.room
    AlertDialog(
        onDismissRequest = onClose,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (room != null) {
                    Avatar(room.title, 88.dp, photo = room.photo)
                    Text(room.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("${roomLabel(room.kind)}, ${memberText(room.memberCount)}", color = InkMuted, fontSize = 14.sp)
                    if (room.about.isNotEmpty()) Text(room.about, color = Ink, fontSize = 14.5.sp, textAlign = TextAlign.Center)
                }
                if (state.error.isNotEmpty()) Text(state.error, color = Danger, fontSize = 13.5.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            if (room != null) {
                TextButton(onClick = onJoin, enabled = !state.busy) { Text("Join", color = Chroma, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Close", color = Ink) }
        },
    )
}
