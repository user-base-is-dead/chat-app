package com.meghnathnews.mironchat.ui.rooms

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.data.AddResult
import com.meghnathnews.mironchat.data.ApiError
import com.meghnathnews.mironchat.data.Chat
import com.meghnathnews.mironchat.data.Person
import com.meghnathnews.mironchat.data.RoomRepository
import com.meghnathnews.mironchat.data.roomLabel
import com.meghnathnews.mironchat.ui.chats.Avatar
import com.meghnathnews.mironchat.ui.theme.Card
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Danger
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.bouncySpring
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PHOTO = 5 * 1024 * 1024
private const val MIN_QUERY = 2
private const val SEARCH_WAIT = 250L

data class RoomDraft(
    val title: String = "",
    val about: String = "",
    val isPublic: Boolean = false,
    val username: String = "",
    val photoId: String = "",
    val photo: String = "",
)

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onBack),
        ) {
            Icon(painterResource(R.drawable.ic_back), "Back", tint = Ink)
        }
        Text(title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Chroma, disabledContainerColor = Chroma),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    limit: Int,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
    plain: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = InkMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(limit)) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
            cursorBrush = SolidColor(Chroma),
            keyboardOptions = KeyboardOptions(
                capitalization = if (plain) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                autoCorrectEnabled = !plain,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Card)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun TypeSwitch(isPublic: Boolean, onChange: (Boolean) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .padding(4.dp),
    ) {
        val half = maxWidth / 2
        val pill by animateDpAsState(if (isPublic) half else 0.dp, bouncySpring(), label = "type")
        Box(
            modifier = Modifier
                .offset(x = pill)
                .width(half)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Chroma),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            listOf("Private" to false, "Public" to true).forEach { (label, value) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChange(value) },
                ) {
                    Text(
                        text = label,
                        color = if (isPublic == value) Color.White else InkMuted,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun RoomForm(kind: String, draft: RoomDraft, rooms: RoomRepository, onChange: (RoomDraft) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf("") }
    val label = roomLabel(kind)
    val current by androidx.compose.runtime.rememberUpdatedState(draft)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            photoError = ""
            try {
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IOException()
                if (bytes.size > MAX_PHOTO) {
                    photoError = "Photo must be under 5 MB"
                } else {
                    val photo = rooms.uploadPhoto(bytes, type)
                    onChange(current.copy(photoId = photo.id, photo = photo.url))
                }
            } catch (error: ApiError) {
                photoError = error.message.orEmpty()
            } catch (_: IOException) {
                photoError = "Could not read the photo"
            } finally {
                uploading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(96.dp)
                .clip(CircleShape)
                .background(Chroma)
                .clickable(enabled = !uploading) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
        ) {
            when {
                uploading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp))
                draft.photo.isNotEmpty() -> Avatar(draft.title.ifEmpty { label }, 96.dp, photo = draft.photo)
                else -> Icon(painterResource(R.drawable.ic_camera), "Choose photo", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        if (photoError.isNotEmpty()) {
            Text(photoError, color = Danger, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Field("$label name", draft.title, 64, { onChange(draft.copy(title = it)) })
        Field("Description", draft.about, 255, { onChange(draft.copy(about = it)) }, singleLine = false)
        TypeSwitch(draft.isPublic) { onChange(draft.copy(isPublic = it)) }
        Text(
            text = if (draft.isPublic) "Anyone can find and join it. People you add join directly."
            else "Only people with the link can join. People you add get an invite.",
            color = InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (draft.isPublic) {
            Field("@username", draft.username, 32, { onChange(draft.copy(username = it.removePrefix("@"))) }, plain = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberPicker(
    known: List<Person>,
    selected: List<Person>,
    excludeIds: Set<String>,
    onSearch: suspend (String) -> List<Person>,
    onChange: (List<Person>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var foundFor by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(emptyList<Person>()) }
    val needle = query.trim().removePrefix("@")
    val searching = needle.length >= MIN_QUERY
    val loading = searching && foundFor != needle
    val people = (if (searching) (if (loading) emptyList() else found) else known).filterNot { it.id in excludeIds }

    LaunchedEffect(needle) {
        if (!searching) return@LaunchedEffect
        delay(SEARCH_WAIT)
        found = try {
            onSearch(needle)
        } catch (_: ApiError) {
            emptyList()
        }
        foundFor = needle
    }

    val toggle: (Person) -> Unit = { person ->
        onChange(if (selected.any { it.id == person.id }) selected.filterNot { it.id == person.id } else selected + person)
    }

    Column(modifier = modifier) {
        if (selected.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            ) {
                selected.forEach { person ->
                    Text(
                        text = "${person.displayName}  ×",
                        color = Ink,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Chroma.copy(alpha = 0.14f))
                            .clickable { toggle(person) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Card)
                .padding(horizontal = 14.dp),
        ) {
            if (query.isEmpty()) Text("Search by username", color = InkMuted, fontSize = 15.sp)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Chroma),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            loading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
                CircularProgressIndicator(color = Chroma, modifier = Modifier.size(24.dp))
            }
            people.isEmpty() -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
                Text(if (searching) "No users found" else "Search a username to add people", color = InkMuted, fontSize = 14.sp)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                items(people, key = { it.id }) { person ->
                    val picked = selected.any { it.id == person.id }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { toggle(person) }
                            .padding(8.dp),
                    ) {
                        Avatar(person.displayName, 44.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.displayName, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@${person.username}", color = InkMuted, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (picked) Chroma else BorderStrong),
                        ) {
                            if (picked) Icon(painterResource(R.drawable.ic_tick), "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCreateScreen(
    kind: String,
    known: List<Person>,
    rooms: RoomRepository,
    onSearch: suspend (String) -> List<Person>,
    onBack: () -> Unit,
    onCreated: (Chat, AddResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var choosing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(RoomDraft()) }
    var members by remember { mutableStateOf(emptyList<Person>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val label = roomLabel(kind)
    val goBack = { if (choosing) choosing = false else onBack() }

    BackHandler(onBack = goBack)

    val next = {
        error = when {
            draft.title.isBlank() -> "$label name is required"
            draft.isPublic && draft.username.isBlank() -> "Public needs a username"
            else -> ""
        }
        if (error.isEmpty()) choosing = true
    }

    val create: () -> Unit = {
        scope.launch {
            busy = true
            error = ""
            try {
                val (chat, result) = rooms.create(
                    kind = kind,
                    title = draft.title,
                    about = draft.about,
                    isPublic = draft.isPublic,
                    username = draft.username,
                    photoId = draft.photoId,
                    userIds = members.map { it.id },
                )
                onCreated(chat, result)
            } catch (failure: ApiError) {
                error = failure.message.orEmpty()
                choosing = false
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(if (choosing) "Add Members" else "New $label", goBack)
        if (choosing) {
            MemberPicker(
                known = known,
                selected = members,
                excludeIds = emptySet(),
                onSearch = onSearch,
                onChange = { members = it },
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                RoomForm(kind, draft, rooms) { draft = it }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (error.isNotEmpty()) {
                Text(error, color = Danger, fontSize = 13.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            }
            PrimaryButton(if (choosing) "Create $label" else "Next", busy, if (choosing) create else next)
        }
    }
}
