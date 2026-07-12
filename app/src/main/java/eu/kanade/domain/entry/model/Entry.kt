package eu.kanade.domain.entry.model

import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun Entry.hasCustomCover(coverCache: CoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}

fun Entry.chaptersFiltered(libraryPreferences: LibraryPreferences = Injekt.get()): Boolean {
    val downloadedFilter = if (libraryPreferences.downloadedOnly.get()) {
        TriState.ENABLED_IS
    } else {
        this.downloadedFilter
    }

    return unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED
}
