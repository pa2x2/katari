package eu.kanade.tachiyomi.extension.file

import eu.kanade.tachiyomi.extension.installer.ExtensionInstallOperation
import eu.kanade.tachiyomi.extension.installer.routing
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.store.isInstallable
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** A file extension a restored backup had installed, which [FileExtensionRestore] installs again. */
data class RestoredFileExtension(
    val pkgName: String,
    val name: String,
    val storeIndexUrl: String,
)

/**
 * Installs the file extensions of a restored backup from the stores they came from, in the background once the
 * restored stores' listings are in, then says in one notice what was installed and what couldn't be.
 */
internal class FileExtensionRestore(
    private val installer: ExtensionInstaller,
    private val kinds: ExtensionKinds,
    private val notices: ExtensionNotices,
) {

    /** [listings] are every store's, fetched after the stores were restored; [installed] are on the device already. */
    suspend fun install(
        restored: List<RestoredFileExtension>,
        listings: List<Extension.Available>,
        isInstalled: (pkgName: String) -> Boolean,
    ) {
        val installed = mutableListOf<String>()
        val missed = mutableListOf<String>()
        restored.filterNot { isInstalled(it.pkgName) }.forEach { extension ->
            val listing = listings.firstOrNull {
                it.pkgName == extension.pkgName &&
                    it.store.indexUrl == extension.storeIndexUrl &&
                    it.isInstallable(kinds)
            }
            if (listing != null && install(listing) == InstallStep.Installed) {
                installed += extension.name
            } else {
                missed += extension.name
            }
        }
        if (installed.isNotEmpty() || missed.isNotEmpty()) notices.backupExtensionsInstalled(installed, missed)
    }

    private suspend fun install(listing: Extension.Available): InstallStep {
        val routing = ExtensionInstallOperation.NewInstall.routing()
        return try {
            installer.downloadAndInstall(listing, routing.userActionBehavior, routing.installRoute)
                .first { it.isCompleted() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to install ${listing.pkgName} from a backup" }
            InstallStep.Error.from(e)
        }
    }
}
