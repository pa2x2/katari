package eu.kanade.tachiyomi.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.store.isInstallable
import eu.kanade.tachiyomi.ui.entry.missingsource.MissingSourceExtensions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The store listings the extension manager holds, which it installs from. */
@Inject
class ExtensionManagerMissingSourceExtensions(
    private val extensionManager: ExtensionManager,
) : MissingSourceExtensions {
    override val isOffered: Boolean = true

    override fun listing(sourceId: Long): Flow<Extension.Available?> = extensionManager.availableExtensionsFlow
        .map { extensionManager.getListingOfSource(sourceId)?.takeIf { it.isInstallable && !it.isTorrent } }

    override fun install(listing: Extension.Available): Flow<InstallStep> = extensionManager.installExtension(listing)
}
