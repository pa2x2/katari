package mihon.translation.ui.presentation

import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionState
import kotlin.math.roundToInt

internal enum class TranslationSessionSurface {
    None,
    AnchoredPopup,
    AdaptiveSheet,
}

internal fun TranslationSessionState.preferredSurface(): TranslationSessionSurface {
    return when (this) {
        TranslationSessionState.Hidden -> TranslationSessionSurface.None
        is TranslationSessionState.Active -> if (input.anchor == null) {
            TranslationSessionSurface.AdaptiveSheet
        } else {
            TranslationSessionSurface.AnchoredPopup
        }
    }
}

internal data class TranslationViewportBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right > left)
        require(bottom > top)
    }
}

internal data class TranslationPopupSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0)
        require(height > 0)
    }
}

internal data class TranslationPopupPlacement(
    val x: Int,
    val y: Int,
)

internal fun calculateTranslationPopupPlacement(
    anchor: TranslationSelectionAnchor,
    popup: TranslationPopupSize,
    viewport: TranslationViewportBounds,
    edgeMargin: Int,
    anchorGap: Int,
): TranslationPopupPlacement? {
    require(edgeMargin >= 0)
    require(anchorGap >= 0)

    if (!anchor.isUsable()) return null
    val safeLeft = viewport.left + edgeMargin
    val safeTop = viewport.top + edgeMargin
    val safeRight = viewport.right - edgeMargin
    val safeBottom = viewport.bottom - edgeMargin
    if (safeRight <= safeLeft || safeBottom <= safeTop) return null
    if (
        anchor.left < safeLeft ||
        anchor.top < safeTop ||
        anchor.right > safeRight ||
        anchor.bottom > safeBottom
    ) {
        return null
    }

    val safeWidth = safeRight - safeLeft
    if (popup.width > safeWidth) return null

    val belowY = anchor.bottom.roundToInt() + anchorGap
    val aboveY = anchor.top.roundToInt() - anchorGap - popup.height
    val y = when {
        belowY + popup.height <= safeBottom -> belowY
        aboveY >= safeTop -> aboveY
        else -> return null
    }
    val centeredX = ((anchor.left + anchor.right) / 2f).roundToInt() - popup.width / 2
    val x = centeredX.coerceIn(safeLeft, safeRight - popup.width)
    return TranslationPopupPlacement(x, y)
}

internal fun TranslationSelectionAnchor.isUsable(): Boolean {
    return left.isFinite() &&
        top.isFinite() &&
        right.isFinite() &&
        bottom.isFinite() &&
        right > left &&
        bottom > top
}

internal fun TranslationSelectionAnchor.isInside(
    viewport: TranslationViewportBounds,
    edgeMargin: Int,
): Boolean {
    val safeLeft = viewport.left + edgeMargin
    val safeTop = viewport.top + edgeMargin
    val safeRight = viewport.right - edgeMargin
    val safeBottom = viewport.bottom - edgeMargin
    return left >= safeLeft &&
        top >= safeTop &&
        right <= safeRight &&
        bottom <= safeBottom
}
