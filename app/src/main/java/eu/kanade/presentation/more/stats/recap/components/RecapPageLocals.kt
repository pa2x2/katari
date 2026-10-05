package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette

/** What a page reads from the story it's shown in. */
internal val LocalRecapPalette = staticCompositionLocalOf { RecapPalette.Default }

/**
 * How far the page's entrance has played, 0 to 1. Pages shown outside a story, such as the summary card on its own,
 * get a finished entrance.
 */
internal val LocalRecapReveal = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

/** Where the page's titles are, so holding one can offer to hide it; null where titles can't be hidden. */
internal val LocalRecapTitleTargets = staticCompositionLocalOf<RecapTitleTargets?> { null }
