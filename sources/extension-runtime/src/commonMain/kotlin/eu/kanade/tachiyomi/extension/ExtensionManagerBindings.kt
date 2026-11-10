package eu.kanade.tachiyomi.extension

import app.katari.core.common.GlobalCustomPreferences
import app.katari.core.metro.AppCoroutineScope
import app.katari.domain.extension.interactor.UpdateExtensionStores
import app.katari.domain.extension.repository.ExtensionStoreRepository
import app.katari.profiles.core.ProfileDatabase
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.claims.SourceClaims
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionHostStart
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.SourceRepository

/** The extension manager, built on the platform's answers to what it asks of the platform. */
@ContributesTo(AppScope::class)
@BindingContainer
object ExtensionManagerBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun extensionManager(
        loader: ExtensionLoader,
        hostStart: ExtensionHostStart,
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
        sourceRepository: Lazy<SourceRepository>,
        @AppCoroutineScope scope: CoroutineScope,
    ): ExtensionManager = ExtensionManager(
        loader = loader,
        hostStart = hostStart,
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
        sourceRepository = sourceRepository,
        scope = scope,
    )
}
