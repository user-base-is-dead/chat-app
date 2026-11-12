package com.meghnathnews.mironchat.ui.chat

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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import com.meghnathnews.mironchat.R
import com.meghnathnews.mironchat.ui.theme.BorderStrong
import com.meghnathnews.mironchat.ui.theme.Chroma
import com.meghnathnews.mironchat.ui.theme.Ink
import com.meghnathnews.mironchat.ui.theme.InkMuted
import com.meghnathnews.mironchat.ui.theme.MutedIcon
import com.meghnathnews.mironchat.ui.theme.Panel
import com.meghnathnews.mironchat.ui.theme.Surface
import com.meghnathnews.mironchat.ui.theme.bouncySpring
import com.meghnathnews.mironchat.ui.theme.pressScale
import com.meghnathnews.mironchat.ui.theme.softSpring
import kotlinx.coroutines.delay

private const val KEYBOARD_RISE = 350L
private val BarShape = RoundedCornerShape(31.dp)
private val PanelStart = 300.dp
private val PanelMin = 260.dp
private val PanelMax = 360.dp
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

@Composable
fun InputBar(
    focus: FocusRequester,
    onTyping: () -> Unit,
    onSend: (String) -> Unit,
    onAttach: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var focused by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var emojiSearching by remember { mutableStateOf(false) }
    var keyboardRising by remember { mutableStateOf(false) }
    var panelHeight by remember { mutableStateOf(PanelStart) }
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val keyboardHeight = with(density) {
        (WindowInsets.ime.getBottom(this) - WindowInsets.navigationBars.getBottom(this)).coerceAtLeast(0).toDp()
    }
    val ready = value.text.isNotBlank()
    val sendSource = remember { MutableInteractionSource() }
    val sendScale by animateFloatAsState(if (ready) 1f else 0.9f, bouncySpring(), label = "sendScale")
    val sendTurn by animateFloatAsState(if (ready) 0f else -35f, bouncySpring(), label = "sendTurn")
    val sendColor by animateColorAsState(if (ready) Chroma else Surface, label = "sendColor")
    val sendTint by animateColorAsState(if (ready) Color.White else MutedIcon, label = "sendTint")
    val edge by animateColorAsState(if (focused) Chroma.copy(alpha = 0.45f) else BorderStrong, label = "edge")
    val closeForKeyboard = {
        if (emojiOpen) {
            emojiOpen = false
            keyboardRising = true
        }
    }

    SideEffect {
        if (keyboardHeight > panelHeight || panelHeight == PanelStart && keyboardHeight > PanelMin) {
            panelHeight = keyboardHeight.coerceIn(PanelMin, PanelMax)
        }
    }

    LaunchedEffect(keyboardRising) {
        if (!keyboardRising) return@LaunchedEffect
        delay(KEYBOARD_RISE)
        keyboardRising = false
    }

    BackHandler(enabled = emojiOpen) {
        emojiOpen = false
    }

    Column {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
                .clip(BarShape)
                .background(Panel)
                .border(1.dp, edge, BarShape)
                .padding(8.dp),
        ) {
            BarButton(R.drawable.ic_attach, "Attach", active = false, onClick = onAttach)
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                if (value.text.isEmpty()) Text("Message", color = InkMuted, fontSize = 15.5.sp)
                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (it.text.length > value.text.length) onTyping()
                        value = it
                    },
                    maxLines = 6,
                    textStyle = TextStyle(color = Ink, fontSize = 15.5.sp),
                    cursorBrush = SolidColor(Chroma),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused }
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
            val slot = if (searching) PanelWithKeyboard else (panelHeight - keyboardHeight).coerceAtLeast(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(slot)
                    .clipToBounds(),
            ) {
                EmojiPanel(
                    onPick = { emoji -> value = value.replaced(value.selection.min, value.selection.max, emoji) },
                    onBackspace = { value = value.deletedBefore() },
                    onSearchFocus = { emojiSearching = it },
                    modifier = Modifier.requiredHeight(if (searching) PanelWithKeyboard else panelHeight),
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
            .padding(vertical = 2.dp)
            .size(42.dp)
            .pressScale(source, pressed = 0.86f)
            .clip(CircleShape)
            .background(if (active) Chroma.copy(alpha = 0.11f) else Color.Transparent)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        Icon(painterResource(icon), label, tint = if (active) Chroma else InkMuted)
    }
}
