package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.DefaultExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionManagerPendingUpdates
import eu.kanade.tachiyomi.extension.ExtensionManagerSourceExtensionPackages
import eu.kanade.tachiyomi.extension.ExtensionSystemPackages
import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import eu.kanade.tachiyomi.extension.SourceExtensionPackages
import eu.kanade.tachiyomi.extension.claims.SourceClaims
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.kind.KindExtensionCompatibility
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.platform.ExtensionPackages
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import mihon.core.common.GlobalCustomPreferences
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.extension.DesktopExtensionNotices
import mihon.desktop.extension.NoExtensionSystemPackages
import mihon.desktop.storage.DesktopDirectories
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.extension.runtime.jvm.DesktopHostedExtensionFamilies
import mihon.extension.runtime.jvm.apk.DesktopExtensionPackages
import mihon.extension.runtime.jvm.conversion.ConvertedJarCache
import mihon.extension.runtime.jvm.files.DesktopExtensionFiles
import mihon.extension.runtime.jvm.install.DesktopExtensionInstallEvents
import mihon.extension.runtime.jvm.install.DesktopExtensionPackageInstaller
import mihon.extension.runtime.jvm.loading.DesktopExtensionClassLoaders
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.domain.source.identity.SourceIdentityRepository

/** The shared extension host's contracts for the screens, and the desktop host's answers to what it asks the platform. */
@BindingContainer
interface ExtensionBindings {
    @Binds
    val DefaultExtensionManagement.extensionManagement: ExtensionManagement

    @Binds
    val KindExtensionCompatibility.extensionCompatibility: ExtensionCompatibility

    @Binds
    val NoExtensionSystemPackages.extensionSystemPackages: ExtensionSystemPackages

    @Binds
    val ExtensionManagerSourceExtensionPackages.sourceExtensionPackages: SourceExtensionPackages

    @Binds
    val ExtensionManagerPendingUpdates.pendingExtensionUpdates: PendingExtensionUpdates

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

        @Provides
        @SingleIn(AppScope::class)
        fun extensionManager(
            loader: ExtensionLoader,
            kinds: ExtensionKinds,
            packageInstaller: ExtensionPackageInstaller,
            installEvents: ExtensionInstallEvents,
            notices: ExtensionNotices,
            files: ExtensionFiles,
            preferences: SourcePreferences,
            globalPreferences: GlobalSourcePreferences,
            trustExtension: TrustExtension,
            profileDatabase: ProfileDatabase,
            profileStore: ProfileStore,
            extensionStoreRepository: ExtensionStoreRepository,
            updateExtensionStores: UpdateExtensionStores,
            customPreferences: GlobalCustomPreferences,
            networkHelper: Lazy<NetworkHelper>,
            sourceClaims: SourceClaims,
            sourceIdentities: SourceIdentityRepository,
            @AppCoroutineScope scope: CoroutineScope,
        ): ExtensionManager = ExtensionManager(
            loader = loader,
            kinds = kinds,
            packageInstaller = packageInstaller,
            installEvents = installEvents,
            notices = notices,
            files = files,
            preferences = preferences,
            globalPreferences = globalPreferences,
            trustExtension = trustExtension,
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            extensionStoreRepository = extensionStoreRepository,
            updateExtensionStores = updateExtensionStores,
            customPreferences = customPreferences,
            networkHelper = networkHelper,
            sourceClaims = sourceClaims,
            sourceIdentities = sourceIdentities,
            scope = scope,
        )
    }
}
