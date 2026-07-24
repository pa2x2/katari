package eu.kanade.presentation.entry.components

import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.entry.EntryScreenModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import org.junit.jupiter.api.Test

class EntryPreviewLayoutTest {

    @Test
    fun `preview sizes use a spacing aware unified sizing model`() {
        previewGridColumnCount(PreviewSizeUi.SMALL, 320.dp) shouldBe 3
        previewGridColumnCount(PreviewSizeUi.MEDIUM, 320.dp) shouldBe 2
        previewGridColumnCount(PreviewSizeUi.LARGE, 320.dp) shouldBe 2
        previewGridColumnCount(PreviewSizeUi.EXTRA_LARGE, 320.dp) shouldBe 1
    }

    @Test
    fun `browse sheet width keeps medium and large visually distinct`() {
        val browseSheetContentWidth = 428.dp

        previewGridColumnCount(PreviewSizeUi.SMALL, browseSheetContentWidth) shouldBe 4
        previewGridColumnCount(PreviewSizeUi.MEDIUM, browseSheetContentWidth) shouldBe 3
        previewGridColumnCount(PreviewSizeUi.LARGE, browseSheetContentWidth) shouldBe 2
        previewGridColumnCount(PreviewSizeUi.EXTRA_LARGE, browseSheetContentWidth) shouldBe 1
    }

    @Test
    fun `extra large preview remains a focused mode on wider layouts`() {
        previewGridColumnCount(PreviewSizeUi.EXTRA_LARGE, 560.dp) shouldBe 2
        previewGridColumnCount(PreviewSizeUi.EXTRA_LARGE, 840.dp) shouldBe 3
    }

    @Test
    fun `preview state treats short chapters as loaded content`() {
        val previewState = EntryScreenModel.EntryPreviewState(
            chapterId = 1L,
            pages = listOf(
                EntryScreenModel.PreviewPage(previewPage(0)),
                EntryScreenModel.PreviewPage(previewPage(1)),
                EntryScreenModel.PreviewPage(previewPage(2)),
            ),
            pageCount = 5,
        )

        previewState.hasLoadedContent shouldBe true
    }

    @Test
    fun `entry level preview remains loaded without a child id`() {
        EntryScreenModel.EntryPreviewState(
            chapterId = null,
            pages = emptyList(),
            hasLoaded = true,
        ).hasLoadedContent shouldBe true
    }

    @Test
    fun `preview page only opens when both page and chapter are openable`() {
        previewPage(0).isOpenable(chapterId = 1L) shouldBe true
        previewPage(0).copy(canOpen = false).isOpenable(chapterId = 1L) shouldBe false
        previewPage(0).isOpenable(chapterId = null) shouldBe false
    }

    private fun previewPage(index: Int): EntryPreviewPage {
        return EntryPreviewPage(
            index = index,
            status = MutableStateFlow(EntryPreviewPageStatus.Ready),
            progress = MutableStateFlow(0),
            imageModel = Any(),
        )
    }
}
