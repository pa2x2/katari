package app.katari.desktop.display.scroll.natural

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.desktop.display.backend.DisplayBackend
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.touchpad.TouchpadSettings
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Natural scrolling, as the system sets it or as chosen here, with what the system was found to set. Where that can't
 * be read, the note says that the choice tells the app what the system does.
 */
@Inject
class DesktopTouchpadSettings(
    private val preferences: TouchpadPreferences,
    private val display: DisplayBackend,
) : TouchpadSettings {
    @Composable
    override fun preferences(): List<Preference> {
        val system by display.systemNaturalScrolling.collectAsState()
        val asSystem = when (system) {
            true -> PlatformStrings.natural_scrolling_as_system_on
            false -> PlatformStrings.natural_scrolling_as_system_off
            null -> PlatformStrings.natural_scrolling_as_system_unknown
        }
        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = preferences.naturalScrolling,
                entries = mapOf(
                    NaturalScrollingChoice.AsSystem to stringResource(asSystem),
                    NaturalScrollingChoice.On to stringResource(MR.strings.on),
                    NaturalScrollingChoice.Off to stringResource(MR.strings.off),
                ),
                title = stringResource(PlatformStrings.pref_natural_scrolling),
            ),
            Preference.PreferenceItem.InfoPreference(
                title = stringResource(
                    if (system == null) {
                        PlatformStrings.pref_natural_scrolling_unknown_info
                    } else {
                        PlatformStrings.pref_natural_scrolling_info
                    },
                ),
            ),
        )
    }
}
