package mihon.translation.ui.presentation

import io.kotest.matchers.shouldBe
import mihon.translation.ui.session.TranslationSelectionAnchor
import org.junit.jupiter.api.Test

class TranslationSessionSurfaceTest {

    @Test
    fun `popup prefers below the selection and centers horizontally`() {
        calculate(
            anchor = TranslationSelectionAnchor(400f, 200f, 600f, 240f),
            popup = TranslationPopupSize(300, 200),
        ) shouldBe TranslationPopupPlacement(x = 350, y = 248)
    }

    @Test
    fun `popup falls back above when it cannot fit below`() {
        calculate(
            anchor = TranslationSelectionAnchor(400f, 780f, 600f, 820f),
            popup = TranslationPopupSize(300, 200),
        ) shouldBe TranslationPopupPlacement(x = 350, y = 572)
    }

    @Test
    fun `popup clamps horizontally inside safe viewport`() {
        calculate(
            anchor = TranslationSelectionAnchor(24f, 200f, 80f, 240f),
            popup = TranslationPopupSize(300, 200),
        ) shouldBe TranslationPopupPlacement(x = 16, y = 248)
    }

    @Test
    fun `popup promotes to sheet when measured content fits neither side`() {
        calculate(
            anchor = TranslationSelectionAnchor(400f, 420f, 600f, 460f),
            popup = TranslationPopupSize(400, 520),
        ) shouldBe null
    }

    @Test
    fun `popup promotes to sheet for invalid or unsafe anchors`() {
        calculate(
            anchor = TranslationSelectionAnchor(Float.NaN, 200f, 600f, 240f),
            popup = TranslationPopupSize(300, 200),
        ) shouldBe null

        calculate(
            anchor = TranslationSelectionAnchor(0f, 200f, 600f, 240f),
            popup = TranslationPopupSize(300, 200),
        ) shouldBe null
    }

    private fun calculate(
        anchor: TranslationSelectionAnchor,
        popup: TranslationPopupSize,
    ): TranslationPopupPlacement? {
        return calculateTranslationPopupPlacement(
            anchor = anchor,
            popup = popup,
            viewport = TranslationViewportBounds(0, 0, 1000, 1000),
            edgeMargin = 16,
            anchorGap = 8,
        )
    }
}
