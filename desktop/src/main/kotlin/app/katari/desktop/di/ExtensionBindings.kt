package app.katari.desktop.di

import app.katari.desktop.extension.DesktopExtensionNotices
import app.katari.desktop.extension.NoExtensionSystemPackages
import app.katari.desktop.storage.DesktopDirectories
import app.katari.extension.runtime.jvm.DesktopHostedExtensionFamilies
import app.katari.extension.runtime.jvm.apk.DesktopExtensionPackages
import app.katari.extension.runtime.jvm.conversion.ConvertedJarCache
import app.katari.extension.runtime.jvm.files.DesktopExtensionFiles
import app.katari.extension.runtime.jvm.install.DesktopExtensionInstallEvents
import app.katari.extension.runtime.jvm.install.DesktopExtensionPackageInstaller
import app.katari.extension.runtime.jvm.loading.DesktopExtensionClassLoaders
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies
import eu.kanade.tachiyomi.extension.util.ExtensionLoader

/** The desktop host's answers to what the shared extension host asks the platform. */
@BindingContainer
interface ExtensionBindings {
    @Binds
    val NoExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

    @Binds
    val DesktopExtensionNotices.extensionNotices: ExtensionNotices

    companion object {
        @Provides
        fun hostedExtensionFamilies(): HostedExtensionFamilies = DesktopHostedExtensionFamilies()

        @Provides
        @SingleIn(AppScope::class)
        fun extensionFiles(directories: DesktopDirectories): ExtensionFiles = DesktopExtensionFiles(
            fileExtensionRoot = directories.data.resolve("extensions").toFile(),
            privateApkDir = directories.data.resolve("extension-apks").toFile(),
            downloadDir = directories.cache.resolve("extension-downloads").toFile(),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun extensionInstallEvents(): ExtensionInstallEvents = DesktopExtensionInstallEvents()

        // A cache: the jars are made again from the installed apks whenever they're missing
        @Provides
        @SingleIn(AppScope::class)
        fun convertedJarCache(directories: DesktopDirectories): ConvertedJarCache =
            ConvertedJarCache(directories.cache.resolve("extension-jars"))

        @Provides
        fun extensionPackages(files: ExtensionFiles): ExtensionPackages = DesktopExtensionPackages(files)

        @Provides
        fun extensionClassLoaders(jars: ConvertedJarCache): ExtensionClassLoaders = DesktopExtensionClassLoaders(jars)

        @Provides
        @SingleIn(AppScope::class)
        fun extensionPackageInstaller(
            loader: Lazy<ExtensionLoader>,
            kinds: ExtensionKinds,
            files: ExtensionFiles,
            installEvents: ExtensionInstallEvents,
            jars: ConvertedJarCache,
        ): ExtensionPackageInstaller = DesktopExtensionPackageInstaller(loader, kinds, files, installEvents, jars)
    }
}
