package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.ui.unit.IntSize

/**
 * The sizes the long strip last showed its pages at, by slot key, so a page that left the composition and comes back
 * takes its height at once instead of a placeholder's while its image opens again. A size holds only under the image
 * settings it was shown with, since those can change it, as cropping borders does.
 */
internal class StripPageSizes {
    private val sizes = mutableMapOf<String, ShownSize>()

    operator fun get(slotKey: String, settingsKey: Any): IntSize? =
        sizes[slotKey]?.takeIf { it.settingsKey == settingsKey }?.size

    operator fun set(slotKey: String, settingsKey: Any, size: IntSize) {
        sizes[slotKey] = ShownSize(settingsKey, size)
    }
}

private class ShownSize(val settingsKey: Any, val size: IntSize)
