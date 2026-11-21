package com.mironchat.app.ui.chat

import com.mironchat.app.ui.chats.SearchText

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mironchat.app.R
import com.mironchat.app.ui.theme.BubbleBorder
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.Panel
import com.mironchat.app.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

private const val PREFS = "emoji"
private const val RECENT = "recent"
private const val RECENT_GROUP = "recent"
private const val RECENT_LIMIT = 24

private val defaultRecent = listOf(
    "😂", "❤️", "👍", "😭", "😊", "🙏", "😍", "🔥", "😁", "🥰", "😅", "👏", "🤔", "😢", "😎", "🎉",
)

private val groupIcons = listOf(
    "smileys_emotion" to "😀",
    "people_body" to "👋",
    "animals_nature" to "🐻",
    "food_drink" to "🍔",
    "travel_places" to "✈️",
    "activities" to "⚽",
    "objects" to "💡",
    "symbols" to "❤️",
    "flags" to "🏳️",
)

private data class EmojiItem(val emoji: String, val name: String)

private data class EmojiGroup(val slug: String, val name: String, val items: List<EmojiItem>)

private var cachedGroups: List<EmojiGroup>? = null

private suspend fun loadGroups(context: Context): List<EmojiGroup> {
    cachedGroups?.let { return it }
    val groups = withContext(Dispatchers.IO) {
        val list = JSONArray(context.assets.open("emoji.json").bufferedReader().use { it.readText() })
        List(list.length()) { index ->
            val group = list.getJSONObject(index)
            val emojis = group.getJSONArray("emojis")
            EmojiGroup(
                slug = group.getString("slug"),
                name = group.getString("name"),
                items = List(emojis.length()) { at ->
                    val item = emojis.getJSONArray(at)
                    EmojiItem(item.getString(0), item.getString(1))
                },
            )
        }
    }
    cachedGroups = groups
    return groups
}

private fun readRecent(context: Context): List<String> {
    val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(RECENT, null)
    return saved?.split(" ")?.filter { it.isNotEmpty() }?.ifEmpty { null } ?: defaultRecent
}

private fun saveRecent(context: Context, list: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(RECENT, list.joinToString(" ")).apply()
}

@Composable
fun EmojiPanel(
    onPick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSearchFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val grid = rememberLazyGridState()
    var groups by remember { mutableStateOf(cachedGroups) }
    var recent by remember { mutableStateOf(readRecent(context)) }
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val shape = RoundedCornerShape(24.dp)
    val starts = remember(groups, recent.size) {
        buildList {
            add(RECENT_GROUP to 0)
            var at = recent.size + 1
            groups?.forEach { group ->
                add(group.slug to at)
                at += group.items.size + 1
            }
        }
    }
    val activeGroup by remember(starts) {
        derivedStateOf { starts.lastOrNull { it.second <= grid.firstVisibleItemIndex }?.first ?: RECENT_GROUP }
    }
    val results = remember(needle, groups) {
        if (needle.isEmpty()) null else groups.orEmpty().flatMap { it.items }.filter { needle in it.name }
    }

    LaunchedEffect(Unit) {
        if (groups == null) groups = loadGroups(context)
    }

    val pick: (String) -> Unit = { emoji ->
        onPick(emoji)
        val next = (listOf(emoji) + recent.filterNot { it == emoji }).take(RECENT_LIMIT)
        recent = next
        saveRecent(context, next)
    }
    val goTo: (String) -> Unit = { slug ->
        query = ""
        scope.launch {
            grid.scrollToItem(starts.firstOrNull { it.first == slug }?.second ?: 0)
        }
    }

    Column(
        modifier = modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxSize()
            .clip(shape)
            .background(Panel)
            .border(1.dp, BubbleBorder, shape),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        ) {
            GroupTab(activeGroup == RECENT_GROUP, "Frequently used", { goTo(RECENT_GROUP) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_clock),
                    contentDescription = "Frequently used",
                    tint = if (activeGroup == RECENT_GROUP) Chroma else InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            groupIcons.forEach { (slug, icon) ->
                GroupTab(activeGroup == slug, slug, { goTo(slug) }) {
                    Text(icon, fontSize = 20.sp)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 2.dp, bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(horizontal = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), null, tint = InkMuted, modifier = Modifier.size(18.dp))
                SearchText(
                    value = query,
                    placeholder = "Search Emoji",
                    onChange = { query = it },
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onSearchFocus(it.isFocused) },
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onBackspace),
            ) {
                Icon(painterResource(R.drawable.ic_backspace), "Delete", tint = InkMuted)
            }
        }
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(44.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                results == null -> {
                    groupTitle("Frequently Used")
                    items(recent, key = { "recent-$it" }) { EmojiCell(it, pick) }
                    groups?.forEach { group ->
                        groupTitle(group.name)
                        items(group.items, key = { "${group.slug}-${it.emoji}" }) { EmojiCell(it.emoji, pick) }
                    }
                }
                results.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No emoji found",
                        color = InkMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
                    )
                }
                else -> items(results, key = { "found-${it.emoji}" }) { EmojiCell(it.emoji, pick) }
            }
        }
    }
}

private fun LazyGridScope.groupTitle(name: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = name,
            color = InkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun GroupTab(active: Boolean, label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) Chroma.copy(alpha = 0.14f) else Panel)
            .clickable(onClickLabel = label, onClick = onClick),
    ) {
        content()
    }
}

@Composable
private fun EmojiCell(emoji: String, onPick: (String) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPick(emoji) },
    ) {
        Text(emoji, fontSize = 26.sp)
    }
}
