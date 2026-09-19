package eu.kanade.presentation.reader

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import org.junit.jupiter.api.Test
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState

class ChapterTransitionLoadingTest {

    @Test
    fun `hidden mode renders compact loading while the contiguous destination waits to load`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = true,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Idle,
            chapterGap = 0,
        ) shouldBe true
    }

    @Test
    fun `hidden mode keeps rendering compact loading while the destination loads`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = true,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Loading("Loading"),
            chapterGap = 0,
        ) shouldBe true
    }

    @Test
    fun `hidden mode falls back to the card when the destination failed so the reader offers a retry`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = true,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Failed("Unavailable"),
            chapterGap = 0,
        ) shouldBe false
    }

    @Test
    fun `hidden mode falls back to the card at end of content instead of spinning forever`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = false,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Idle,
            chapterGap = 0,
        ) shouldBe false
    }

    @Test
    fun `hidden mode falls back to the card when chapters are missing between the boundary`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = true,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Idle,
            chapterGap = 3,
        ) shouldBe false
    }

    @Test
    fun `hidden mode renders compact loading across duplicate chapter numbers`() {
        rendersCompactTransitionLoading(
            displayMode = ChapterTransitionMode.HIDDEN,
            hasDestination = true,
            destinationLoadState = ReaderEntryChildTransitionLoadState.Idle,
            chapterGap = -1,
        ) shouldBe true
    }

    @Test
    fun `non-hidden modes always render the chapter card`() {
        listOf(ChapterTransitionMode.ALWAYS, ChapterTransitionMode.WHEN_NEEDED).forEach { mode ->
            rendersCompactTransitionLoading(
                displayMode = mode,
                hasDestination = true,
                destinationLoadState = ReaderEntryChildTransitionLoadState.Idle,
                chapterGap = 0,
            ) shouldBe false
        }
    }
}
