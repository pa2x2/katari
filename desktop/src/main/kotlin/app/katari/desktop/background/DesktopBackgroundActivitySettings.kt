package app.katari.desktop.background

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.desktop.autostart.LoginStart
import app.katari.desktop.tray.AppTray
import app.katari.desktop.tray.TrayPreferences
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.general.BackgroundActivitySettings
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState as collectPreferenceAsState

/**
 * Desktop systems put no limits on an app's background work, which runs for as long as the app does, so what keeps it
 * working is the app running on once its window closes, with its icon in the tray, and starting at login hidden there.
 * Where the desktop shows no tray, neither switch can be turned, as closing the window quits the app then; starting at
 * login also needs the app to keep running in the tray, and a place to start from that outlasts this run.
 */
@Inject
class DesktopBackgroundActivitySettings(
    private val preferences: TrayPreferences,
    private val tray: AppTray,
    private val loginStart: LoginStart,
) : BackgroundActivitySettings {
    @Composable
    override fun preferences(): List<Preference.PreferenceItem<out Any, out Any>> {
        val trayRuns by tray.hostAvailable.collectAsState()
        val keepsRunning by preferences.keepRunningInTray.collectPreferenceAsState()
        return listOfNotNull(
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
            loginStartItem(interactive = trayRuns && keepsRunning).takeIf { loginStart.isOffered },
        )
    }

    // The entry in the autostart folder is the setting, rather than a stored preference, so the row is built here
    @Composable
    private fun loginStartItem(interactive: Boolean): Preference.PreferenceItem<out Any, out Any> {
        val title = stringResource(PlatformStrings.pref_start_at_login)
        val summary = stringResource(PlatformStrings.pref_start_at_login_summary)
        val on by loginStart.isOn.collectAsState()
        // The desktop's own settings may have changed the entry since the app last looked
        LaunchedEffect(loginStart) { loginStart.refresh() }
        return Preference.PreferenceItem.CustomPreference(title = title) {
            SwitchPreferenceWidget(
                title = title,
                subtitle = summary,
                checked = on,
                enabled = interactive,
                onCheckedChanged = loginStart::set,
            )
        }
    }
}
