package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryPinnedDisplayStyle

@Composable
internal fun Modifier.libraryPinnedGridDecoration(
    state: LazyGridState,
    items: List<LibraryItem>,
    style: LibraryPinnedDisplayStyle,
    contentPadding: PaddingValues,
): Modifier {
    if (items.isEmpty() || style == LibraryPinnedDisplayStyle.Shelf) return this
    val keys = rememberPinnedSectionKeys(items)
    val tonalColor = KatariTheme.colors.surfaceContainer
    val tonalShape = KatariTheme.shapes.large
    return drawBehind {
        val visibleItems = state.layoutInfo.visibleItemsInfo.filter { it.key in keys }
        if (visibleItems.isEmpty()) return@drawBehind
        val top = if (visibleItems.any { it.key == LIBRARY_PINNED_HEADER_KEY }) {
            visibleItems.minOf { it.offset.y }.toFloat()
        } else {
            0f
        }
        val bottom = if (visibleItems.any { it.key == LIBRARY_PINNED_FOOTER_KEY }) {
            visibleItems.maxOf { it.offset.y + it.size.height }.toFloat()
        } else {
            size.height
        }
        val gridInset = KatariTheme.spacing.s
        val left = (contentPadding.calculateLeftPadding(layoutDirection) + gridInset).toPx()
        val right = size.width - (contentPadding.calculateRightPadding(layoutDirection) + gridInset).toPx()
        drawPinnedDecoration(style, tonalColor, tonalShape, left, right, top, bottom)
    }
}

@Composable
internal fun Modifier.libraryPinnedListDecoration(
    state: LazyListState,
    items: List<LibraryItem>,
    style: LibraryPinnedDisplayStyle,
    contentPadding: PaddingValues,
): Modifier {
    if (items.isEmpty() || style == LibraryPinnedDisplayStyle.Shelf) return this
    val keys = rememberPinnedSectionKeys(items)
    val tonalColor = KatariTheme.colors.surfaceContainer
    val tonalShape = KatariTheme.shapes.large
    return drawBehind {
        val visibleItems = state.layoutInfo.visibleItemsInfo.filter { it.key in keys }
        if (visibleItems.isEmpty()) return@drawBehind
        val top = if (visibleItems.any { it.key == LIBRARY_PINNED_HEADER_KEY }) {
            visibleItems.minOf { it.offset }.toFloat()
        } else {
            0f
        }
        val bottom = if (visibleItems.any { it.key == LIBRARY_PINNED_FOOTER_KEY }) {
            visibleItems.maxOf { it.offset + it.size }.toFloat()
        } else {
            size.height
        }
        val left = contentPadding.calculateLeftPadding(layoutDirection).toPx()
        val right = size.width - contentPadding.calculateRightPadding(layoutDirection).toPx()
        drawPinnedDecoration(style, tonalColor, tonalShape, left, right, top, bottom)
    }
}

@Composable
private fun rememberPinnedSectionKeys(items: List<LibraryItem>): Set<String> {
    return remember(items) {
        buildSet {
            add(LIBRARY_PINNED_HEADER_KEY)
            add(LIBRARY_PINNED_FOOTER_KEY)
            items.forEach { add(libraryPinnedItemKey(it)) }
        }
    }
}

private fun DrawScope.drawPinnedDecoration(
    style: LibraryPinnedDisplayStyle,
    tonalColor: Color,
    tonalShape: Shape,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
) {
    if (right <= left || bottom <= top) return
    when (style) {
        LibraryPinnedDisplayStyle.TonalGroup -> {
            translate(left = left, top = top) {
                drawOutline(
                    outline = tonalShape.createOutline(Size(right - left, bottom - top), layoutDirection, this),
                    color = tonalColor,
                )
            }
        }
        LibraryPinnedDisplayStyle.Shelf -> Unit
    }
}
