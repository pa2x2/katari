package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.runtime.staticCompositionLocalOf
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette

/** What a page reads from the story it's shown in. */
internal val LocalRecapPalette = staticCompositionLocalOf { RecapPalette.Default }

/** Where the page's titles are, so holding one can offer to hide it; null where titles can't be hidden. */
internal val LocalRecapTitleTargets = staticCompositionLocalOf<RecapTitleTargets?> { null }
