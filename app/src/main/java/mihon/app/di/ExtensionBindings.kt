package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.extension.AvailableExtensionsRefresh
import eu.kanade.tachiyomi.extension.ExtensionManager

@BindingContainer
object ExtensionBindings {
    @Provides
    fun availableExtensionsRefresh(extensionManager: ExtensionManager): AvailableExtensionsRefresh =
        AvailableExtensionsRefresh { extensionManager.findAvailableExtensions() }
}
