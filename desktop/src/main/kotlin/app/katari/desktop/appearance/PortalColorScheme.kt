package app.katari.desktop.appearance

import app.katari.presentation.kit.theme.SystemColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Linux: the `color-scheme` the XDG desktop portal publishes, followed as it changes. */
class PortalColorScheme(settings: PortalSettings, scope: CoroutineScope) : SystemColorScheme {
    override val prefersDark: StateFlow<Boolean> = settings.appearance(COLOR_SCHEME_KEY).let { value ->
        value.map(::prefersDark).stateIn(scope, SharingStarted.Eagerly, prefersDark(value.value))
    }

    // gdbus prints the value as `uint32 N` inside a variant; 1 is a preference for dark, 2 for light and 0 none
    private fun prefersDark(value: String?): Boolean =
        value?.let { UINT32_VALUE.find(it) }?.groupValues?.get(1)?.toIntOrNull() == 1

    private companion object {
        const val COLOR_SCHEME_KEY = "color-scheme"
        val UINT32_VALUE = Regex("""uint32 (\d+)""")
    }
}
