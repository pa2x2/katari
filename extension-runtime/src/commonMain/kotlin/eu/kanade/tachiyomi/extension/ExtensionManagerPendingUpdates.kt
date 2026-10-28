package eu.kanade.tachiyomi.extension

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/** The updates the extension manager found, and whether it's installing them without asking. */
@Inject
class ExtensionManagerPendingUpdates(extensionManager: ExtensionManager) : PendingExtensionUpdates {
    override val count: Flow<Int> = extensionManager.pendingUpdatesCount
    override val isAutoUpdateInProgress: Flow<Boolean> = extensionManager.isAutoUpdateInProgress
}
