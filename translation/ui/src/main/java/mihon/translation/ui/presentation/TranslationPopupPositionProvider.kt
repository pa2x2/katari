package mihon.translation.ui.presentation

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import mihon.translation.ui.session.TranslationSelectionAnchor
import kotlin.math.roundToInt

internal data class TranslationWindowInsets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal enum class TranslationPopupPlacementAvailability {
    Fits,
    AnchorOutsideViewport,
    NeedsSheet,
}

internal class TranslationPopupPositionProvider(
    private val anchor: TranslationSelectionAnchor,
    private val hostSize: IntSize,
    private val windowInsets: TranslationWindowInsets,
    private val edgeMargin: Int,
    private val anchorGap: Int,
    private val onPlacementAvailabilityChanged: (TranslationPopupPlacementAvailability) -> Unit,
    private val onPopupBoundsChanged: (Rect?) -> Unit = {},
) : PopupPositionProvider {
    private var lastPlacementSide: TranslationPopupAnchorSide? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val rootLeft = anchorBounds.left
        val rootTop = anchorBounds.top
        val viewportLeft = (windowInsets.left - rootLeft).coerceAtLeast(0)
        val viewportTop = (windowInsets.top - rootTop).coerceAtLeast(0)
        val viewportRight = minOf(
            hostSize.width,
            windowSize.width - rootLeft - windowInsets.right,
        )
        val viewportBottom = minOf(
            hostSize.height,
            windowSize.height - rootTop - windowInsets.bottom,
        )
        val viewport = if (viewportRight > viewportLeft && viewportBottom > viewportTop) {
            TranslationViewportBounds(
                left = viewportLeft,
                top = viewportTop,
                right = viewportRight,
                bottom = viewportBottom,
            )
        } else {
            null
        }
        val placement = if (viewport != null) {
            calculateTranslationPopupPlacement(
                anchor = anchor,
                popup = TranslationPopupSize(popupContentSize.width, popupContentSize.height),
                viewport = viewport,
                edgeMargin = edgeMargin,
                anchorGap = anchorGap,
            )
        } else {
            null
        }
        val availability = when {
            placement != null -> TranslationPopupPlacementAvailability.Fits
            viewport != null && anchor.isUsable() &&
                !anchor.isInside(viewport, edgeMargin = 0) ->
                TranslationPopupPlacementAvailability.AnchorOutsideViewport
            else -> TranslationPopupPlacementAvailability.NeedsSheet
        }
        placement?.let {
            lastPlacementSide = if (it.y >= anchor.bottom.roundToInt()) {
                TranslationPopupAnchorSide.Below
            } else {
                TranslationPopupAnchorSide.Above
            }
        }
        val resolvedPosition = placement?.let {
            IntOffset(rootLeft + it.x, rootTop + it.y)
        } ?: calculateTranslationPopupFallbackPosition(
            anchor = anchor,
            popupContentSize = popupContentSize,
            viewportLeft = viewportLeft,
            viewportTop = viewportTop,
            viewportRight = viewportRight,
            viewportBottom = viewportBottom,
            rootLeft = rootLeft,
            rootTop = rootTop,
            edgeMargin = edgeMargin,
            anchorGap = anchorGap,
            lastPlacementSide = lastPlacementSide,
        )
        onPlacementAvailabilityChanged(availability)
        onPopupBoundsChanged(
            resolvedPosition.takeIf {
                availability == TranslationPopupPlacementAvailability.Fits
            }?.let {
                Rect(
                    left = it.x.toFloat(),
                    top = it.y.toFloat(),
                    right = (it.x + popupContentSize.width).toFloat(),
                    bottom = (it.y + popupContentSize.height).toFloat(),
                )
            },
        )
        return resolvedPosition
    }
}

private fun calculateTranslationPopupFallbackPosition(
    anchor: TranslationSelectionAnchor,
    popupContentSize: IntSize,
    viewportLeft: Int,
    viewportTop: Int,
    viewportRight: Int,
    viewportBottom: Int,
    rootLeft: Int,
    rootTop: Int,
    edgeMargin: Int,
    anchorGap: Int,
    lastPlacementSide: TranslationPopupAnchorSide?,
): IntOffset {
    val safeLeft = (viewportLeft + edgeMargin).coerceAtMost(viewportRight)
    val safeTop = (viewportTop + edgeMargin).coerceAtMost(viewportBottom)
    val safeRight = (viewportRight - edgeMargin).coerceAtLeast(safeLeft)
    val safeBottom = (viewportBottom - edgeMargin).coerceAtLeast(safeTop)
    val maximumX = (safeRight - popupContentSize.width).coerceAtLeast(safeLeft)
    val anchorCenterX = ((anchor.left + anchor.right) / 2f)
        .takeIf(Float::isFinite)
        ?.roundToInt()
        ?: safeLeft
    val preferredX = anchorCenterX - popupContentSize.width / 2
    val fallbackSide = lastPlacementSide ?: anchor.takeIf { it.isUsable() }?.let {
        val availableAbove = it.top - anchorGap - safeTop
        val availableBelow = safeBottom - anchorGap - it.bottom
        if (availableBelow >= availableAbove) {
            TranslationPopupAnchorSide.Below
        } else {
            TranslationPopupAnchorSide.Above
        }
    }
    // Compose applies the returned WindowManager position before it can recompose the popup into
    // its sheet. Keep an unavailable remeasurement attached to the selection so that transient
    // content sizes cannot expose a viewport-clamped jump or cover the selected text.
    val preferredY = when (fallbackSide) {
        TranslationPopupAnchorSide.Below -> anchor.bottom.roundToInt() + anchorGap
        TranslationPopupAnchorSide.Above -> anchor.top.roundToInt() - anchorGap - popupContentSize.height
        null -> safeTop
    }
    return IntOffset(
        x = rootLeft + preferredX.coerceIn(safeLeft, maximumX),
        y = rootTop + preferredY,
    )
}

private enum class TranslationPopupAnchorSide {
    Above,
    Below,
}
