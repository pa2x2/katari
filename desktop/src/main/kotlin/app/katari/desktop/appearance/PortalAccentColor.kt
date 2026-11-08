package app.katari.desktop.appearance

import app.katari.presentation.kit.theme.SystemAccentColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToLong

/**
 * Linux: the `accent-color` the XDG desktop portal publishes, followed as it changes. Newer GNOME and KDE Plasma
 * publish one; many other desktops publish none.
 */
class PortalAccentColor(settings: PortalSettings, scope: CoroutineScope) : SystemAccentColor {
    override val accentArgb: StateFlow<Long?> = settings.appearance(ACCENT_COLOR_KEY).let { value ->
        value.map(::accentArgb).stateIn(scope, SharingStarted.Eagerly, accentArgb(value.value))
    }

    // gdbus prints the value as `(0.24, 0.68, 0.91)` inside a variant: red, green and blue in sRGB from 0 to 1. The
    // portal marks an accent the user left unset with values outside that range.
    private fun accentArgb(value: String?): Long? {
        val channels = value?.let { RGB_VALUE.find(it) }?.destructured?.toList()?.map { it.toDoubleOrNull() }
        if (channels == null || channels.any { it == null || it !in 0.0..1.0 }) return null
        return channels.filterNotNull().fold(OPAQUE) { argb, channel -> (argb shl 8) or (channel * 255).roundToLong() }
    }

    private companion object {
        const val ACCENT_COLOR_KEY = "accent-color"
        const val OPAQUE = 0xFFL
        val RGB_VALUE = Regex("""\(([^,()]+), ([^,()]+), ([^,()]+)\)""")
    }
}
