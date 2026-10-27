package eu.kanade.tachiyomi.ui.video.player.subtitle

import eu.kanade.tachiyomi.ui.video.player.DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION
import eu.kanade.tachiyomi.ui.video.player.VideoSubtitleAppearance
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Where the player draws plain-text subtitles, in pixels of a player [playerWidth] by [playerHeight].
 *
 * Their size and drag offsets stay a share of the picture's size, as when they were always drawn over it. A fitted
 * picture leaves bands above and below it; when the band under it holds two lines, the subtitles move into it unless
 * the viewer keeps them in the picture. Whenever the controls' bottom row shows, the subtitles rise above it, which
 * lifts band subtitles back over the bottom of the picture until the row hides.
 */
internal data class VideoSubtitleLayout(
    val playerWidth: Float,
    val playerHeight: Float,
    /** The subtitles sit in the band, so the picture's view must cover the whole player to draw them there. */
    val inBand: Boolean,
    val textSizePx: Float,
    /** From the player's bottom edge to the bottom of the lowest line. */
    val bottomPx: Float,
    /** What the appearance's offsets are shares of: the picture's fitted size, or the player's when it covers it. */
    val offsetBaseWidth: Float,
    val offsetBaseHeight: Float,
)

/**
 * [bottomRowHeightPx] is how far the controls' bottom row reaches up from the player's bottom while it shows, otherwise
 * zero. [gapPx] keeps the subtitles off the picture's edge and the row.
 */
internal fun videoSubtitleLayout(
    playerWidth: Float,
    playerHeight: Float,
    aspectRatio: Float?,
    framing: PictureFraming,
    appearance: VideoSubtitleAppearance,
    keepInPicture: Boolean,
    bottomRowHeightPx: Float,
    gapPx: Float,
): VideoSubtitleLayout {
    val geometry = aspectRatio?.takeIf { playerWidth > 0f && playerHeight > 0f }
        ?.let { VideoPictureGeometry(playerWidth, playerHeight, it) }
    // Fill and zoom already lay the subtitles over the whole player, as Media3 does for them
    val fitted = framing == PictureFraming.Fit && geometry != null
    val baseWidth = if (fitted) geometry.fitWidth else playerWidth
    val baseHeight = if (fitted) geometry.fitHeight else playerHeight
    val textSizePx = appearance.normalized().textSize * baseHeight
    val band = (playerHeight - baseHeight) / 2f
    val twoLinesPx = textSizePx * TWO_LINES_HEIGHT
    val inBand = fitted && !keepInPicture && band >= twoLinesPx + gapPx * 2f
    val restingBottom = if (inBand) {
        band - gapPx - twoLinesPx
    } else {
        band + baseHeight * DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION
    }
    val bottomPx = if (bottomRowHeightPx > 0f) maxOf(restingBottom, bottomRowHeightPx + gapPx) else restingBottom
    return VideoSubtitleLayout(
        playerWidth = playerWidth,
        playerHeight = playerHeight,
        inBand = inBand,
        textSizePx = textSizePx,
        bottomPx = bottomPx,
        offsetBaseWidth = baseWidth,
        offsetBaseHeight = baseHeight,
    )
}

/**
 * The height of the view Media3 draws the subtitles in: the one that holds the picture, which either covers the
 * player or is the fitted picture.
 */
internal fun VideoSubtitleLayout.viewHeight(viewCoversPlayer: Boolean): Float =
    if (viewCoversPlayer) playerHeight else offsetBaseHeight

// Two lines of the subtitle font with the padding of the box behind them, in text sizes
private const val TWO_LINES_HEIGHT = 2.6f
