package com.civenth.app.ui.rooms

import com.civenth.app.ui.theme.pressClickable
import com.civenth.app.data.GroupData
import com.civenth.app.data.MEMBER_LIMIT
import com.civenth.app.data.colorOf
import com.civenth.app.data.rankOf
import com.civenth.app.data.separateRoleOf
import com.civenth.app.ui.theme.roleColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.Chat
import com.civenth.app.data.Presence
import com.civenth.app.data.RoomMember
import com.civenth.app.ui.chats.Avatar
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.Canvas
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.InkMuted
import com.civenth.app.ui.theme.Panel

private sealed interface MemberItem {
    val key: String
}

private data class SectionTitle(val title: String) : MemberItem {
    override val key get() = "title-$title"
}

private data class MemberLine(val member: RoomMember, val muted: Boolean, val color: String?) : MemberItem {
    override val key get() = member.id
}

private data class MoreLine(val text: String) : MemberItem {
    override val key get() = "more"
}

private fun sectionOf(member: RoomMember, presence: Map<String, Presence>, loadedAt: Long): Int {
    if (member.disabled || member.deleted) return 1
    val live = presence[member.id]?.takeIf { it.at > loadedAt }
    val hidden = live?.hidden ?: member.lastSeenHidden
    val online = live?.online ?: member.online
    return if (hidden) 2 else if (online) 0 else 1
}

@Composable
fun MemberListScreen(
    group: Chat,
    data: GroupData?,
    userId: String,
    presence: Map<String, Presence>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val current = data
    val items = if (current == null) emptyList() else buildList<MemberItem> {
        val roles = current.roles
        val rankKey: (RoomMember) -> Int = { if (it.role == "owner") -1 else rankOf(it.roleIds, roles) ?: Int.MAX_VALUE }
        val sorted = current.members.sortedWith(compareBy<RoomMember>(rankKey, { it.name.lowercase() }))
        val separate = linkedMapOf<String, MutableList<RoomMember>>()
        val plain = mutableMapOf<Int, MutableList<RoomMember>>()
        sorted.forEach { member ->
            val section = sectionOf(member, presence, current.loadedAt)
            val role = if (section == 0) separateRoleOf(member.roleIds, roles) else null
            if (role != null) separate.getOrPut(role.id) { mutableListOf() } += member
            else plain.getOrPut(section) { mutableListOf() } += member
        }
        val colorFor: (RoomMember) -> String? = { if (it.disabled || it.deleted) null else colorOf(it.roleIds, roles) }
        roles.filter { separate.containsKey(it.id) }.forEach { role ->
            val list = separate.getValue(role.id)
            add(SectionTitle("${role.name.uppercase()} — ${list.size}"))
            list.forEach { add(MemberLine(it, muted = false, color = colorFor(it))) }
        }
        listOf(0 to "ONLINE", 1 to "OFFLINE", 2 to "MEMBERS").forEach { (index, title) ->
            val list = plain[index].orEmpty()
            if (list.isNotEmpty()) {
                add(SectionTitle("$title — ${list.size}"))
                list.forEach { add(MemberLine(it, muted = index != 0, color = colorFor(it))) }
            }
        }
        if (current.members.size >= MEMBER_LIMIT && group.memberCount > MEMBER_LIMIT) {
            add(MoreLine("Showing $MEMBER_LIMIT of ${group.memberCount} members"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel)
                    .drawBehind { drawLine(BubbleBorder, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .pressClickable(onClick = onBack),
                ) {
                    Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
                }
                Text("Members", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            if (current == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = Chroma)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(items, key = { it.key }) { item ->
                        when (item) {
                            is SectionTitle -> Text(
                                text = item.title,
                                color = InkMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                            )
                            is MemberLine -> Box(Modifier.animateItem()) { MemberListRow(item.member, item.member.id == userId, item.muted, item.color) }
                            is MoreLine -> Text(
                                text = item.text,
                                color = InkMuted,
                                fontSize = 12.5.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberListRow(member: RoomMember, self: Boolean, muted: Boolean, color: String?) {
    val roleName = if (member.role == "owner") "Owner" else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Avatar(member.name, 32.dp, disabled = member.disabled, deleted = member.deleted)
        Text(
            text = if (self) "${member.name} (You)" else member.name,
            color = roleColor(color) ?: if (muted) InkMuted else Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (member.disabled) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (roleName.isNotEmpty()) Text(roleName, color = Chroma, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
