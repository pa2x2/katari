package mihon.feature.appupdate.ui

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings

/** The updater that installs Katari's APKs from its GitHub releases. */
@Inject
class AndroidAppUpdateSettings : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> = appUpdateAboutRows()
}
