package mihon.entry.viewer.chrome.ui.tap

/** Normalized tap regions shared by paged readers and their navigation overlays. */
enum class ReaderTapAction { MENU, PREV, NEXT, LEFT, RIGHT }

data class ReaderTapRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val action: ReaderTapAction,
)

fun readerTapRegions(mode: Int, vertical: Boolean = false): List<ReaderTapRegion> = when (mode) {
    0 -> readerTapRegions(if (vertical) 1 else 4)
    1 -> listOf(
        ReaderTapRegion(0f, .33f, .33f, .66f, ReaderTapAction.PREV),
        ReaderTapRegion(0f, 0f, 1f, .33f, ReaderTapAction.PREV),
        ReaderTapRegion(.66f, .33f, 1f, .66f, ReaderTapAction.NEXT),
        ReaderTapRegion(0f, .66f, 1f, 1f, ReaderTapAction.NEXT),
    )
    2 -> listOf(
        ReaderTapRegion(.33f, .33f, 1f, 1f, ReaderTapAction.NEXT),
        ReaderTapRegion(0f, .33f, .33f, 1f, ReaderTapAction.PREV),
    )
    3 -> listOf(
        ReaderTapRegion(0f, 0f, .33f, 1f, ReaderTapAction.NEXT),
        ReaderTapRegion(.33f, .66f, .66f, 1f, ReaderTapAction.PREV),
        ReaderTapRegion(.66f, 0f, 1f, 1f, ReaderTapAction.NEXT),
    )
    4 -> listOf(
        ReaderTapRegion(0f, 0f, .33f, 1f, ReaderTapAction.LEFT),
        ReaderTapRegion(.66f, 0f, 1f, 1f, ReaderTapAction.RIGHT),
    )
    else -> emptyList()
}

/**
 * The regions of [mode] mirrored as [inversion] says: 1 flips them left to right, 2 top to bottom, 3 both. Taps and
 * the overlay that shows the regions both read these, so what is shown is what a tap does.
 */
fun readerTapRegions(mode: Int, vertical: Boolean, inversion: Int): List<ReaderTapRegion> =
    readerTapRegions(mode, vertical).map { region ->
        region.copy(
            left = if (inversion == 1 || inversion == 3) 1f - region.right else region.left,
            right = if (inversion == 1 || inversion == 3) 1f - region.left else region.right,
            top = if (inversion == 2 || inversion == 3) 1f - region.bottom else region.top,
            bottom = if (inversion == 2 || inversion == 3) 1f - region.top else region.bottom,
        )
    }

fun readerTapAction(mode: Int, vertical: Boolean, inversion: Int, x: Float, y: Float): ReaderTapAction =
    readerTapRegions(mode, vertical, inversion)
        .firstOrNull { x >= it.left && x < it.right && y >= it.top && y < it.bottom }
        ?.action
        ?: ReaderTapAction.MENU
