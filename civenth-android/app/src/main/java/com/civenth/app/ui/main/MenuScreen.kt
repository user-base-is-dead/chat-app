package com.civenth.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civenth.app.R
import com.civenth.app.ui.theme.Chroma
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private val Sweep = CubicBezierEasing(0.87f, 0f, 0.13f, 1f)
private const val OPEN_MS = 720
private const val CLOSE_MS = 560

object SideMenu {
    var open by mutableStateOf(false)
    var origin by mutableStateOf(Offset.Zero)

    fun show(from: Offset) {
        origin = from
        open = true
    }
}

private fun stage(progress: Float, start: Float, length: Float = 0.35f) = ((progress - start) / length).coerceIn(0f, 1f)

@Composable
fun MenuScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(SideMenu.open) {
        progress.animateTo(if (SideMenu.open) 1f else 0f, tween(if (SideMenu.open) OPEN_MS else CLOSE_MS, easing = Sweep))
    }
    if (!SideMenu.open && progress.value == 0f) return
    BackHandler(enabled = SideMenu.open) { SideMenu.open = false }
    val origin = SideMenu.origin
    val close = { SideMenu.open = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .drawWithContent {
                val far = max(
                    max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                    max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                )
                val circle = Path().apply { addOval(Rect(origin, far * progress.value)) }
                clipPath(circle) { this@drawWithContent.drawContent() }
            }
            .background(Color(0xFF252525)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
        ) {
            listOf(
                Triple("Coming soon", 44.sp, FontWeight.ExtraBold),
                Triple("New options will show up here.", 17.sp, FontWeight.Medium),
            ).forEachIndexed { index, (text, size, weight) ->
                val shown = stage(progress.value, 0.45f + index * 0.12f)
                Text(
                    text = text,
                    color = Color.White.copy(alpha = if (index == 0) 1f else 0.82f),
                    fontSize = size,
                    fontWeight = weight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = shown
                        translationY = (1f - shown) * 48.dp.toPx()
                    },
                )
            }
        }
        val turn = stage(progress.value, 0.3f, 0.5f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset((origin.x - 22.dp.toPx()).roundToInt(), (origin.y - 22.dp.toPx()).roundToInt()) }
                .size(44.dp)
                .graphicsLayer {
                    rotationZ = (1f - turn) * -90f
                    scaleX = 0.6f + turn * 0.4f
                    scaleY = scaleX
                    alpha = turn
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = close),
        ) {
            Icon(painterResource(R.drawable.ic_close), "Close", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}
