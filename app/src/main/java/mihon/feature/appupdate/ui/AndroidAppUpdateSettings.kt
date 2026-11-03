package mihon.feature.appupdate.ui

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences

/** The updater that installs Katari's APKs from its GitHub releases; builds that can't update themselves have none. */
@Inject
class AndroidAppUpdateSettings(
    private val controller: AppUpdateController,
    private val preferences: AppUpdatePreferences,
) : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> =
        if (AppUpdateBuild.isSupported) appUpdateAboutRows(controller, preferences) else emptyList()
}
