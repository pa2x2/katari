package app.katari.desktop.update

import androidx.compose.runtime.Composable
import app.katari.appupdate.AppUpdateController
import app.katari.appupdate.AppUpdatePreferences
import app.katari.appupdate.ui.appUpdateAboutRows
import app.katari.desktop.application.installation.AppInstallation
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings

/** The updater of an installation that updates itself from its GitHub releases; other installations have none. */
@Inject
class DesktopAppUpdateSettings(
    private val controller: AppUpdateController,
    private val preferences: AppUpdatePreferences,
    private val installation: AppInstallation,
) : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> =
        if (installation.update != null) appUpdateAboutRows(controller, preferences) else emptyList()
}
