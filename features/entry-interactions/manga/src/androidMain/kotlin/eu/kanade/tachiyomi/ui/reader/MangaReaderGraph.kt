package eu.kanade.tachiyomi.ui.reader

import app.katari.entry.viewer.settings.ui.device.ViewerDeviceControls
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.domain.ui.UiPreferences

/** Graph entry points of the manga reader's activity and views, which Android constructs. */
@ContributesTo(AppScope::class)
interface MangaReaderGraph {
    val uiPreferences: UiPreferences
    val viewerDeviceControls: ViewerDeviceControls
}
