package eu.kanade.tachiyomi.ui.video.player.picture

import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import mihon.video.engine.spi.picture.SelfPlacedPicture
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Shows the picture in a [PlayerView] as a framing asks, and again whenever the view's size changes, as on rotation or
 * in picture-in-picture.
 *
 * Fit is the view's own fitting. Any other framing has the view's content cover the whole player, so the picture can
 * reach past the fitted area, and the subtitles the view draws stay within the player. A [SelfPlacedPicture] player
 * then scales and moves the picture itself; any other draws the frame across the whole surface, which is scaled and
 * moved back to the picture's shape.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class VideoPicturePlacer : View.OnLayoutChangeListener {
    private var view: PlayerView? = null
    private var framing: PictureFraming = PictureFraming.Fit
    private var aspectRatio: Float? = null
    private var player: Player? = null

    fun attach(view: PlayerView) {
        this.view?.removeOnLayoutChangeListener(this)
        this.view = view
        view.addOnLayoutChangeListener(this)
    }

    /** [player] is the engine's own player, which may place the picture itself, not a wrapper around it. */
    fun show(framing: PictureFraming, aspectRatio: Float?, player: Player) {
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
        val selfPlaced = player as? SelfPlacedPicture
        val geometry = aspectRatio
            ?.takeIf { view.width > 0 && view.height > 0 }
            ?.let { VideoPictureGeometry(view.width.toFloat(), view.height.toFloat(), it) }
        if (framing == PictureFraming.Fit || geometry == null) {
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            surface?.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
            selfPlaced?.placePicture(scale = 1.0, offsetX = 0.0, offsetY = 0.0)
            return
        }
        view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        val placement = geometry.place(framing)
        val pictureWidth = geometry.fitWidth * placement.scale
        val pictureHeight = geometry.fitHeight * placement.scale
        if (selfPlaced != null) {
            surface?.place(scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f)
            selfPlaced.placePicture(
                scale = placement.scale.toDouble(),
                offsetX = (placement.offsetX / pictureWidth).toDouble(),
                offsetY = (placement.offsetY / pictureHeight).toDouble(),
            )
        } else {
            surface?.place(
                scaleX = pictureWidth / geometry.width,
                scaleY = pictureHeight / geometry.height,
                offsetX = placement.offsetX,
                offsetY = placement.offsetY,
            )
        }
    }
}

private fun View.place(scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
    this.scaleX = scaleX
    this.scaleY = scaleY
    translationX = offsetX
    translationY = offsetY
}
