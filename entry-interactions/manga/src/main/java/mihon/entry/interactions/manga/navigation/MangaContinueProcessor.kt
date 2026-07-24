package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryContinueProcessor
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class MangaContinueProcessor(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryProgressRepository: EntryProgressRepository,
    private val openProcessor: MangaOpenProcessor,
) : EntryContinueProcessor {
    override val type: EntryType = EntryType.MANGA

    override suspend fun findNext(entry: Entry): EntryChapter? {
        entry.requireManga()
        val chapters = getEntryWithChapters.awaitChapters(entry)
        val chapterById = chapters.associateBy { it.id }
        return chapters
            .map { it.entryId }
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
            .asSequence()
            .filter { !it.completed && it.pageIndex > 0L }
            .sortedByDescending { it.lastReadAt }
            .mapNotNull { it.chapterId?.let(chapterById::get) }
            .firstOrNull()
            ?: chapters.firstOrNull { !it.read }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireManga()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
