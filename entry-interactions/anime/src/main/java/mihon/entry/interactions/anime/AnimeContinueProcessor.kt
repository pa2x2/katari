package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.EntryContinueProcessor
import mihon.entry.interactions.EntryOpenOptions
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class AnimeContinueProcessor(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryProgressRepository: EntryProgressRepository,
    private val openProcessor: AnimeOpenProcessor,
) : EntryContinueProcessor {
    override val type: EntryType = EntryType.ANIME

    override suspend fun findNext(entry: Entry): EntryChapter? {
        entry.requireAnime()
        val chapters = getEntryWithChapters.awaitChapters(entry.id)
        if (chapters.isEmpty()) return null

        val chapterIds = chapters.map { it.id }.toSet()
        val states = chapters
            .map { it.entryId }
            .distinct()
            .flatMap { entryId -> entryProgressRepository.getByEntryId(entryId) }
            .filter { it.chapterId in chapterIds }
        val stateByChapterId = states.associateBy { it.chapterId }

        return chapters
            .sortedBy { it.sourceOrder }
            .firstOrNull { chapter ->
                val state = stateByChapterId[chapter.id]
                state != null && state.positionMs > 0L && !state.completed
            }
            ?: chapters
                .sortedBy { it.sourceOrder }
                .firstOrNull { chapter ->
                    !chapter.read && stateByChapterId[chapter.id]?.completed != true
                }
    }

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        entry.requireAnime()
        openProcessor.open(context, entry, chapter, EntryOpenOptions())
    }
}
