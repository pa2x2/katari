package app.katari.entry.interactions.anime.introtimes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/** Reads the scope an episode's times are matched by, for every service alike. */
@Inject
@SingleIn(AppScope::class)
internal class IntroTimesEpisodeReader(
    private val entryRepository: EntryRepository,
    private val chapterRepository: EntryChapterRepository,
    private val groupRepository: EntryChapterGroupRepository,
) {
    suspend fun episode(chapterId: Long): IntroTimesEpisode? {
        val chapter = chapterRepository.getChapterById(chapterId) ?: return null
        val entry = entryRepository.getEntryById(chapter.entryId) ?: return null
        return introTimesEpisode(
            entry = entry,
            episode = chapter,
            entryEpisodes = chapterRepository.getChaptersByEntryIdAwait(entry.id),
            groupTitles = groupRepository.getByEntryId(entry.id).associate { it.key to it.title },
        )
    }
}
