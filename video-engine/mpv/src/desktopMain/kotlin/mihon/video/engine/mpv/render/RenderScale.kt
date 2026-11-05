package mihon.video.engine.mpv.render

import java.awt.GraphicsConfiguration
import java.lang.reflect.InaccessibleObjectException
import kotlin.math.roundToInt

/**
 * The size to render a [width] by [height] picture at, in pixels, for a window whose toolkit draws at
 * [effectiveScale] on a screen of [screenScale].
 *
 * The native Wayland toolkit draws at whole scales only: on a screen scaled by 1.25 it draws at 2, and the compositor
 * shrinks the window to fit. A frame rendered at the window's own pixels would be rendered at 2.56 times the pixels
 * the screen shows, and shrunk again on the way. So the picture renders at the screen's scale and is stretched to the
 * window's pixels as it is drawn. A toolkit drawing at the screen's scale, or below it, renders at its own.
 */
internal fun renderSize(width: Int, height: Int, effectiveScale: Double, screenScale: Double): Pair<Int, Int> {
    if (screenScale <= 0.0 || effectiveScale <= screenScale) return width to height
    val factor = screenScale / effectiveScale
    return (width * factor).roundToInt().coerceAtLeast(1) to (height * factor).roundToInt().coerceAtLeast(1)
}

/** The scale the toolkit draws [this] configuration's windows at. */
internal val GraphicsConfiguration.effectiveScale: Double get() = defaultTransform.scaleX

/**
 * The scale the screen itself has, which only the native Wayland toolkit draws away from: its device knows the scale
 * the compositor gave the screen, which the app's `--add-opens java.desktop/sun.awt.wl` lets this read. 0 elsewhere,
 * and where it can't be read.
 */
internal val GraphicsConfiguration.screenScale: Double
    get() {
        val device = device
        if (device.javaClass.name != WAYLAND_DEVICE) return 0.0
        return try {
            device.javaClass.getDeclaredMethod("getPhysicalScale").apply {
                isAccessible = true
            }.invoke(device) as Double
        } catch (_: ReflectiveOperationException) {
            0.0
        } catch (_: InaccessibleObjectException) {
            0.0
        }
    }

private const val WAYLAND_DEVICE = "sun.awt.wl.WLGraphicsDevice"
