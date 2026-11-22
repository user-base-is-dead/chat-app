package com.civenth.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val STAGGER = 35L

fun <T> softSpring(visibility: T? = null) = spring(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = visibility,
)

fun <T> bouncySpring(visibility: T? = null) = spring(
    dampingRatio = 0.62f,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = visibility,
)

fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val down by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (down) pressed else 1f, bouncySpring(), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun Modifier.riseIn(order: Int?): Modifier = composed {
    val progress = remember { Animatable(if (order == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (order == null) return@LaunchedEffect
        delay(order * STAGGER)
        progress.animateTo(1f, softSpring())
    }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 22.dp.toPx()
    }
}

fun Modifier.pressClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source, 0.98f).clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.pressCombined(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source, 0.98f).combinedClickable(
        interactionSource = source,
        indication = LocalIndication.current,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
