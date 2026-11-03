package mihon.translation.ui.presentation

import androidx.compose.ui.unit.IntRect
import io.kotest.matchers.shouldBe
import mihon.translation.ui.session.TranslationSelectionAnchor
import org.junit.jupiter.api.Test

class TranslationPopupPlacementTest {
    @Test
    fun `a popup that would cover the reader's toolbar below the selection goes above it`() {
        val placement = calculateTranslationPopupPlacement(
            anchor = TranslationSelectionAnchor(left = 740f, top = 1588f, right = 1015f, bottom = 1718f),
            popup = TranslationPopupSize(width = 940, height = 433),
            viewport = TranslationViewportBounds(left = 0, top = 0, right = 1080, bottom = 2424),
            edgeMargin = 42,
            anchorGap = 126,
            avoid = IntRect(left = 42, top = 2260, right = 1038, bottom = 2380),
        )

        placement shouldBe TranslationPopupPlacement(x = 98, y = 1029)
    }
}
