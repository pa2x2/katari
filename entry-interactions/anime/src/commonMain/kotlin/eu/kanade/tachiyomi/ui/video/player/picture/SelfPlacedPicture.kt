package eu.kanade.tachiyomi.ui.video.player.picture

import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerCapabilities
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Has [player], which places the picture itself ([VideoPlayerCapabilities.placesPicture]), show it as [framing] asks in
 * an output of [width] by [height] pixels, for a video of [aspectRatio]. Without the video's shape yet, the picture
 * fills the output until its first frame says otherwise.
 */
internal fun placeSelfPlacedPicture(
    player: VideoPlayer,
    width: Float,
    height: Float,
    framing: PictureFraming,
    aspectRatio: Float?,
) {
    val geometry = aspectRatio
        ?.takeIf { width > 0f && height > 0f }
        ?.let { VideoPictureGeometry(width, height, it) }
    val placement = geometry?.place(framing) ?: VideoPicturePlacement(scale = 1f, offsetX = 0f, offsetY = 0f)
    val pictureWidth = (geometry?.fitWidth ?: width) * placement.scale
    val pictureHeight = (geometry?.fitHeight ?: height) * placement.scale
    player.placePicture(
        scale = placement.scale.toDouble(),
        offsetX = share(placement.offsetX, pictureWidth).toDouble(),
        offsetY = share(placement.offsetY, pictureHeight).toDouble(),
    )
}

private fun share(offset: Float, size: Float): Float = if (size > 0f) offset / size else 0f
