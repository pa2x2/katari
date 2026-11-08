package app.katari.appupdate.ui

import androidx.compose.runtime.Composable
import app.katari.appupdate.AppUpdateBuild
import app.katari.appupdate.AppUpdateController
import app.katari.appupdate.AppUpdatePreferences
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings

/** The updater that installs Katari's APKs from its GitHub releases; builds that can't update themselves have none. */
@Inject
class AndroidAppUpdateSettings(
    private val controller: AppUpdateController,
    private val preferences: AppUpdatePreferences,
) : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> =
        if (AppUpdateBuild.isSupported) appUpdateAboutRows(controller, preferences) else emptyList()
}
