package eu.kanade.tachiyomi.ui.video.player.picture

import android.view.View
import app.katari.video.engine.spi.player.VideoPlayer
import app.katari.video.engine.spi.player.VideoPlayerCapabilities
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurfaceView
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Shows the picture in a [VideoSurfaceView] as a framing asks, and again whenever the view's size changes, as on
 * rotation or in picture-in-picture. The surface always covers the whole view, so changing the framing never resizes
 * it.
 *
 * A player that places the picture itself ([VideoPlayerCapabilities.placesPicture]) scales and moves it inside its
 * untouched surface: mpv redraws a paused frame wrongly after its surface is resized, and what it draws over the
 * picture, such as its own subtitles, then stays on screen however far the picture is zoomed. Any other player draws
 * the frame across the whole surface, which is scaled and moved to the picture's shape and place.
 */
internal class VideoPicturePlacer : View.OnLayoutChangeListener {
    private var view: VideoSurfaceView? = null
    private var framing: PictureFraming = PictureFraming.Fit
    private var aspectRatio: Float? = null
    private var player: VideoPlayer? = null

    fun attach(view: VideoSurfaceView) {
        this.view?.removeOnLayoutChangeListener(this)
        this.view = view
        view.addOnLayoutChangeListener(this)
    }

    fun show(framing: PictureFraming, aspectRatio: Float?, player: VideoPlayer) {
        if (framing == this.framing && aspectRatio == this.aspectRatio && player === this.player) return
        this.framing = framing
        this.aspectRatio = aspectRatio
        this.player = player
        apply()
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int,
    ) {
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) apply()
    }

    private fun apply() {
        val view = view ?: return
        val surface = view.surfaceView
        val selfPlaced = player?.takeIf { it.capabilities.placesPicture }
        if (selfPlaced != null) {
            surface.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
            placeSelfPlacedPicture(selfPlaced, view.width.toFloat(), view.height.toFloat(), framing, aspectRatio)
            return
        }
        val geometry = aspectRatio
            ?.takeIf { view.width > 0 && view.height > 0 }
            ?.let { VideoPictureGeometry(view.width.toFloat(), view.height.toFloat(), it) }
        val placement = geometry?.place(framing) ?: VideoPicturePlacement(scale = 1f, offsetX = 0f, offsetY = 0f)
        val pictureWidth = (geometry?.fitWidth ?: view.width.toFloat()) * placement.scale
        val pictureHeight = (geometry?.fitHeight ?: view.height.toFloat()) * placement.scale
        when {
            geometry != null -> surface.place(
                scaleX = pictureWidth / view.width,
                scaleY = pictureHeight / view.height,
                offsetX = placement.offsetX,
                offsetY = placement.offsetY,
            )
            else -> surface.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
        }
    }
}

private fun View.place(scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
    this.scaleX = scaleX
    this.scaleY = scaleY
    translationX = offsetX
    translationY = offsetY
}
