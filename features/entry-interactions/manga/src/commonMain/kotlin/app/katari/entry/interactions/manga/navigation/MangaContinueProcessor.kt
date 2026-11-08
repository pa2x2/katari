package app.katari.entry.interactions.manga.navigation

import app.katari.entry.interactions.manga.runtime.requireManga
import app.katari.entry.interactions.manga.state.lastReadAt
import app.katari.entry.interactions.manga.state.pageIndex
import app.katari.entry.interactions.navigation.EntryContinueProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.service.sortedForReading

internal object MangaContinueProcessor : EntryContinueProcessor {
    override fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? {
        entry.requireManga()
        val chapterById = chapters.associateBy { it.id }
        return progressStates
            .asSequence()
            .filter { !it.completed && it.pageIndex > 0L }
            .sortedByDescending { it.lastReadAt }
            .mapNotNull { it.chapterId?.let(chapterById::get) }
            .firstOrNull()
            ?: chapters.sortedForReading(entry).firstOrNull { !it.read }
    }
}
