package mihon.desktop.update

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import mihon.desktop.application.installation.AppInstallation
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.ui.appUpdateAboutRows

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
