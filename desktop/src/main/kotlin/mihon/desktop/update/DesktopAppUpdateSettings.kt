package mihon.desktop.update

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.ui.appUpdateAboutRows

/** The updater that replaces the release's AppImage from its GitHub releases; other builds have none. */
@Inject
class DesktopAppUpdateSettings(
    private val controller: AppUpdateController,
    private val preferences: AppUpdatePreferences,
) : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> =
        if (RunningAppImage.canUpdate) appUpdateAboutRows(controller, preferences) else emptyList()
}
