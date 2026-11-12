package app.katari.desktop.background

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.desktop.tray.AppTray
import app.katari.desktop.tray.TrayPreferences
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.general.BackgroundActivitySettings
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Desktop systems put no limits on an app's background work, which runs for as long as the app does, so what keeps it
 * working is the app running on once its window closes, with its icon in the tray. Where the desktop shows no tray,
 * the switch can't be turned, as closing the window quits the app then.
 */
@Inject
class DesktopBackgroundActivitySettings(
    private val preferences: TrayPreferences,
    private val tray: AppTray,
) : BackgroundActivitySettings {
    @Composable
    override fun preferences(): List<Preference.PreferenceItem<out Any, out Any>> {
        val trayRuns by tray.hostAvailable.collectAsState()
        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = preferences.keepRunningInTray,
                title = stringResource(PlatformStrings.pref_keep_running_in_tray),
                subtitle = stringResource(
                    if (trayRuns) {
                        PlatformStrings.pref_keep_running_in_tray_summary
                    } else {
                        PlatformStrings.pref_keep_running_in_tray_no_tray
                    },
                ),
                isInteractive = trayRuns,
            ),
        )
    }
}
