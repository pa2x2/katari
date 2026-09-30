package eu.kanade.tachiyomi.ui.reader

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.ViewTreeObserver
import android.view.Window
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Samples the reader content drawn behind system bars so bar icons can keep contrast with the page beneath them.
 *
 * Pages are drawn edge-to-edge, so the artwork under a bar changes with every page turn and scroll. While enabled,
 * redraws are sampled at most once per [SAMPLE_INTERVAL_MS], with a trailing sample once drawing settles.
 */
internal class ReaderSystemBarContentSampler(
    private val window: Window,
    private val onContentChanged: () -> Unit,
) {
    /** Whether the content behind the status bar is light, or null before it has been sampled. */
    var statusBarContentIsLight: Boolean? = null
        private set

    /** Whether the content behind the navigation bar is light, or null before it has been sampled. */
    var navigationBarContentIsLight: Boolean? = null
        private set

    private val decorView = window.decorView
    private val handler = Handler(Looper.getMainLooper())
    private val drawListener = ViewTreeObserver.OnDrawListener { scheduleSample() }
    private val sampleRunnable = Runnable {
        samplePending = false
        sample()
    }
    private var enabled = false
    private var samplePending = false

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (enabled) {
            decorView.viewTreeObserver.addOnDrawListener(drawListener)
            scheduleSample()
        } else {
            decorView.viewTreeObserver.removeOnDrawListener(drawListener)
            handler.removeCallbacks(sampleRunnable)
            samplePending = false
        }
    }

    private fun scheduleSample() {
        if (samplePending) return
        samplePending = true
        handler.postDelayed(sampleRunnable, SAMPLE_INTERVAL_MS)
    }

    private fun sample() {
        if (!enabled || !decorView.isAttachedToWindow) return
        val insets = ViewCompat.getRootWindowInsets(decorView) ?: return
        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) return

        val statusBar = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
        if (statusBar.top > 0) {
            sampleRegion(Rect(0, 0, width, statusBar.top)) { luminance ->
                val isLight = isLightContent(statusBarContentIsLight, luminance)
                if (statusBarContentIsLight != isLight) {
                    statusBarContentIsLight = isLight
                    onContentChanged()
                }
            }
        }

        val navigationBar = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
        val navigationBarRegion = when {
            navigationBar.bottom > 0 -> Rect(0, height - navigationBar.bottom, width, height)
            navigationBar.left > 0 -> Rect(0, 0, navigationBar.left, height)
            navigationBar.right > 0 -> Rect(width - navigationBar.right, 0, width, height)
            else -> null
        }
        if (navigationBarRegion != null) {
            sampleRegion(navigationBarRegion) { luminance ->
                val isLight = isLightContent(navigationBarContentIsLight, luminance)
                if (navigationBarContentIsLight != isLight) {
                    navigationBarContentIsLight = isLight
                    onContentChanged()
                }
            }
        }
    }

    private fun sampleRegion(region: Rect, onSampled: (luminance: Double) -> Unit) {
        val horizontal = region.width() >= region.height()
        val bitmap = createBitmap(
            width = if (horizontal) SAMPLE_LENGTH else SAMPLE_THICKNESS,
            height = if (horizontal) SAMPLE_THICKNESS else SAMPLE_LENGTH,
        )
        PixelCopy.request(
            window,
            region,
            bitmap,
            { result ->
                if (enabled && result == PixelCopy.SUCCESS) {
                    onSampled(bitmap.averageLuminance())
                }
                bitmap.recycle()
            },
            handler,
        )
    }

    private fun Bitmap.averageLuminance(): Double {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels.sumOf(ColorUtils::calculateLuminance) / pixels.size
    }

    private fun isLightContent(previous: Boolean?, luminance: Double): Boolean = when (previous) {
        true -> luminance > LIGHT_CONTENT_LUMINANCE - LUMINANCE_HYSTERESIS
        false -> luminance > LIGHT_CONTENT_LUMINANCE + LUMINANCE_HYSTERESIS
        null -> luminance > LIGHT_CONTENT_LUMINANCE
    }

    companion object {
        /**
         * Relative luminance at which dark and light icons contrast equally with the content behind them.
         */
        const val LIGHT_CONTENT_LUMINANCE = 0.18

        // Keeps icons from flickering while mid-tone artwork scrolls under a bar.
        private const val LUMINANCE_HYSTERESIS = 0.06
        private const val SAMPLE_INTERVAL_MS = 120L
        private const val SAMPLE_LENGTH = 48
        private const val SAMPLE_THICKNESS = 4
    }
}
