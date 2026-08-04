package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderViewModelInitialStateTest {

    @Test
    fun `launch arguments select the requested chapter and explicit page`() {
        val state = initialState(
            "manga" to 10L,
            "chapter" to 20L,
            "page" to 4,
        )

        state.hasValidArgs shouldBe true
        state.mangaId shouldBe 10L
        state.chapterId shouldBe 20L
        state.pageIndex shouldBe 4
    }

    @Test
    fun `restored chapter and page take precedence over launch arguments`() {
        val state = initialState(
            "manga" to 10L,
            "chapter" to 20L,
            "page" to 4,
            "chapter_id" to 30L,
            "page_index" to 8,
        )

        state.hasValidArgs shouldBe true
        state.chapterId shouldBe 30L
        state.pageIndex shouldBe 8
    }

    @Test
    fun `missing launch identifiers are invalid even when restored position exists`() {
        val state = initialState(
            "chapter_id" to 30L,
            "page_index" to 8,
        )

        state.hasValidArgs shouldBe false
    }

    private fun initialState(vararg values: Pair<String, Any>): ReaderViewModel.InitialState {
        return ReaderViewModel.InitialState.from(SavedStateHandle(mapOf(*values)))
    }
}
