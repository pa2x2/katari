package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.geometry.Rect
import tachiyomi.domain.entry.model.SubtitlePosition
import kotlin.math.abs

/** A line the subtitle editor lines dragged subtitles up with: the picture's centre, or where a row's stack rests. */
internal enum class SubtitleGuide(val row: SubtitlePosition.Row?) {
    CENTER(null),
    TOP(SubtitlePosition.Row.TOP),
    MIDDLE(SubtitlePosition.Row.MIDDLE),
    USUAL(SubtitlePosition.Row.BOTTOM),
}

/** Subtitles moved in the editor: their [appearance], and the [reached] guides they rest on. */
internal data class SubtitleMove(val appearance: VideoSubtitleAppearance, val reached: Set<SubtitleGuide>)

/** Where [guide] is drawn: across the picture from its left edge for [SubtitleGuide.CENTER], down it otherwise. */
internal fun VideoSubtitleLayout.guideAt(guide: SubtitleGuide): Float = when (guide.row) {
    null -> areaCentreX
    SubtitlePosition.Row.TOP -> restTop
    SubtitlePosition.Row.MIDDLE -> restMiddle
    SubtitlePosition.Row.BOTTOM -> restBottom
}

/**
 * The appearance that draws subtitles now drawn at [from] at [to] instead, held inside the player. When [from] bounds
 * the cues the app places ([stacked]), they rest on a guide in its row or column where [to] comes within [snapPx] of
 * it, so a snap keeps them in their cell on any screen; cues the file places all move by the offsets alone.
 */
internal fun VideoSubtitleLayout.moveSubtitles(
    appearance: VideoSubtitleAppearance,
    from: Rect,
    to: Rect,
    stacked: Boolean,
    snapPx: Float,
): SubtitleMove {
    val target = held(to)
    if (!stacked) {
        val moved = appearance.copy(
            offsetX = appearance.offsetX + (target.left - from.left) / areaWidth,
            offsetY = appearance.offsetY + (target.top - from.top) / areaHeight,
        )
        return SubtitleMove(moved.normalized(), emptySet())
    }
    val rowGuide = listOf(
        SubtitleGuide.TOP to target.top,
        SubtitleGuide.MIDDLE to target.center.y,
        SubtitleGuide.USUAL to target.bottom,
    )
        .map { (guide, edge) -> guide to abs(edge - guideAt(guide)) }
        .filter { (_, distance) -> distance <= snapPx }
        .minByOrNull { (_, distance) -> distance }
        ?.first
    val centred = abs(target.center.x - guideAt(SubtitleGuide.CENTER)) <= snapPx
    val position = SubtitlePosition.of(
        row = rowGuide?.row ?: appearance.position.row,
        column = if (centred) SubtitlePosition.Column.CENTER else appearance.position.column,
    )
    val moved = appearance.copy(
        position = position,
        offsetX = if (centred) 0f else (target.left - restingLeft(position.column, target.width)) / areaWidth,
        offsetY = if (rowGuide != null) 0f else (target.top - restingTop(position.row, target.height)) / areaHeight,
    )
    return SubtitleMove(moved.normalized(), setOfNotNull(rowGuide, SubtitleGuide.CENTER.takeIf { centred }))
}
