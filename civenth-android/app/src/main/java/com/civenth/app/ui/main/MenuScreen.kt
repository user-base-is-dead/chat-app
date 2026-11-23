package com.civenth.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
    val dark = com.civenth.app.ui.theme.Appearance.dark
    val foreground = if (dark) Color.Black else Color.White
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
            .background(if (dark) Color.White else Color.Black),
    ) {
        val brand = stage(progress.value, 0.4f)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (origin.y - 17.dp.toPx()).roundToInt()) }
                .graphicsLayer {
                    alpha = brand
                    translationY = (1f - brand) * -24.dp.toPx()
                },
        ) {
            Image(painterResource(R.drawable.logo), null, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)))
            Text("Civenth", color = foreground, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 20.dp),
        ) {
            val line = foreground.copy(alpha = 0.16f)
            listOf("Chat" to true, "Crypto Cold Wallet" to false, "Buy & Sell" to false).forEachIndexed { index, (name, ready) ->
                val shown = stage(progress.value, 0.42f + index * 0.1f)
                if (index == 0) Box(Modifier.fillMaxWidth(shown).height(1.dp).background(line))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = shown
                            translationY = (1f - shown) * 40.dp.toPx()
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = ready, onClick = close)
                        .padding(vertical = 20.dp, horizontal = 4.dp),
                ) {
                    val dim = if (ready) 1f else 0.4f
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        color = foreground.copy(alpha = 0.5f * dim),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Top).padding(top = 6.dp),
                    )
                    Text(
                        text = name,
                        color = foreground.copy(alpha = dim),
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (ready) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(foreground),
                        ) {
                            Icon(painterResource(R.drawable.ic_chevron), null, tint = if (dark) Color.White else Color.Black, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Text(
                            text = "SOON",
                            color = foreground.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                            modifier = Modifier
                                .border(1.dp, foreground.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth(shown).height(1.dp).background(line))
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
                .background(foreground.copy(alpha = 0.18f))
                .clickable(onClick = close),
        ) {
            Icon(painterResource(R.drawable.ic_close), "Close", tint = foreground, modifier = Modifier.size(22.dp))
        }
    }
}
