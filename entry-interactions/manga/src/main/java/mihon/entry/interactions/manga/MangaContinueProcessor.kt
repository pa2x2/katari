package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryContinueProcessor
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class MangaContinueProcessor(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val openProcessor: MangaOpenProcessor,
) : EntryContinueProcessor {
    override val type: EntryType = EntryType.MANGA

    override suspend fun findNext(entry: Entry): EntryChapter? {
        entry.requireManga()
        return getEntryWithChapters.awaitChapters(entry.id).firstOrNull { !it.read }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireManga()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
