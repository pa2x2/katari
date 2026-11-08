package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.extension.AndroidExtensionSystemPackages
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionFiles
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionNotices
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.AndroidExtensionPackages
import eu.kanade.tachiyomi.extension.platform.AndroidHostedExtensionFamilies
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies

/** Android's answers to what the shared extension host asks the platform. */
@BindingContainer
interface ExtensionBindings {
    @Binds
    val AndroidExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

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

    @Binds
    val AndroidHostedExtensionFamilies.hostedExtensionFamilies: HostedExtensionFamilies
}
