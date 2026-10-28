package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.extension.AndroidExtensionManagement
import eu.kanade.tachiyomi.extension.AndroidExtensionSystemPackages
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionManagerSourceExtensionPackages
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.kind.KindExtensionCompatibility
import kotlinx.coroutines.flow.Flow

@BindingContainer
interface ExtensionBindings {
    @Binds
    val AndroidExtensionManagement.extensionManagement: ExtensionManagement

    @Binds
    val KindExtensionCompatibility.extensionCompatibility: ExtensionCompatibility

    @Binds
    val AndroidExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

    @Binds
    val ExtensionManagerSourceExtensionPackages.sourceExtensionPackages: SourceExtensionPackages

    companion object {
        @Provides
        fun pendingExtensionUpdates(extensionManager: ExtensionManager): PendingExtensionUpdates =
            object : PendingExtensionUpdates {
                override val count: Flow<Int> = extensionManager.pendingUpdatesCount
                override val isAutoUpdateInProgress: Flow<Boolean> = extensionManager.isAutoUpdateInProgress
            }
    }
}
