package app.katari.presentation.kit.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * A scrolling column of page images read top to bottom, such as a long strip of comic pages. The pages fill the
 * width less [sideInset] at each side, which the reader works out from `KatariSizes.stripWidth` and its side padding
 * because its zoom needs the same figure; the list itself spans the window so a drag or tap beside the pages still
 * reaches it. Pages up to three quarters of a screen beyond either edge stay composed, so their images are decoded
 * before they scroll in. It shows no scroll indicator: readers show their own progress.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageStrip(
    state: LazyListState,
    sideInset: Dp,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = sideInset),
        cacheWindow = PageStripCacheWindow,
        content = content,
    )
}

// The default prefetch lays out only the next item, which a fast scroll reaches before its image is decoded
@OptIn(ExperimentalFoundationApi::class)
private val PageStripCacheWindow = LazyLayoutCacheWindow(CACHE_SCREENS, CACHE_SCREENS)

private const val CACHE_SCREENS = 0.75f
