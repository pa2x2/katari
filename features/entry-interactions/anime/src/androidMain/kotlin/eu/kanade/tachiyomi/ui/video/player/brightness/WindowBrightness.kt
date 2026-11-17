package eu.kanade.tachiyomi.ui.video.player.brightness

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The window's brightness as a level from -[ExtraDim.LEVELS] to [MAX_LEVEL]. While the window has no brightness
 * override and the level sits at the default, the screen follows the system setting and the level tracks it; a gesture
 * overrides the window's brightness along a gamma curve. At 0 and below the screen is at its lowest, and below 0
 * [ExtraDim] darkens the picture further. When the viewer keeps it, the level set carries over to the next time the
 * player opens.
 */
internal class WindowBrightness(
    private val window: Window,
    private val contentResolver: ContentResolver,
    private val preferences: AnimePlayerDevicePreferences,
) : VideoPlayerBrightness {
    override var level by mutableIntStateOf(DEFAULT_LEVEL)
        private set

    override val levels = -ExtraDim.LEVELS..MAX_LEVEL

    override var pictureDim by mutableFloatStateOf(0f)
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
            pictureDim = 0f
        } else {
            // Every level from 0 down shares the screen's lowest brightness, so the window alone can't tell them apart
            if (screenBrightness != screenBrightnessForLevel(level)) level = levelForScreenBrightness(screenBrightness)
            pictureDim = ExtraDim.pictureDim(level)
        }
    }

    /** Opens at the level kept from the last time, or follows the system when none is kept. */
    fun restore() {
        val keptLevel = preferences.brightnessLevel.get()
        if (preferences.rememberBrightness.get() && keptLevel in levels) {
            adjust(keptLevel)
        } else {
            sync()
        }
    }

    /** Keeps a level set by a gesture for the next opening, and forgets any once the viewer stops keeping it. */
    fun remember() {
        when {
            !preferences.rememberBrightness.get() -> preferences.brightnessLevel.delete()
            window.attributes.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE -> {
                preferences.brightnessLevel.set(level)
            }
        }
    }

    override fun reapply() {
        sync()
        applyToWindow(if (isUsingSystemBrightness()) DEFAULT_LEVEL else level)
    }

    override fun adjust(level: Int): Int {
        val normalizedLevel = level.coerceIn(levels)
        this.level = normalizedLevel
        pictureDim = ExtraDim.pictureDim(normalizedLevel)
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
        val screenBrightness = if (isUsingSystemBrightness(level)) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            screenBrightnessForLevel(level)
        }
        window.attributes = window.attributes.apply {
            this.screenBrightness = screenBrightness
        }
    }

    private fun readSystemLevel(): Int {
        val brightnessValue = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(DEFAULT_SYSTEM_BRIGHTNESS)
        return levelForScreenBrightness(brightnessValue / SYSTEM_BRIGHTNESS_MAX.toFloat())
    }

    private fun isUsingSystemBrightness(level: Int = this.level): Boolean {
        return level == DEFAULT_LEVEL &&
            window.attributes.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    private companion object {
        const val MAX_LEVEL = 100
        const val DEFAULT_LEVEL = 50
        const val DEFAULT_SYSTEM_BRIGHTNESS = 127
        const val SYSTEM_BRIGHTNESS_MAX = 255
        const val MIN_POSITIVE_BRIGHTNESS = 0.01f
        const val BRIGHTNESS_RESPONSE_GAMMA = 2.2f

        fun screenBrightnessForLevel(level: Int): Float {
            if (level <= 0) return MIN_POSITIVE_BRIGHTNESS

            val gestureFraction = level.coerceAtMost(MAX_LEVEL) / MAX_LEVEL.toFloat()
            return (
                MIN_POSITIVE_BRIGHTNESS +
                    (1f - MIN_POSITIVE_BRIGHTNESS) * gestureFraction.pow(BRIGHTNESS_RESPONSE_GAMMA)
                ).coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
        }

        fun levelForScreenBrightness(screenBrightness: Float): Int {
            val clampedBrightness = screenBrightness.coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
            val brightnessFraction = (
                (clampedBrightness - MIN_POSITIVE_BRIGHTNESS) /
                    (1f - MIN_POSITIVE_BRIGHTNESS)
                ).coerceIn(0f, 1f)
            return (brightnessFraction.pow(1f / BRIGHTNESS_RESPONSE_GAMMA) * MAX_LEVEL)
                .roundToInt()
                .coerceIn(0, MAX_LEVEL)
        }
    }
}
