package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.extension.AvailableExtensionsRefresh
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import mihon.desktop.extension.NoAvailableExtensionsRefresh
import mihon.desktop.extension.NoPendingExtensionUpdates

@BindingContainer
interface ExtensionBindings {
    @Binds
    val NoAvailableExtensionsRefresh.availableExtensionsRefresh: AvailableExtensionsRefresh

    @Binds
    val NoPendingExtensionUpdates.pendingExtensionUpdates: PendingExtensionUpdates
}
