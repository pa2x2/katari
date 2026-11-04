package eu.kanade.tachiyomi.ui.video.player.picture

import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerCapabilities
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Shows the picture in a [PlayerView] as a framing asks, and again whenever the view's size changes, as on rotation or
 * in picture-in-picture.
 *
 * The output of a player that places the picture itself ([VideoPlayerCapabilities.placesPicture]) always covers the
 * whole player and the player scales and moves the picture in it, so changing the framing never resizes its surface:
 * mpv redraws a paused frame wrongly after a resize. For any other player Fit is the view's own fitting. Otherwise the
 * view's content covers the whole player, so the picture can reach past the fitted area; the player draws the frame
 * across the whole surface, which is scaled and moved back to the picture's shape.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class VideoPicturePlacer : View.OnLayoutChangeListener {
    private var view: PlayerView? = null
    private var framing: PictureFraming = PictureFraming.Fit
    private var aspectRatio: Float? = null
    private var player: VideoPlayer? = null

    fun attach(view: PlayerView) {
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
        val surface = view.videoSurfaceView
        val selfPlaced = player?.takeIf { it.capabilities.placesPicture }
        val geometry = aspectRatio
            ?.takeIf { view.width > 0 && view.height > 0 }
            ?.let { VideoPictureGeometry(view.width.toFloat(), view.height.toFloat(), it) }
        val coversPlayer = selfPlaced != null || (geometry != null && framing != PictureFraming.Fit)
        view.resizeMode = if (coversPlayer) {
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        // Without the video's shape yet, the picture is placed as fitted until its first frame says otherwise
        val placement = geometry?.place(framing) ?: VideoPicturePlacement(scale = 1f, offsetX = 0f, offsetY = 0f)
        val pictureWidth = (geometry?.fitWidth ?: view.width.toFloat()) * placement.scale
        val pictureHeight = (geometry?.fitHeight ?: view.height.toFloat()) * placement.scale
        when {
            selfPlaced != null -> {
                surface?.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
                selfPlaced.placePicture(
                    scale = placement.scale.toDouble(),
                    offsetX = share(placement.offsetX, pictureWidth).toDouble(),
                    offsetY = share(placement.offsetY, pictureHeight).toDouble(),
                )
            }
            coversPlayer -> surface?.place(
                scaleX = pictureWidth / view.width,
                scaleY = pictureHeight / view.height,
                offsetX = placement.offsetX,
                offsetY = placement.offsetY,
            )
            else -> surface?.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
        }
    }
}

private fun share(offset: Float, size: Float): Float = if (size > 0f) offset / size else 0f

private fun View.place(scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
    this.scaleX = scaleX
    this.scaleY = scaleY
    translationX = offsetX
    translationY = offsetY
}
