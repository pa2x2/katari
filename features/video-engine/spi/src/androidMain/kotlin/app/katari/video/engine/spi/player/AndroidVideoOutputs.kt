package app.katari.video.engine.spi.player

import android.view.SurfaceView
import android.view.TextureView

/** A [SurfaceView] as a player's output, which every Android engine draws into. */
class SurfaceViewOutput(val view: SurfaceView) : VideoOutput

/**
 * A [TextureView] as a player's output, for a picture that is clipped, faded or moved with the views around it, as a
 * [SurfaceView] can't be. Only ExoPlayer draws into one.
 */
class TextureViewOutput(val view: TextureView) : VideoOutput
