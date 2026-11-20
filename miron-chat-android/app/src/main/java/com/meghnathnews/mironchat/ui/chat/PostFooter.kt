package com.meghnathnews.mironchat.ui.chat

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.PostStats
import com.meghnathnews.mironchat.data.PostSwitches
import com.meghnathnews.mironchat.ui.chats.formatCount
import com.meghnathnews.mironchat.ui.chats.formatTime
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import com.meghnathnews.mironchat.ui.theme.BubbleMuted
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.SurfaceStrong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostFooter(
    stats: PostStats?,
    createdAt: Long,
    onReact: (String) -> Unit,
    onOpenComments: (() -> Unit)?,
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
                            .clickable { onReact(reaction.emoji) }
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
                Text(remember(createdAt) { formatTime(createdAt) }, color = BubbleMuted, fontSize = 11.sp)
            }
        }
        if (stats?.allowComments == true && onOpenComments != null) {
            Box(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(BubbleBorder))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onOpenComments),
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
fun PostToggles(value: PostSwitches, onChange: (PostSwitches) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
    ) {
        TogglePill("Reactions", value.allowReactions) { onChange(value.copy(allowReactions = !value.allowReactions)) }
        TogglePill("Comments", value.allowComments) { onChange(value.copy(allowComments = !value.allowComments)) }
    }
}

@Composable
private fun TogglePill(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (on) Chroma else Panel)
            .border(1.dp, if (on) Chroma else BorderStrong, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = "$label ${if (on) "on" else "off"}",
            color = if (on) Color.White else InkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
