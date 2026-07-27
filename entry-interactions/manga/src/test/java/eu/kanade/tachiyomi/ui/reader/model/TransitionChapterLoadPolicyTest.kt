package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TransitionChapterLoadPolicyTest {
    @Test
    fun `near-final and final chapter pages never request an adjacent chapter`() {
        val current = chapter(1L)
        val pages = listOf(ReaderPage(8), ReaderPage(9)).onEach { it.chapter = current }

        pages.forEach { page ->
            ReaderViewerItem.Page(page).automaticTransitionLoadDestination() shouldBe null
        }
    }

    @Test
    fun `active transition requests a waiting destination`() {
        val current = chapter(1L)
        val next = chapter(2L)
        val item = ReaderViewerItem.Transition(
            EntryChildWindow(current, null, next).nextTransition(),
        )

        item.automaticTransitionLoadDestination() shouldBe next
    }

    @Test
    fun `failed transition waits for explicit retry`() {
        val current = chapter(1L)
        val next = chapter(2L).apply {
            state = ReaderChapter.State.Error(IllegalStateException("Unavailable"))
        }
        val item = ReaderViewerItem.Transition(
            EntryChildWindow(current, null, next).nextTransition(),
        )

        item.automaticTransitionLoadDestination() shouldBe null
    }

    @Test
    fun `terminal transition has no destination to request`() {
        val current = chapter(1L)
        val item = ReaderViewerItem.Transition(
            EntryChildWindow(current, null, null).nextTransition(),
        )

        item.automaticTransitionLoadDestination() shouldBe null
    }

    @Test
    fun `non-scrollable final chapter requests its previous boundary instead of its centered terminal boundary`() {
        val previous = chapter(1L)
        val current = chapter(2L)
        val window = EntryChildWindow(current, previous, null)
        val previousTransition = ReaderViewerItem.Transition(window.previousTransition())
        val terminalTransition = ReaderViewerItem.Transition(window.nextTransition())

        automaticTransitionLoadItemAtAnchor(
            centeredItem = terminalTransition,
            firstVisibleItem = previousTransition,
            lastVisibleItem = terminalTransition,
            canScrollBackward = false,
            canScrollForward = false,
        ) shouldBe previousTransition
    }

    private fun chapter(id: Long) = ReaderChapter(
        Chapter.create().copy(
            id = id,
            mangaId = 9L,
            name = "Chapter $id",
            url = "/chapter/$id",
        ),
    )
}
