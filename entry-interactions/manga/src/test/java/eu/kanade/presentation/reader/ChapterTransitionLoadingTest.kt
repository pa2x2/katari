package eu.kanade.presentation.reader

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import org.junit.jupiter.api.Test
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionLoadState

class ChapterTransitionLoadingTest {

    @Test
    fun `hidden transitions fall back to the card whenever a spinner would never resolve`() {
        fun hidden(hasDestination: Boolean, state: ReaderEntryChildTransitionLoadState, chapterGap: Int) =
            rendersCompactTransitionLoading(ChapterTransitionMode.HIDDEN, hasDestination, state, chapterGap)

        hidden(true, ReaderEntryChildTransitionLoadState.Loading("Loading"), chapterGap = 0) shouldBe true
        hidden(true, ReaderEntryChildTransitionLoadState.Idle, chapterGap = -1) shouldBe true
        hidden(true, ReaderEntryChildTransitionLoadState.Failed("Unavailable"), chapterGap = 0) shouldBe false
        hidden(false, ReaderEntryChildTransitionLoadState.Idle, chapterGap = 0) shouldBe false
        hidden(true, ReaderEntryChildTransitionLoadState.Idle, chapterGap = 3) shouldBe false
    }
}
