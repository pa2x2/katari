package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.extension.AvailableExtensionsRefresh
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import kotlinx.coroutines.flow.Flow

@BindingContainer
object ExtensionBindings {
    @Provides
    fun availableExtensionsRefresh(extensionManager: ExtensionManager): AvailableExtensionsRefresh =
        AvailableExtensionsRefresh { extensionManager.findAvailableExtensions() }

    @Provides
    fun pendingExtensionUpdates(extensionManager: ExtensionManager): PendingExtensionUpdates =
        object : PendingExtensionUpdates {
            override val count: Flow<Int> = extensionManager.pendingUpdatesCount
            override val isAutoUpdateInProgress: Flow<Boolean> = extensionManager.isAutoUpdateInProgress
        }
}
