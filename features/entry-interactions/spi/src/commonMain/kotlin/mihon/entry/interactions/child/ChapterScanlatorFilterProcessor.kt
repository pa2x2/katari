package mihon.entry.interactions.child

import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Filters children by the scanlator their source names, which every entry type keeps the same way. */
object ChapterScanlatorFilterProcessor : EntryScanlatorFilterProcessor {

    override fun scanlatorFor(entry: Entry, chapter: EntryChapter): String? =
        normalizeScanlator(entry, chapter.scanlator.orEmpty())

    override fun normalizeScanlator(entry: Entry, scanlator: String): String? =
        scanlator.trim().takeIf(String::isNotEmpty)
}
