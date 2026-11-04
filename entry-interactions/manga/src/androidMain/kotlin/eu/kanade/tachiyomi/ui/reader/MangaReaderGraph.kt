package eu.kanade.tachiyomi.ui.reader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.core.common.storage.StorageFileResolver

/** Graph entry points of the manga reader's activity and views, which Android constructs. */
@ContributesTo(AppScope::class)
interface MangaReaderGraph {
    val uiPreferences: UiPreferences
    val storageFiles: StorageFileResolver
}
