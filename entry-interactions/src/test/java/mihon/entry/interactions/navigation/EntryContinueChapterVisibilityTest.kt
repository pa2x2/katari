package mihon.entry.interactions.navigation

import eu.kanade.tachiyomi.source.entry.ChapterKind
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.library.service.LibraryPreferences

class EntryContinueChapterVisibilityTest {

    @Test
    fun `Continue never offers a chapter the entry's filters hide, even one with saved progress`() {
        val entry = Entry.create().copy(id = 1, chapterFlags = Entry.CHAPTER_SHOW_NOT_FILLER)
        val episodes = (1L..6L).map { number ->
            EntryChapter.create().copy(
                id = number,
                entryId = entry.id,
                read = number <= 3,
                kind = if (number in 4L..5L) ChapterKind.FILLER else ChapterKind.REGULAR,
            )
        }
        val fillerProgress = EntryProgressState(
            entryId = entry.id,
            chapterId = 4,
            resourceKey = "video",
            locator = EntryProgressLocator(kind = "position", position = 60_000),
        )
        // A strict mock: with no Downloaded filter on, Continue must not look up downloads at all.
        val visibility = EntryContinueChapterVisibility(
            libraryPreferences = LibraryPreferences(InMemoryPreferenceStore()),
            downloadRuntime = mockk<EntryDownloadRuntimeFeature>(),
        )

        val shown = visibility.shown(
            listOwner = entry,
            owners = listOf(entry),
            input = EntryContinuePreparedInput(episodes, listOf(fillerProgress)),
        )

        shown.chapters.map(EntryChapter::id) shouldContainExactly listOf(1L, 2L, 3L, 6L)
        shown.progressStates shouldBe emptyList()
    }
}
