package eu.kanade.presentation.components

import eu.kanade.presentation.entry.DownloadAction
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class DownloadDropdownMenuTest {

    @Test
    fun `menu includes bookmarked action only when selected by its feature`() {
        downloadActions(bookmarkedDownloadsSupported = false).shouldContainExactly(baseActions)
        downloadActions(bookmarkedDownloadsSupported = true)
            .shouldContainExactly(baseActions + DownloadAction.BOOKMARKED_CHAPTERS)
    }

    private companion object {
        val baseActions = listOf(
            DownloadAction.NEXT_1_CHAPTER,
            DownloadAction.NEXT_5_CHAPTERS,
            DownloadAction.NEXT_10_CHAPTERS,
            DownloadAction.NEXT_25_CHAPTERS,
            DownloadAction.UNREAD_CHAPTERS,
        )
    }
}
