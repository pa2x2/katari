package eu.kanade.tachiyomi.util.system

import java.awt.GraphicsEnvironment

/**
 * Converts to px, at the scale the toolkit draws the main screen at.
 */
val Int.dpToPx: Int
    get() = (this * screenScale).toInt()

private val screenScale: Double
    get() {
        if (GraphicsEnvironment.isHeadless()) return 1.0
        return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            .defaultTransform.scaleX
    }
