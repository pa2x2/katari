package eu.kanade.tachiyomi.extension.update

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.LaunchTask
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat

/**
 * Checks the stores for updates of the installed extensions at each launch, which installs them or asks about them as
 * the user chose. In the background, so the launch doesn't wait for the stores to answer.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class ExtensionUpdateCheckLaunchTask(private val extensionManager: ExtensionManager) : LaunchTask {
    override suspend fun run(scope: CoroutineScope) {
        scope.launchIO {
            try {
                extensionManager.checkForUpdates()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }
}
