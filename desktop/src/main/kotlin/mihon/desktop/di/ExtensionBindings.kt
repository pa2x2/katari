package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import mihon.desktop.extension.NoExtensionCompatibility
import mihon.desktop.extension.NoExtensionManagement
import mihon.desktop.extension.NoExtensionSystemPackages
import mihon.desktop.extension.NoPendingExtensionUpdates
import mihon.desktop.extension.NoSourceExtensionPackages

@BindingContainer
interface ExtensionBindings {
    @Binds
    val NoExtensionManagement.extensionManagement: ExtensionManagement

    @Binds
    val NoExtensionCompatibility.extensionCompatibility: ExtensionCompatibility

    @Binds
    val NoExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

    @Binds
    val NoPendingExtensionUpdates.pendingExtensionUpdates: PendingExtensionUpdates

    @Binds
    val NoSourceExtensionPackages.sourceExtensionPackages: SourceExtensionPackages
}
