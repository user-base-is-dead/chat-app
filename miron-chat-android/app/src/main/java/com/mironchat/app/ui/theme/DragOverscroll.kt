package com.mironchat.app.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

private class DragOnlyEffect(private val inner: OverscrollEffect) : OverscrollEffect by inner {
    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        if (source == NestedScrollSource.SideEffect && !inner.isInProgress) return performScroll(delta)
        return inner.applyToScroll(delta, source, performScroll)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        if (!inner.isInProgress) {
            performFling(velocity)
            return
        }
        inner.applyToFling(velocity, performFling)
    }
}

private class DragOnlyFactory(private val base: OverscrollFactory) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = DragOnlyEffect(base.createOverscrollEffect())

    override fun equals(other: Any?) = other is DragOnlyFactory && other.base == base

    override fun hashCode() = base.hashCode() * 31 + 1
}

@Composable
fun DragOnlyOverscroll(content: @Composable () -> Unit) {
    val base = LocalOverscrollFactory.current
    val factory = remember(base) { base?.let { DragOnlyFactory(it) } }
    CompositionLocalProvider(LocalOverscrollFactory provides factory, content = content)
}
