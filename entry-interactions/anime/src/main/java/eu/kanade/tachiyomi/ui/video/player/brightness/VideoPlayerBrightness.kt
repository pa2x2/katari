package eu.kanade.tachiyomi.ui.video.player.brightness

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The player's screen brightness as a gesture level from 0 to [MAX_LEVEL]. While the window has no brightness override
 * and the level sits at the default, the screen follows the system setting and the level tracks it; a gesture overrides
 * the window's brightness along a gamma curve. Level 0 also dims the picture through [overlayValue].
 */
internal class VideoPlayerBrightness(
    private val window: Window,
    private val contentResolver: ContentResolver,
) {
    var level by mutableStateOf(DEFAULT_LEVEL)
        private set

    /** What the content overlay draws over the video: a dim at level 0, nothing otherwise. */
    var overlayValue by mutableStateOf(0)
        private set

    private var observerRegistered = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (isUsingSystemBrightness()) {
                sync()
            }
        }
    }

    fun sync() {
        val screenBrightness = window.attributes.screenBrightness
        if (screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            level = readSystemLevel()
            overlayValue = 0
        } else {
            level = gestureLevelForScreenBrightness(screenBrightness)
            overlayValue = brightnessOverlayLevelFor(level)
        }
    }

    /** Re-reads the level and writes it back to the window, as each new episode starts. */
    fun reapply() {
        sync()
        applyToWindow(if (isUsingSystemBrightness()) DEFAULT_LEVEL else level)
    }

    /** Sets the level a side gesture asks for and returns it clamped to the gesture range. */
    fun adjust(level: Int): Int {
        val normalizedLevel = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        if (this.level != normalizedLevel) {
            this.level = normalizedLevel
        }
        val overlayValue = brightnessOverlayLevelFor(normalizedLevel)
        if (this.overlayValue != overlayValue) {
            this.overlayValue = overlayValue
        }
        applyToWindow(normalizedLevel)
        return normalizedLevel
    }

    fun reset() {
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    fun registerObserver() {
        if (observerRegistered) return

        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer,
        )
        observerRegistered = true
    }

    fun unregisterObserver() {
        if (!observerRegistered) return

        contentResolver.unregisterContentObserver(observer)
        observerRegistered = false
    }

    private fun applyToWindow(level: Int) {
        val normalizedLevel = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val screenBrightness = when {
            isUsingSystemBrightness(level) -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            normalizedLevel > 0 -> screenBrightnessForGestureLevel(normalizedLevel)
            else -> MIN_POSITIVE_BRIGHTNESS
        }
        window.attributes = window.attributes.apply {
            this.screenBrightness = screenBrightness
        }
    }

    private fun readSystemLevel(): Int {
        val brightnessValue = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(DEFAULT_SYSTEM_BRIGHTNESS)
        return gestureLevelForScreenBrightness(brightnessValue / SYSTEM_BRIGHTNESS_MAX.toFloat())
    }

    private fun isUsingSystemBrightness(level: Int = this.level): Boolean {
        return level == DEFAULT_LEVEL &&
            window.attributes.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    companion object {
        const val MAX_LEVEL = 100
        private const val MIN_LEVEL = 0
        private const val DEFAULT_LEVEL = 50
        private const val DEFAULT_SYSTEM_BRIGHTNESS = 127
        private const val SYSTEM_BRIGHTNESS_MAX = 255
        private const val MIN_POSITIVE_BRIGHTNESS = 0.01f
        private const val BRIGHTNESS_RESPONSE_GAMMA = 2.2f

        private fun brightnessOverlayLevelFor(level: Int): Int {
            return if (level <= 0) -75 else 0
        }

        private fun screenBrightnessForGestureLevel(level: Int): Float {
            val normalizedLevel = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
            if (normalizedLevel <= MIN_LEVEL) {
                return MIN_POSITIVE_BRIGHTNESS
            }

            val gestureFraction = normalizedLevel / MAX_LEVEL.toFloat()
            return (
                MIN_POSITIVE_BRIGHTNESS +
                    (1f - MIN_POSITIVE_BRIGHTNESS) * gestureFraction.pow(BRIGHTNESS_RESPONSE_GAMMA)
                ).coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
        }

        private fun gestureLevelForScreenBrightness(screenBrightness: Float): Int {
            val clampedBrightness = screenBrightness.coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
            val brightnessFraction = (
                (clampedBrightness - MIN_POSITIVE_BRIGHTNESS) /
                    (1f - MIN_POSITIVE_BRIGHTNESS)
                ).coerceIn(0f, 1f)
            return (brightnessFraction.pow(1f / BRIGHTNESS_RESPONSE_GAMMA) * MAX_LEVEL)
                .roundToInt()
                .coerceIn(MIN_LEVEL, MAX_LEVEL)
        }
    }
}
