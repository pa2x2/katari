package mihon.entry.interactions.manga.child

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.manga.runtime.requireManga
import mihon.entry.interactions.runtime.EntryChildGroupFilterProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal object MangaChildGroupFilterProcessor : EntryChildGroupFilterProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun groupFor(entry: Entry, chapter: EntryChapter): String? {
        entry.requireManga()
        return normalizeGroup(entry, chapter.scanlator.orEmpty())
    }

    override fun normalizeGroup(entry: Entry, group: String): String? {
        entry.requireManga()
        return group.trim().takeIf(String::isNotEmpty)
    }
}
