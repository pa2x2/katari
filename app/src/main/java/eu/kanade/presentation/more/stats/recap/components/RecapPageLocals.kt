package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.runtime.staticCompositionLocalOf
import mihon.presentation.kit.theme.StoryPalette

/** The colours of the page being drawn, for what the page draws itself rather than lays out as text. */
internal val LocalRecapPalette = staticCompositionLocalOf<StoryPalette> { error("Provided by RecapPageFrame") }

/** Where the page's titles are, so holding one can offer to hide it; null where titles can't be hidden. */
internal val LocalRecapTitleTargets = staticCompositionLocalOf<RecapTitleTargets?> { null }
