package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.extension.AndroidExtensionSystemPackages
import eu.kanade.tachiyomi.extension.DefaultExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionManagerPendingUpdates
import eu.kanade.tachiyomi.extension.ExtensionManagerSourceExtensionPackages
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.kind.KindExtensionCompatibility
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionFiles
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionNotices
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackages
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages

/** The shared extension host's contracts for the screens, and Android's answers to what it asks the platform. */
@BindingContainer
interface ExtensionBindings {
    @Binds
    val DefaultExtensionManagement.extensionManagement: ExtensionManagement

    @Binds
    val KindExtensionCompatibility.extensionCompatibility: ExtensionCompatibility

    @Binds
    val AndroidExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

    @Binds
    val ExtensionManagerSourceExtensionPackages.sourceExtensionPackages: SourceExtensionPackages

    @Binds
    val ExtensionManagerPendingUpdates.pendingExtensionUpdates: PendingExtensionUpdates

    @Binds
    val AndroidExtensionPackages.extensionPackages: ExtensionPackages

    @Binds
    val AndroidExtensionClassLoaders.extensionClassLoaders: ExtensionClassLoaders

    @Binds
    val AndroidExtensionPackageInstaller.extensionPackageInstaller: ExtensionPackageInstaller

    @Binds
    val AndroidExtensionInstallEvents.extensionInstallEvents: ExtensionInstallEvents

    @Binds
    val AndroidExtensionNotices.extensionNotices: ExtensionNotices

    @Binds
    val AndroidExtensionFiles.extensionFiles: ExtensionFiles
}
