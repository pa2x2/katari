package eu.kanade.tachiyomi.ui.video.player.subtitle

import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Where the app draws text subtitles, in pixels of a player [playerWidth] by [playerHeight].
 *
 * Their size and drag offsets stay a share of the picture's size, as when they were always drawn over it. A fitted
 * picture leaves bands above and below it; when the band under it holds two lines, the subtitles move into it unless
 * the viewer keeps them in the picture. Whenever the controls show, the subtitles rise above their bottom row, which
 * lifts band subtitles back over the bottom of the picture until the row hides.
 *
 * Cues their file places go in the area [areaLeft], [areaTop], [areaWidth] by [areaHeight]: the fitted picture, or the
 * whole player once the picture fills or is zoomed past it. Every cue is moved by [shiftX] and [shiftY], the
 * appearance's offsets as shares of that area.
 */
internal data class VideoSubtitleLayout(
    val playerWidth: Float,
    val playerHeight: Float,
    val textSizePx: Float,
    /** From the player's bottom edge to the bottom of the lowest line of the cues the app places. */
    val bottomPx: Float,
    val areaLeft: Float,
    val areaTop: Float,
    val areaWidth: Float,
    val areaHeight: Float,
    val shiftX: Float,
    val shiftY: Float,
)

/**
 * Where the shown controls sit over the bottom of the player, in pixels up from its bottom edge: the top of their
 * bottom row and the bottom of the centre buttons, zero for what doesn't show.
 */
internal data class VideoControlsFootprint(val bottomRowTopPx: Float, val centreButtonsBottomPx: Float) {
    companion object {
        val None = VideoControlsFootprint(bottomRowTopPx = 0f, centreButtonsBottomPx = 0f)
    }
}

/** [gapPx] keeps the subtitles off the picture's edge and the controls. */
internal fun videoSubtitleLayout(
    playerWidth: Float,
    playerHeight: Float,
    aspectRatio: Float?,
    framing: PictureFraming,
    appearance: VideoSubtitleAppearance,
    keepInPicture: Boolean,
    controls: VideoControlsFootprint,
    gapPx: Float,
): VideoSubtitleLayout {
    val normalizedAppearance = appearance.normalized()
    val geometry = aspectRatio?.takeIf { playerWidth > 0f && playerHeight > 0f }
        ?.let { VideoPictureGeometry(playerWidth, playerHeight, it) }
    // Fill and zoom lay the subtitles over the whole player
    val fitted = framing == PictureFraming.Fit && geometry != null
    val baseWidth = if (fitted) geometry.fitWidth else playerWidth
    val baseHeight = if (fitted) geometry.fitHeight else playerHeight
    val textSizePx = normalizedAppearance.textSize * baseHeight
    val band = (playerHeight - baseHeight) / 2f
    val twoLinesPx = textSizePx * TWO_LINES_HEIGHT
    val inBand = fitted && !keepInPicture && band >= twoLinesPx + gapPx * 2f
    val restingBottom = if (inBand) {
        band - gapPx - twoLinesPx
    } else {
        band + baseHeight * DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION
    }
    val bottomPx = if (controls.bottomRowTopPx > 0f) {
        val aboveRow = controls.bottomRowTopPx + gapPx
        // A short window leaves no room for two lines between the row and the centre buttons, so the lines stop
        // under the buttons and overlap the row's top, which holds the time and chapter at its start and a skip
        // button at its end
        val underButtons = controls.centreButtonsBottomPx - gapPx - twoLinesPx
        val lifted = if (controls.centreButtonsBottomPx > 0f) minOf(aboveRow, underButtons) else aboveRow
        maxOf(restingBottom, lifted)
    } else {
        restingBottom
    }
    return VideoSubtitleLayout(
        playerWidth = playerWidth,
        playerHeight = playerHeight,
        textSizePx = textSizePx,
        bottomPx = bottomPx,
        areaLeft = (playerWidth - baseWidth) / 2f,
        areaTop = band,
        areaWidth = baseWidth,
        areaHeight = baseHeight,
        shiftX = baseWidth * normalizedAppearance.offsetX,
        shiftY = baseHeight * normalizedAppearance.offsetY,
    )
}

// Two lines of the subtitle font with the padding of the box behind them, in text sizes
private const val TWO_LINES_HEIGHT = 2.6f
