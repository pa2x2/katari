package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.extension.AvailableExtensionsRefresh
import mihon.desktop.extension.NoAvailableExtensionsRefresh

@BindingContainer
interface ExtensionBindings {
    @Binds
    val NoAvailableExtensionsRefresh.availableExtensionsRefresh: AvailableExtensionsRefresh
}
