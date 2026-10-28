package mihon.desktop.display.size

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The interface size the window draws at on top of the session's scale, which its shortcuts change too. */
@Inject
class DesktopInterfaceSizeSetting(private val preferences: InterfaceSizePreferences) : InterfaceSizeSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any> =
        Preference.PreferenceItem.StepperPreference(
            preference = preferences.interfaceSize,
            valueRange = InterfaceSize.RANGE,
            step = InterfaceSize.STEP,
            valueFormatter = { "$it%" },
            inputSuffix = "%",
            decreaseContentDescription = stringResource(MR.strings.keyboard_shortcut_interface_smaller),
            increaseContentDescription = stringResource(MR.strings.keyboard_shortcut_interface_larger),
            editContentDescription = stringResource(MR.strings.action_edit_interface_size),
            title = stringResource(MR.strings.pref_interface_size),
            subtitle = stringResource(MR.strings.pref_interface_size_summary),
        )
}
