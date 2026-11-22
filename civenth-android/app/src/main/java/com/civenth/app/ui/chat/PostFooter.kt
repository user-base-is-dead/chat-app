package com.civenth.app.ui.chat

import com.civenth.app.ui.theme.pressClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.data.PostStats
import com.civenth.app.ui.chats.formatCount
import com.civenth.app.ui.chats.formatTime
import com.civenth.app.ui.chats.formatDateTime
import com.civenth.app.ui.theme.BubbleBorder
import com.civenth.app.ui.theme.BubbleMuted
import com.civenth.app.ui.theme.Chroma
import com.civenth.app.ui.theme.Ink
import com.civenth.app.ui.theme.Panel
import com.civenth.app.ui.theme.SurfaceStrong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostFooter(
    stats: PostStats?,
    createdAt: Long,
    onReact: (String) -> Unit,
    onOpenComments: (() -> Unit)?,
    onEditPost: (() -> Unit)? = null,
    onToggleSwitch: (Boolean) -> Unit = {},
) {
    val reactions = if (stats?.allowReactions == true) stats.reactions else emptyList()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                reactions.forEach { reaction ->
                    val mine = stats?.myReaction == reaction.emoji
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(if (mine) Chroma else SurfaceStrong)
                            .border(1.dp, if (mine) Chroma else BubbleBorder, CircleShape)
                            .pressClickable { onReact(reaction.emoji) }
                            .padding(start = 8.dp, end = 11.dp),
                    ) {
                        Text(reaction.emoji, fontSize = 15.sp)
                        Text(
                            text = formatCount(reaction.count),
                            color = if (mine) Color.White else Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp),
            ) {
                if (stats != null) {
                    Icon(painterResource(R.drawable.ic_eye), "Views", tint = BubbleMuted, modifier = Modifier.size(15.dp))
                    Text(formatCount(stats.views), color = BubbleMuted, fontSize = 11.sp)
                    Box(modifier = Modifier.size(4.dp))
                }
                Text(remember(createdAt) { formatDateTime(createdAt) }, color = BubbleMuted, fontSize = 11.sp)
            }
        }
        if (stats != null && onEditPost != null) {
            PostSettings(stats, onEditPost, onToggleSwitch)
        }
        if (stats?.allowComments == true && onOpenComments != null) {
            Box(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(BubbleBorder))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .pressClickable(onClick = onOpenComments),
            ) {
                Icon(painterResource(R.drawable.ic_comment), null, tint = Chroma, modifier = Modifier.size(18.dp))
                Text(
                    text = when (stats.comments) {
                        0 -> "Leave a comment"
                        1 -> "1 comment"
                        else -> "${formatCount(stats.comments)} comments"
                    },
                    color = Chroma,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.ic_chevron), null, tint = Chroma, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PostSettings(stats: PostStats, onEditPost: () -> Unit, onToggleSwitch: (Boolean) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        SettingLabel("Comments", stats.allowComments)
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(BubbleMuted))
        SettingLabel("Reactions", stats.allowReactions)
        Box(modifier = Modifier.weight(1f))
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = true },
            ) {
                Icon(painterResource(R.drawable.ic_more), "Post options", tint = BubbleMuted, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = Panel,
            ) {
                MenuItem("Edit post") {
                    menuOpen = false
                    onEditPost()
                }
                MenuItem(if (stats.allowComments) "Turn off comments" else "Turn on comments") {
                    menuOpen = false
                    onToggleSwitch(false)
                }
                MenuItem(if (stats.allowReactions) "Turn off reactions" else "Turn on reactions") {
                    menuOpen = false
                    onToggleSwitch(true)
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(label: String, on: Boolean) {
    Text(
        text = "$label ${if (on) "on" else "off"}",
        color = if (on) Chroma else BubbleMuted,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
