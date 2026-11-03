package mihon.desktop.display.backend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.advanced.DisplayBackendSetting
import mihon.desktop.application.AppRestart
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "Use native Wayland", offered where [backend] offers the toolkit. The toolkit is picked at start, so a change asks
 * to restart the app.
 */
@Inject
class DesktopDisplayBackendSetting(
    private val backend: DisplayBackend,
    private val preferences: DisplayBackendPreferences,
    private val restart: AppRestart,
) : DisplayBackendSetting {
    @Composable
    override fun preferenceItem(): Preference.PreferenceItem<out Any, out Any>? {
        if (!backend.isNativeWaylandOffered) return null
        var askToRestart by rememberSaveable { mutableStateOf(false) }
        if (askToRestart) {
            ConfirmDialog(
                title = stringResource(MR.strings.restart_app_title),
                text = stringResource(MR.strings.restart_app_display_backend),
                confirmLabel = stringResource(MR.strings.action_restart),
                onConfirm = {
                    askToRestart = false
                    restart.request()
                },
                onDismissRequest = { askToRestart = false },
                dismissLabel = stringResource(MR.strings.action_later),
            )
        }
        return Preference.PreferenceItem.SwitchPreference(
            preference = preferences.useNativeWayland,
            title = stringResource(MR.strings.pref_use_native_wayland),
            subtitle = stringResource(MR.strings.pref_use_native_wayland_summary),
            onValueChanged = {
                askToRestart = true
                true
            },
        )
    }
}
