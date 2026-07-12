package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry

@Immutable
data class GlobalSearchItem(val entry: Entry) {
    val id: Long get() = entry.id
    val source: Long get() = entry.source
    val title: String get() = entry.title
    val displayTitle: String get() = entry.displayTitle
    val favorite: Boolean get() = entry.favorite
    val thumbnailUrl: String? get() = entry.thumbnailUrl
    val url: String get() = entry.url
    val entryType: EntryType get() = entry.type
}
