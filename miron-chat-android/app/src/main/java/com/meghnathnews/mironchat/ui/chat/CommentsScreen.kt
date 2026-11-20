package com.meghnathnews.mironchat.ui.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.COMMENT_MAX_LENGTH
import com.meghnathnews.mironchat.data.COMMENT_PAGE_SIZE
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.Comment
import com.meghnathnews.mironchat.data.PostStats
import com.meghnathnews.mironchat.data.toComment
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.theme.BubbleBorder
import com.meghnathnews.mironchat.ui.theme.BubbleMuted
import com.meghnathnews.mironchat.ui.theme.BubbleOther
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.DoodleInk
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.Stage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val LOAD_EDGE = 3
private val commentTime = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentsScreen(
    room: ChatRoom,
    chat: Chat,
    post: Message,
    stats: PostStats?,
    userId: String,
    canModerate: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val inputFocus = remember { FocusRequester() }
    var comments by remember(post.id) { mutableStateOf<List<Comment>?>(null) }
    var hasMore by remember(post.id) { mutableStateOf(false) }
    var loadingOlder by remember(post.id) { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Comment?>(null) }
    var deleting by remember { mutableStateOf<Comment?>(null) }
    val warn: (ApiError) -> Unit = { error -> Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show() }

    BackHandler(onBack = onBack)

    LaunchedEffect(post.id) {
        try {
            val list = room.posts.comments(room.chatId, post.id)
            comments = list
            hasMore = list.size == COMMENT_PAGE_SIZE
        } catch (error: ApiError) {
            warn(error)
            comments = emptyList()
        }
    }

    LaunchedEffect(room.commentEvent) {
        val event = room.commentEvent ?: return@LaunchedEffect
        val data = event.data ?: return@LaunchedEffect
        val current = comments ?: return@LaunchedEffect
        if (data.optString("messageId") != post.id) return@LaunchedEffect
        if (event.name == "post.comment") {
            val added = data.optJSONObject("comment")?.toComment() ?: return@LaunchedEffect
            if (current.none { it.id == added.id }) comments = current + added
        } else {
            val gone = data.optString("commentId")
            comments = current.filterNot { it.id == gone }
        }
    }

    LaunchedEffect(comments?.lastOrNull()?.id) {
        if (comments != null && listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
    }

    LaunchedEffect(post.id) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            val current = comments ?: return@collect
            if (!hasMore || loadingOlder || current.isEmpty() || last < current.size - LOAD_EDGE) return@collect
            loadingOlder = true
            try {
                val older = room.posts.comments(room.chatId, post.id, current.first().id)
                val known = (comments ?: emptyList()).map { it.id }.toSet()
                comments = older.filterNot { it.id in known } + (comments ?: emptyList())
                hasMore = older.size == COMMENT_PAGE_SIZE
            } catch (error: ApiError) {
                warn(error)
            }
            loadingOlder = false
        }
    }

    val send: (String) -> Unit = { text ->
        val body = text.trim()
        if (body.codePointCount(0, body.length) > COMMENT_MAX_LENGTH) {
            Toast.makeText(context, "A comment can be up to 2000 characters", Toast.LENGTH_SHORT).show()
        } else if (body.isNotEmpty()) {
            scope.launch {
                try {
                    val added = room.posts.addComment(room.chatId, post.id, body)
                    val current = comments ?: emptyList()
                    if (current.none { it.id == added.id }) comments = current + added
                    listState.scrollToItem(0)
                } catch (error: ApiError) {
                    warn(error)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Stage)) {
        Doodle(DoodleInk, tileSize = 520.dp, solid = true)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Panel)
                    .padding(horizontal = 4.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
                ) {
                    Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
                }
                Text("Comments", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BubbleBorder))
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(comments?.asReversed() ?: emptyList(), key = { it.id }) { comment ->
                        val name = if (comment.userDeleted) "Deleted Account" else comment.userName
                        Box {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BubbleOther)
                                    .combinedClickable(onClick = {}, onLongClick = { menuFor = comment })
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Avatar(name, 36.dp, deleted = comment.userDeleted)
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = name,
                                            color = Chroma,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        Text(
                                            text = remember(comment.createdAt) { commentTime.format(Date(comment.createdAt)) },
                                            color = BubbleMuted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                    Text(comment.text, color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
                                }
                            }
                            if (menuFor?.id == comment.id) DropdownMenu(
                                expanded = true,
                                onDismissRequest = { menuFor = null },
                                shape = RoundedCornerShape(18.dp),
                                containerColor = Panel,
                            ) {
                                MenuItem("Copy") {
                                    menuFor = null
                                    copyText(context, comment.text)
                                }
                                if (canModerate || comment.userId == userId) {
                                    MenuItem("Delete", danger = true) {
                                        menuFor = null
                                        deleting = comment
                                    }
                                }
                            }
                        }
                    }
                    if (comments?.isEmpty() == true) {
                        item(key = "empty") {
                            Text(
                                text = "No comments yet",
                                color = InkMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            )
                        }
                    }
                    item(key = "post") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(BubbleOther)
                                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            Text(chat.name, color = Chroma, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(post.text, color = Ink, fontSize = 15.sp, lineHeight = 21.sp)
                            PostFooter(stats, post.createdAt, { emoji -> room.react(post.id, emoji) }, null)
                        }
                    }
                }
                if (comments == null) CircularProgressIndicator(color = Chroma, modifier = Modifier.align(Alignment.Center))
            }
            InputBar(
                focus = inputFocus,
                onTyping = {},
                onSend = send,
                onAttach = { Toast.makeText(context, "Attachments coming soon", Toast.LENGTH_SHORT).show() },
            )
        }
    }

    deleting?.let { comment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete comment?") },
            text = { Text("This comment will be deleted for everyone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        try {
                            room.posts.deleteComment(room.chatId, post.id, comment.id)
                            comments = comments?.filterNot { it.id == comment.id }
                        } catch (error: ApiError) {
                            warn(error)
                        }
                    }
                }) {
                    Text("Yes", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("No", color = Ink) }
            },
        )
    }
}
