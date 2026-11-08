package eu.kanade.tachiyomi.ui.reader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.domain.ui.UiPreferences
import mihon.entry.viewer.settings.ui.device.ViewerDeviceControls

/** Graph entry points of the manga reader's activity and views, which Android constructs. */
@ContributesTo(AppScope::class)
interface MangaReaderGraph {
    val uiPreferences: UiPreferences
    val viewerDeviceControls: ViewerDeviceControls
}
