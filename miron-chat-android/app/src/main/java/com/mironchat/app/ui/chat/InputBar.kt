package com.mironchat.app.ui.chat

import com.mironchat.app.ui.theme.SoftShadow
import androidx.compose.ui.draw.shadow
import android.icu.text.BreakIterator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.mironchat.app.R
import com.mironchat.app.ui.theme.BorderStrong
import com.mironchat.app.ui.theme.Chroma
import com.mironchat.app.ui.theme.Ink
import com.mironchat.app.ui.theme.InkMuted
import com.mironchat.app.ui.theme.MutedIcon
import com.mironchat.app.ui.theme.Panel
import com.mironchat.app.ui.theme.Surface
import com.mironchat.app.ui.theme.bouncySpring
import com.mironchat.app.ui.theme.pressScale
import com.mironchat.app.ui.theme.softSpring
import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.snapshotFlow

private const val KEYBOARD_RISE = 900L
private const val KEYBOARD_SETTLE = 120L
private const val INPUT_PREFS = "input"
private const val KEYBOARD_KEY = "keyboard_height"
private val BarShape = RoundedCornerShape(28.dp)
private val PanelStart = 300.dp
private val PanelFloor = 150.dp
private val PanelMax = 520.dp
private val PanelWithKeyboard = 240.dp

private fun TextFieldValue.replaced(start: Int, end: Int, text: String): TextFieldValue {
    val at = start + text.length
    return copy(text = this.text.replaceRange(start, end, text), selection = TextRange(at))
}

private fun TextFieldValue.deletedBefore(): TextFieldValue {
    if (!selection.collapsed) return replaced(selection.min, selection.max, "")
    val end = selection.min
    if (end == 0) return this
    val parts = BreakIterator.getCharacterInstance()
    parts.setText(text)
    val start = parts.preceding(end).coerceAtLeast(0)
    return replaced(start, end, "")
}

@OptIn(FlowPreview::class)
@Composable
fun InputBar(
    focus: FocusRequester,
    onTyping: () -> Unit,
    onSend: (String) -> Unit,
    onAttach: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var emojiOpen by remember { mutableStateOf(false) }
    var emojiSearching by remember { mutableStateOf(false) }
    var keyboardRising by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val saved = remember { context.getSharedPreferences(INPUT_PREFS, Context.MODE_PRIVATE) }
    var panelHeight by remember { mutableStateOf(saved.getFloat(KEYBOARD_KEY, 0f).takeIf { it > 0f }?.dp ?: PanelStart) }
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    val keyboardHeight = with(density) {
        (imeInsets.getBottom(this) - navInsets.getBottom(this)).coerceAtLeast(0).toDp()
    }
    val ready = value.text.isNotBlank()
    val sendSource = remember { MutableInteractionSource() }
    val sendScale by animateFloatAsState(if (ready) 1f else 0.9f, bouncySpring(), label = "sendScale")
    val sendTurn by animateFloatAsState(if (ready) 0f else -35f, bouncySpring(), label = "sendTurn")
    val sendColor by animateColorAsState(if (ready) Chroma else Surface, label = "sendColor")
    val sendTint by animateColorAsState(if (ready) Color.White else MutedIcon, label = "sendTint")
    val closeForKeyboard = {
        if (emojiOpen) {
            emojiOpen = false
            keyboardRising = true
        }
    }

    val keyboardFlow = remember(density, imeInsets, navInsets) {
        snapshotFlow {
            with(density) { (imeInsets.getBottom(this) - navInsets.getBottom(this)).coerceAtLeast(0).toDp() }
        }
    }
    val fitPanel: (Dp) -> Unit = { height ->
        if (height >= PanelFloor) {
            val next = height.coerceAtMost(PanelMax)
            if (next != panelHeight) {
                panelHeight = next
                saved.edit().putFloat(KEYBOARD_KEY, next.value).apply()
            }
        }
    }

    LaunchedEffect(keyboardFlow) {
        keyboardFlow.debounce(KEYBOARD_SETTLE).collect { fitPanel(it) }
    }

    LaunchedEffect(keyboardRising) {
        if (!keyboardRising) return@LaunchedEffect
        val settled = withTimeoutOrNull(KEYBOARD_RISE) {
            keyboardFlow.first { it > 0.dp }
            keyboardFlow.debounce(KEYBOARD_SETTLE).first()
        }
        if (settled != null) fitPanel(settled)
        keyboardRising = false
    }

    BackHandler(enabled = emojiOpen) {
        emojiOpen = false
    }

    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                .fillMaxWidth()
                .shadow(10.dp, BarShape, ambientColor = SoftShadow, spotColor = SoftShadow)
                .clip(BarShape)
                .background(Panel)
                .border(1.dp, BorderStrong, BarShape)
                .padding(4.dp),
        ) {
            BarButton(R.drawable.ic_attach, "Attach", active = false, onClick = onAttach)
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                if (value.text.isEmpty()) Text("Message", color = InkMuted, fontSize = 17.5.sp)
                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (it.text.length > value.text.length) onTyping()
                        value = it
                    },
                    maxLines = 6,
                    textStyle = TextStyle(color = Ink, fontSize = 17.5.sp, lineHeight = 23.sp),
                    cursorBrush = SolidColor(Chroma),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                closeForKeyboard()
                            }
                        },
                )
            }
            BarButton(
                icon = if (emojiOpen) R.drawable.ic_keyboard else R.drawable.ic_emoji,
                label = if (emojiOpen) "Keyboard" else "Emoji",
                active = emojiOpen,
                onClick = {
                    if (emojiOpen) {
                        closeForKeyboard()
                        focus.requestFocus()
                        keyboard?.show()
                    } else {
                        emojiOpen = true
                        keyboard?.hide()
                    }
                },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .pressScale(sendSource, pressed = 0.88f)
                    .clip(CircleShape)
                    .background(sendColor)
                    .clickable(interactionSource = sendSource, indication = null, enabled = ready) {
                        onSend(value.text.trim())
                        value = TextFieldValue("")
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Send",
                    tint = sendTint,
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .size(21.dp)
                        .graphicsLayer {
                            rotationZ = sendTurn
                            scaleX = sendScale
                            scaleY = sendScale
                        },
                )
            }
        }
        if (emojiOpen || keyboardRising) {
            val searching = emojiSearching && keyboardHeight > 0.dp
            val full = if (searching) PanelWithKeyboard else panelHeight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val keyboardPx = (imeInsets.getBottom(this) - navInsets.getBottom(this)).coerceAtLeast(0)
                        val slot = if (searching) full.roundToPx() else (full.roundToPx() - keyboardPx).coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                        layout(constraints.maxWidth, slot) { placeable.place(0, 0) }
                    },
            ) {
                EmojiPanel(
                    onPick = { emoji -> value = value.replaced(value.selection.min, value.selection.max, emoji) },
                    onBackspace = { value = value.deletedBefore() },
                    onSearchFocus = { emojiSearching = it },
                    modifier = Modifier.height(full),
                )
            }
        }
    }
}

@Composable
private fun BarButton(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .pressScale(source, pressed = 0.86f)
            .clip(CircleShape)
            .background(if (active) Chroma.copy(alpha = 0.11f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        Icon(painterResource(icon), label, tint = if (active) Chroma else InkMuted)
    }
}
