package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderViewModelInitialStateTest {

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

    private fun initialState(vararg values: Pair<String, Any>): ReaderViewModel.InitialState {
        return ReaderViewModel.InitialState.from(SavedStateHandle(mapOf(*values)))
    }
}
