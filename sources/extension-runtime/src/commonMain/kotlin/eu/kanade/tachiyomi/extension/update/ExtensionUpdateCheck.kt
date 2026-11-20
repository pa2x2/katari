package eu.kanade.tachiyomi.extension.update

import app.katari.core.metro.AutomaticCheck
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** Checks the stores for updates of the installed extensions, which installs them or asks about them as the user chose. */
@Inject
@ContributesIntoSet(AppScope::class)
class ExtensionUpdateCheck(private val extensionManager: ExtensionManager) : AutomaticCheck {
    override suspend fun check(): Boolean = try {
        extensionManager.checkForUpdates()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logcat(LogPriority.ERROR, e)
        false
    }
}
