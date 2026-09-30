package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Header that folds away as the content below it scrolls down and unfolds once the content is back at its top,
 * so it behaves as if it were the first row of the scrolled content while staying fixed across pages.
 */
@Stable
internal class LibraryCollapsingHeaderState {
    /** Measured header height; written during layout and only read while scrolling, so not snapshot state. */
    internal var heightPx = 0f

    /** How far the header is folded, from 0 (fully shown) to [heightPx]. */
    var collapsedPx by mutableFloatStateOf(0f)
        private set

    val isCollapsed: Boolean
        get() = collapsedPx > 0f

    fun expand() {
        collapsedPx = 0f
    }

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Fold before the content moves when scrolling down.
            if (available.y >= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Unfold only with what the content could not use, i.e. once it is back at its top.
            if (available.y <= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }
    }

    private fun consume(delta: Float): Float {
        val target = (collapsedPx - delta).coerceIn(0f, heightPx)
        val consumed = collapsedPx - target
        collapsedPx = target
        return consumed
    }
}

@Composable
internal fun rememberLibraryCollapsingHeaderState(): LibraryCollapsingHeaderState {
    return remember { LibraryCollapsingHeaderState() }
}

/** Reselecting the tab also unfolds the header, since the content it heads is being returned to its top. */
@Composable
internal fun BindHeaderToTop(target: LibraryScrollToTopTarget?, state: LibraryCollapsingHeaderState) {
    if (target == null) return
    DisposableEffect(target, state) {
        val handler: suspend () -> Boolean = {
            if (state.isCollapsed) {
                state.expand()
                true
            } else {
                false
            }
        }
        target.bind(handler)
        onDispose { target.unbind(handler) }
    }
}

@Composable
internal fun LibraryCollapsingHeader(
    state: LibraryCollapsingHeaderState,
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(header, content),
        modifier = modifier
            .clipToBounds()
            .nestedScroll(state.nestedScrollConnection),
    ) { (headerMeasurables, contentMeasurables), constraints ->
        val looseConstraints = constraints.copy(minHeight = 0)
        val headerPlaceables = headerMeasurables.map { it.measure(looseConstraints) }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        state.heightPx = headerHeight.toFloat()
        val collapsed = state.collapsedPx.coerceAtMost(headerHeight.toFloat()).roundToInt()
        val visibleHeaderHeight = headerHeight - collapsed

        val contentHeight = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight - visibleHeaderHeight).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val contentPlaceables = contentMeasurables.map {
            it.measure(looseConstraints.copy(minHeight = 0, maxHeight = contentHeight))
        }
        val width = constraints.maxWidth
        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            visibleHeaderHeight + (contentPlaceables.maxOfOrNull { it.height } ?: 0)
        }
        layout(width, height) {
            headerPlaceables.forEach { it.place(0, -collapsed) }
            contentPlaceables.forEach { it.place(0, visibleHeaderHeight) }
        }
    }
}
