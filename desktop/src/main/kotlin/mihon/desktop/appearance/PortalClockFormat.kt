package mihon.desktop.appearance

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.presentation.util.SystemClockFormat
import mihon.core.platform.PlatformContext
import java.util.Locale

/**
 * Linux: the `clock-format` the XDG desktop portal publishes under `org.gnome.desktop.interface`, which GNOME's clock
 * setting writes and some portals publish on other desktops too, followed as it changes. Where the portal publishes
 * none, the clock of the system's language, as Android takes it on a phone left at its language's default.
 */
class PortalClockFormat(settings: PortalSettings) : SystemClockFormat {
    private val clockFormat = settings.setting(INTERFACE_NAMESPACE, CLOCK_FORMAT_KEY)

    override fun is24Hour(context: PlatformContext): Boolean {
        // gdbus prints the value as `'24h'` or `'12h'` inside a variant
        return when (CLOCK_FORMAT_VALUE.find(clockFormat.value.orEmpty())?.groupValues?.get(1)) {
            "24h" -> true
            "12h" -> false
            else -> UiPreferences.uses24Hours(
                timeFormat = TimeFormat.FOLLOW_LANGUAGE,
                locale = Locale.getDefault(Locale.Category.FORMAT),
                is24HourSystem = false,
            )
        }
    }

    private companion object {
        const val INTERFACE_NAMESPACE = "org.gnome.desktop.interface"
        const val CLOCK_FORMAT_KEY = "clock-format"
        val CLOCK_FORMAT_VALUE = Regex("""'(12h|24h)'""")
    }
}
