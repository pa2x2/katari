package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class GetNextUnreadChapter(
    private val entryChapterRepository: EntryChapterRepository,
) {

    suspend fun await(entryId: Long): EntryChapter? {
        return entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            .filterNot { it.read }
            .minByOrNull { it.sourceOrder }
    }

    suspend fun await(entryId: Long, chapter: EntryChapter): EntryChapter? {
        return entryChapterRepository.getChaptersByEntryIdAwait(entryId)
            .sortedWith(compareBy({ it.chapterNumber }, { it.sourceOrder }))
            .let { sortedChapters ->
                val currIndex = sortedChapters.indexOfFirst { it.id == chapter.id }
                sortedChapters
                    .drop(currIndex + 1)
                    .firstOrNull { !it.read }
            }
    }
}
