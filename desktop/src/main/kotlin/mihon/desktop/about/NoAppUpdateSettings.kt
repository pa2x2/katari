package mihon.desktop.about

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings

/**
 * Katari's releases carry Android APKs only, so desktop has nothing to update itself from. Stands in until desktop
 * builds are released.
 */
@Inject
class NoAppUpdateSettings : AppUpdateSettings {
    override fun aboutRows(): List<@Composable () -> Unit> = emptyList()
}
