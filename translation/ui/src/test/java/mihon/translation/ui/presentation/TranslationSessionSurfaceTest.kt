package mihon.translation.ui.presentation

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.shouldBe
import mihon.translation.api.request.TranslationRequest
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
import org.junit.jupiter.api.Test

class TranslationSessionSurfaceTest {

    @Test
    fun `settling request is visible on the surface appropriate for its anchor`() {
        TranslationSessionState.Settling(
            TranslationSessionInput(
                request = TranslationRequest("selected text"),
                anchor = TranslationSelectionAnchor(400f, 200f, 600f, 240f),
            ),
        ).preferredSurface() shouldBe TranslationSessionSurface.AnchoredPopup

        TranslationSessionState.Settling(
            TranslationSessionInput(request = TranslationRequest("settings text")),
        ).preferredSurface() shouldBe TranslationSessionSurface.AdaptiveSheet
    }

    @Test
    fun `translation failure follows its anchor instead of forcing a sheet`() {
        TranslationSessionState.Failed(
            input = TranslationSessionInput(
                request = TranslationRequest("selected text"),
                anchor = TranslationSelectionAnchor(400f, 200f, 600f, 240f),
            ),
            failure = TranslationSessionFailure.UnexpectedExecutionFailure,
        ).preferredSurface() shouldBe TranslationSessionSurface.AnchoredPopup

        TranslationSessionState.Failed(
            input = TranslationSessionInput(request = TranslationRequest("settings text")),
            failure = TranslationSessionFailure.UnexpectedExecutionFailure,
        ).preferredSurface() shouldBe TranslationSessionSurface.AdaptiveSheet
    }

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
    fun `page-spanning selection promotes to sheet when popup fits neither side`() {
        calculate(
            anchor = TranslationSelectionAnchor(100f, 100f, 900f, 900f),
            popup = TranslationPopupSize(300, 200),
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

    @Test
    fun `platform popup placement preserves reader-root coordinates inside its window`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val provider = TranslationPopupPositionProvider(
            anchor = TranslationSelectionAnchor(400f, 200f, 600f, 240f),
            hostSize = IntSize(1000, 1000),
            windowInsets = TranslationWindowInsets(0, 0, 0, 0),
            edgeMargin = 16,
            anchorGap = 8,
            onPlacementAvailabilityChanged = { availability = it },
        )

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(300, 200),
        ) shouldBe IntOffset(400, 318)
        availability shouldBe TranslationPopupPlacementAvailability.Fits
    }

    @Test
    fun `unplaceable remeasurement remains attached below the selection during sheet transition`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val provider = TranslationPopupPositionProvider(
            anchor = TranslationSelectionAnchor(400f, 420f, 600f, 460f),
            hostSize = IntSize(1000, 1000),
            windowInsets = TranslationWindowInsets(0, 0, 0, 0),
            edgeMargin = 16,
            anchorGap = 8,
            onPlacementAvailabilityChanged = { availability = it },
        )

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(300, 200),
        ) shouldBe IntOffset(400, 538)
        availability shouldBe TranslationPopupPlacementAvailability.Fits

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(400, 700),
        ) shouldBe IntOffset(350, 538)
        availability shouldBe TranslationPopupPlacementAvailability.NeedsSheet
    }

    @Test
    fun `unplaceable remeasurement remains attached above the selection during sheet transition`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val provider = TranslationPopupPositionProvider(
            anchor = TranslationSelectionAnchor(400f, 780f, 600f, 820f),
            hostSize = IntSize(1000, 1000),
            windowInsets = TranslationWindowInsets(0, 0, 0, 0),
            edgeMargin = 16,
            anchorGap = 8,
            onPlacementAvailabilityChanged = { availability = it },
        )

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(300, 200),
        ) shouldBe IntOffset(400, 642)
        availability shouldBe TranslationPopupPlacementAvailability.Fits

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(400, 900),
        ) shouldBe IntOffset(350, -58)
        availability shouldBe TranslationPopupPlacementAvailability.NeedsSheet
    }

    @Test
    fun `platform popup distinguishes an offscreen anchor from sheet fallback`() {
        var availability: TranslationPopupPlacementAvailability? = null
        val provider = TranslationPopupPositionProvider(
            anchor = TranslationSelectionAnchor(400f, -100f, 600f, -60f),
            hostSize = IntSize(1000, 1000),
            windowInsets = TranslationWindowInsets(0, 0, 0, 0),
            edgeMargin = 16,
            anchorGap = 8,
            onPlacementAvailabilityChanged = { availability = it },
        )

        provider.calculatePosition(
            anchorBounds = IntRect(50, 70, 1050, 1070),
            windowSize = IntSize(1200, 1300),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(300, 200),
        )

        availability shouldBe TranslationPopupPlacementAvailability.AnchorOutsideViewport
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
