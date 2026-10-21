package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.chip.FloatingChip
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Reveals the back-to-top button only once the reader deliberately heads back up: their scrolling, flings included,
 * has to travel [REVEAL_VIEWPORT_FRACTION] of the viewport upward without scrolling down in between. Small
 * corrections while reading stay below that, and any downward scroll hides the button again.
 *
 * Only user scrolls pass through nested scroll, so restoring the saved anchor or keeping the position across
 * prepended items never reveals it.
 */
@Stable
internal class FeedBackToTopState(
    private val viewportHeight: () -> Int,
) : NestedScrollConnection {
    var isRevealed by mutableStateOf(false)
        private set

    private var upwardTravel = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        // Positive y moves the content down, toward the top of the feed.
        when {
            consumed.y > 0f -> {
                upwardTravel += consumed.y
                if (upwardTravel >= viewportHeight() * REVEAL_VIEWPORT_FRACTION) isRevealed = true
            }
            consumed.y < 0f -> hide()
        }
        return Offset.Zero
    }

    fun hide() {
        upwardTravel = 0f
        isRevealed = false
    }
}

@Composable
internal fun rememberFeedBackToTopState(viewportHeight: () -> Int): FeedBackToTopState {
    val currentViewportHeight by rememberUpdatedState(viewportHeight)
    return remember { FeedBackToTopState { currentViewportHeight() } }
}

@Composable
internal fun FeedBackToTopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingChip(
        label = stringResource(MR.strings.action_move_to_top),
        icon = MaterialSymbols.Rounded.KeyboardArrowUp,
        onClick = onClick,
        modifier = modifier,
    )
}

private const val REVEAL_VIEWPORT_FRACTION = 0.3f
