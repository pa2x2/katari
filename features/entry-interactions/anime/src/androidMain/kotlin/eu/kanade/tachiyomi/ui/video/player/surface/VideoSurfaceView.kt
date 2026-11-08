package eu.kanade.tachiyomi.ui.video.player.surface

import android.content.Context
import android.graphics.Canvas
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Handler
import android.os.Looper
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.window.SurfaceSyncGroup
import androidx.annotation.RequiresApi

/**
 * The view a player draws its picture into: a [SurfaceView] filling it, under a shutter in the media colour that hides
 * the surface until it shows a picture. The surface is scaled and moved inside the view to place the picture.
 *
 * Two of Media3's player view's workarounds are kept. From Android 14 the surface lives as long as the view is attached,
 * not only while it is visible, so it isn't torn down under a playing video. On Android 14 alone, a surface that
 * changes size inside Compose shows a frame of the old size until the next drawing
 * ([androidx/media#1237](https://github.com/androidx/media/issues/1237)), so its size change and the window's next
 * drawing are applied together.
 */
internal class VideoSurfaceView(context: Context) : FrameLayout(context), SurfaceHolder.Callback {
    val surfaceView: SurfaceView = SurfaceView(context)
    private val shutter = View(context)
    private val sizeSync = if (SDK_INT == UPSIDE_DOWN_CAKE) SurfaceSizeSync() else null

    init {
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            surfaceView.setSurfaceLifecycle(SurfaceView.SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)
        }
        surfaceView.holder.addCallback(this)
        addView(surfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(shutter, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** The colour around the picture and over the surface while the shutter is closed. */
    fun setMediaColor(argb: Int) {
        setBackgroundColor(argb)
        shutter.setBackgroundColor(argb)
    }

    fun setShutterClosed(closed: Boolean) {
        shutter.visibility = if (closed) VISIBLE else INVISIBLE
    }

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (SDK_INT == UPSIDE_DOWN_CAKE) {
            sizeSync?.register(surfaceView, ::invalidate)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (SDK_INT == UPSIDE_DOWN_CAKE) {
            sizeSync?.markReady()
        }
    }
}

@RequiresApi(UPSIDE_DOWN_CAKE)
private class SurfaceSizeSync {
    private val main = Handler(Looper.getMainLooper())
    private var group: SurfaceSyncGroup? = null

    fun register(surfaceView: SurfaceView, invalidate: () -> Unit) {
        main.post {
            // Not in a window, so there is no drawing to wait for
            val root = surfaceView.rootSurfaceControl ?: return@post
            val group = SurfaceSyncGroup(SYNC_GROUP_NAME)
            check(group.add(root) {})
            this.group = group
            invalidate()
            root.applyTransactionOnDraw(SurfaceControl.Transaction())
        }
    }

    fun markReady() {
        group?.markSyncReady()
        group = null
    }

    private companion object {
        const val SYNC_GROUP_NAME = "katari-video-surface-size"
    }
}
