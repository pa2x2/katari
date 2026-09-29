package mihon.translation.ui.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.shouldBe
import mihon.translation.ui.session.TranslationSelectionAnchor
import org.junit.jupiter.api.Test

class TranslationSessionSurfaceTest {

    @Test
    fun `popup prefers below the selection, falls back above, and stays inside the safe viewport`() {
        val popup = TranslationPopupSize(300, 200)

        calculate(TranslationSelectionAnchor(400f, 200f, 600f, 240f), popup) shouldBe
            TranslationPopupPlacement(x = 350, y = 248)
        calculate(TranslationSelectionAnchor(400f, 780f, 600f, 820f), popup) shouldBe
            TranslationPopupPlacement(x = 350, y = 572)
        calculate(TranslationSelectionAnchor(24f, 200f, 80f, 240f), popup) shouldBe
            TranslationPopupPlacement(x = 16, y = 248)
    }

    @Test
    fun `popup promotes to a sheet when it fits neither side or the anchor is invalid or unsafe`() {
        calculate(TranslationSelectionAnchor(400f, 420f, 600f, 460f), TranslationPopupSize(400, 520)) shouldBe null
        calculate(TranslationSelectionAnchor(100f, 100f, 900f, 900f), TranslationPopupSize(300, 200)) shouldBe null
        calculate(TranslationSelectionAnchor(Float.NaN, 200f, 600f, 240f), TranslationPopupSize(300, 200)) shouldBe
            null
        calculate(TranslationSelectionAnchor(0f, 200f, 600f, 240f), TranslationPopupSize(300, 200)) shouldBe null
    }

    @Test
    fun `unplaceable remeasurement stays attached to the selection during the sheet transition`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val below = provider(
            anchor = TranslationSelectionAnchor(400f, 420f, 600f, 460f),
            onPlacementAvailabilityChanged = { availability = it },
        )
        below.position(IntSize(300, 200)) shouldBe IntOffset(400, 538)
        availability shouldBe TranslationPopupPlacementAvailability.Fits
        below.position(IntSize(400, 700)) shouldBe IntOffset(350, 538)
        availability shouldBe TranslationPopupPlacementAvailability.NeedsSheet

        val above = provider(
            anchor = TranslationSelectionAnchor(400f, 780f, 600f, 820f),
            onPlacementAvailabilityChanged = { availability = it },
        )
        above.position(IntSize(300, 200)) shouldBe IntOffset(400, 642)
        availability shouldBe TranslationPopupPlacementAvailability.Fits
        above.position(IntSize(400, 900)) shouldBe IntOffset(350, -58)
        availability shouldBe TranslationPopupPlacementAvailability.NeedsSheet
    }

    @Test
    fun `an offscreen anchor is distinguished from sheet fallback and hidden from toolbar avoidance`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val reported = mutableListOf<Rect?>()
        val provider = provider(
            anchor = TranslationSelectionAnchor(400f, -100f, 600f, -60f),
            onPlacementAvailabilityChanged = { availability = it },
            onPopupBoundsChanged = { reported.add(it) },
        )

        provider.position(IntSize(300, 200))

        availability shouldBe TranslationPopupPlacementAvailability.AnchorOutsideViewport
        reported shouldBe listOf(null)
    }

    /** A provider for a 1000×1000 reader root placed at (50, 70) inside a 1200×1300 window. */
    private fun provider(
        anchor: TranslationSelectionAnchor,
        onPlacementAvailabilityChanged: (TranslationPopupPlacementAvailability) -> Unit,
        onPopupBoundsChanged: (Rect?) -> Unit = {},
    ) = TranslationPopupPositionProvider(
        anchor = anchor,
        hostSize = IntSize(1000, 1000),
        windowInsets = TranslationWindowInsets(0, 0, 0, 0),
        edgeMargin = 16,
        anchorGap = 8,
        onPlacementAvailabilityChanged = onPlacementAvailabilityChanged,
        onPopupBoundsChanged = onPopupBoundsChanged,
    )

    private fun TranslationPopupPositionProvider.position(content: IntSize) = calculatePosition(
        anchorBounds = IntRect(50, 70, 1050, 1070),
        windowSize = IntSize(1200, 1300),
        layoutDirection = LayoutDirection.Ltr,
        popupContentSize = content,
    )

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
