package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.matchers.shouldBe
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class TransitionChapterLoadPolicyTest {
    @Test
    fun `a waiting destination is requested automatically but a failed one waits for an explicit retry`() {
        val current = chapter(1L)
        val next = chapter(2L)
        val transition = ReaderViewerItem.Transition(EntryChildWindow(current, null, next).nextTransition())

        transition.automaticTransitionLoadDestination() shouldBe next

        next.state = ReaderChapter.State.Error(IllegalStateException("Unavailable"))

        transition.automaticTransitionLoadDestination() shouldBe null
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
