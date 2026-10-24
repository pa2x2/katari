package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.ChapterKind
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryChapterFilters

class VideoPlayerEpisodeWindowTest {

    @Test
    fun `next and previous skip episodes the filters hide while the playing one keeps its place`() {
        val entry = Entry.create().copy(
            id = 1L,
            type = EntryType.ANIME,
            chapterFlags = Entry.CHAPTER_SORTING_NUMBER or Entry.CHAPTER_SHOW_DOWNLOADED or
                Entry.CHAPTER_SHOW_NOT_FILLER,
        )
        val episodes = (1L..5L).map { number ->
            EntryChapter.create().copy(
                id = number,
                entryId = 1L,
                url = "/episode-$number",
                name = "Episode $number",
                chapterNumber = number.toDouble(),
                kind = if (number == 2L) ChapterKind.FILLER else ChapterKind.REGULAR,
            )
        }
        val downloaded = setOf(1L, 2L, 3L, 5L)
        val filters = EntryChapterFilters(entry, downloadedOnly = false)
        val isShown = { episode: EntryChapter -> filters.shows(episode) { episode.id in downloaded } }

        episodes.episodeWindow(entry, episodeId = 3L, isShown = isShown)!!.run {
            previous?.id shouldBe 1L
            next?.id shouldBe 5L
        }
        episodes.episodeWindow(entry, episodeId = 2L, isShown = isShown)!!.run {
            previous?.id shouldBe 1L
            next?.id shouldBe 3L
        }
    }
}
