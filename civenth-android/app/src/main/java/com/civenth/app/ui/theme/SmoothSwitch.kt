package com.civenth.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SwitchWidth = 52.dp
private val SwitchThumb = 26.dp
private val SwitchGap = 3.dp
private const val SWITCH_TIME = 220

@Composable
fun SmoothSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val thumbX by animateDpAsState(
        targetValue = if (checked) SwitchWidth - SwitchThumb - SwitchGap * 2 else 0.dp,
        animationSpec = tween<Dp>(SWITCH_TIME, easing = FastOutSlowInEasing),
        label = "thumb",
    )
    val mix by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(SWITCH_TIME, easing = FastOutSlowInEasing),
        label = "track",
    )
    val track = androidx.compose.ui.graphics.lerp(SwitchOff, Chroma, mix)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .width(SwitchWidth)
            .height(SwitchThumb + SwitchGap * 2)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onChange,
            )
            .padding(SwitchGap),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(SwitchThumb)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
