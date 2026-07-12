package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class GetNextChapter(
    private val entryChapterRepository: EntryChapterRepository,
) {

    suspend fun await(entryId: Long, chapter: EntryChapter): EntryChapter? {
        val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entryId)
        return chapters
            .sortedWith(compareBy({ it.chapterNumber }, { it.sourceOrder }))
            .let { sortedChapters ->
                val currIndex = sortedChapters.indexOfFirst { it.id == chapter.id }
                sortedChapters.getOrNull(currIndex + 1)
            }
    }
}
