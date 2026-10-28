package mihon.app.di

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.cache.AndroidCoverCacheDirectories
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.entry.AppEntryMetadataChangeNotifier
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.claims.SourceClaims
import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.platform.ExtensionFiles
import eu.kanade.tachiyomi.extension.platform.ExtensionInstallEvents
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import eu.kanade.tachiyomi.extension.platform.ExtensionPackageInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.DefaultSourceManager
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.app.localnetwork.localNetworkAccessDeniedMessage
import mihon.app.network.LocalizedWebViewInterceptorMessages
import mihon.app.storage.AndroidStorageFolderProvider
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.core.metro.AppCoroutineScope
import mihon.data.database.DatabaseRepositoryImpl
import mihon.data.database.createSqlDriver
import mihon.domain.app.AppIdentity
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.MpvAddonReleaseSource
import mihon.feature.migration.execution.SourceMigrationExecutionPlanner
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.work.AndroidSourceMigrationReviewVisibility
import mihon.feature.migration.work.AndroidSourceMigrationWorkScheduler
import mihon.feature.migration.work.SourceMigrationReviewVisibility
import mihon.feature.migration.work.SourceMigrationWorkScheduler
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.AndroidSourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.ProfileSourcePreferenceProvider
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.telemetry.TelemetryConfig
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonPackage
import mihon.video.engine.mpv.install.MpvAddonInstallation
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.EntryInfoSourceNames
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

/** The app's process-wide services: database, serialization, storage, sources, extensions and app update. */
@BindingContainer
interface AppBindings {
    @Binds
    val Application.context: Context

    @Binds
    val CoverCache.customCoverWriter: EntryCustomCoverWriter

    @Binds
    val AppEntryMetadataChangeNotifier.metadataChangeNotifier: EntryMetadataChangeNotifier

    @Binds
    val DefaultSourceManager.sourceManager: SourceManager

    @Binds
    val EntryInfoSourceNames.sourceInfoNames: SourceInfoNames

    @Binds
    val ProfileSourcePreferenceProvider.entryPreferenceProvider: EntryPreferenceProvider

    @Binds
    val AndroidSourcePreferenceFileStore.sourcePreferenceFileStore: SourcePreferenceFileStore

    @Binds
    val AndroidSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun sqlDriver(context: Context, databaseRepository: DatabaseRepositoryImpl): SqlDriver =
            createSqlDriver(context, databaseRepository)

        @Provides
        @SingleIn(AppScope::class)
        @AppCoroutineScope
        fun appCoroutineScope(): CoroutineScope {
            // Work here outlives the screen that started it, so a failure would crash the app on whatever screen is
            // open by then. Debug builds still crash so the bug gets noticed.
            val handler = CoroutineExceptionHandler { _, throwable ->
                if (isDebugBuildType) {
                    val thread = Thread.currentThread()
                    thread.uncaughtExceptionHandler?.uncaughtException(thread, throwable)
                } else {
                    logcat(LogPriority.ERROR, throwable) { "Uncaught exception in the app scope" }
                    TelemetryConfig.recordException(throwable)
                }
            }
            return CoroutineScope(SupervisorJob() + Dispatchers.IO + handler)
        }

        @Provides
        fun appIdentity(): AppIdentity = AppIdentity(
            applicationId = BuildConfig.APPLICATION_ID,
            versionName = BuildConfig.VERSION_NAME,
            commitSha = BuildConfig.COMMIT_SHA,
            platformName = "Android",
        )

        @Provides
        @SingleIn(AppScope::class)
        fun json(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        @Provides
        @SingleIn(AppScope::class)
        fun xml(): XML = XML.v1 {
            policy {
                ignoreUnknownChildren()
                autoPolymorphic = true
            }
            xmlDeclMode = XmlDeclMode.Charset
            xmlVersion = XmlVersion.XML10
            setIndent(2)
        }

        @Provides
        @SingleIn(AppScope::class)
        fun protoBuf(): ProtoBuf = ProtoBuf

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationWorkScheduler(
            context: Context,
            store: SourceMigrationSessionStore,
            executionPlanner: SourceMigrationExecutionPlanner,
        ): SourceMigrationWorkScheduler = AndroidSourceMigrationWorkScheduler(context, store, executionPlanner)

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferenceFiles(
            fileStore: SourcePreferenceFileStore,
            profileStore: ProfileStore,
            profileDatabase: ProfileDatabase,
            identities: SourceIdentityRepository,
            @AppCoroutineScope appScope: CoroutineScope,
        ): SourcePreferenceFiles = SourcePreferenceFiles(
            fileStore,
            profileStore,
            profileDatabase,
            identities,
            appScope,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun profileSourcePreferenceProvider(files: SourcePreferenceFiles): ProfileSourcePreferenceProvider =
            ProfileSourcePreferenceProvider(files)

        @Provides
        @SingleIn(AppScope::class)
        fun coverCache(@AppCoroutineScope scope: CoroutineScope, context: Context): CoverCache =
            CoverCache(scope, AndroidCoverCacheDirectories(context))

        @Provides
        @SingleIn(AppScope::class)
        fun metadataChangeNotifier(metadataLifecycle: EntryMetadataLifecycleFeature): AppEntryMetadataChangeNotifier =
            AppEntryMetadataChangeNotifier(metadataLifecycle)

        // Not constructor-injected: production validation graphs substitute a controlled network.
        @Provides
        @SingleIn(AppScope::class)
        fun networkHelper(context: Context, preferences: NetworkPreferences): NetworkHelper = NetworkHelper(
            context = context,
            preferences = preferences,
            webViewMessages = LocalizedWebViewInterceptorMessages(context),
            localNetworkAccessDeniedMessage = context::localNetworkAccessDeniedMessage,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun storageFolderProvider(
            context: Context,
        ): AndroidStorageFolderProvider = AndroidStorageFolderProvider(context)

        @Provides
        @SingleIn(AppScope::class)
        fun localSourceFileSystem(storageManager: StorageManager): LocalSourceFileSystem =
            LocalSourceFileSystem(storageManager)

        @Provides
        @SingleIn(AppScope::class)
        fun localCoverManager(context: Context, fileSystem: LocalSourceFileSystem): LocalCoverManager =
            LocalCoverManager(context, fileSystem)

        @Provides
        @SingleIn(AppScope::class)
        fun storageManager(
            context: Context,
            storagePreferences: StoragePreferences,
            @AppCoroutineScope scope: CoroutineScope,
        ): StorageManager = StorageManager(context, storagePreferences, scope)

        @Provides
        @SingleIn(AppScope::class)
        fun defaultSourceManager(
            context: Context,
            extensionManager: ExtensionManager,
            sourceRepository: StubSourceRepository,
            @AppCoroutineScope scope: CoroutineScope,
            downloadMaintenance: Lazy<EntryDownloadMaintenanceFeature>,
            localSourceFileSystem: Lazy<LocalSourceFileSystem>,
            localCoverManager: Lazy<LocalCoverManager>,
        ): DefaultSourceManager = DefaultSourceManager(
            extensionManager = extensionManager,
            sourceRepository = sourceRepository,
            scope = scope,
            downloadMaintenance = downloadMaintenance,
            localSource = { LocalSource(context, localSourceFileSystem.value, localCoverManager.value) },
        )

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

        @Provides
        @SingleIn(AppScope::class)
        fun imageSaver(context: Context): ImageSaver = ImageSaver(context)

        @Provides
        @SingleIn(AppScope::class)
        fun progressiveImageEngine(preferences: ProgressiveImagePreferences): ProgressiveImageEngine =
            ProgressiveImageEngine(preferences)

        @Provides
        @SingleIn(AppScope::class)
        internal fun appUpdateController(
            context: Context,
            preferences: AppUpdatePreferences,
            network: NetworkHelper,
            json: Json,
            mpvAddon: MpvAddon,
            mpvAddonSource: MpvAddonReleaseSource,
            mpvAddonController: MpvAddonController,
            @AppCoroutineScope scope: CoroutineScope,
        ): AppUpdateController = AppUpdateController(
            preferences = preferences,
            source = GithubReleaseSource(network, json),
            downloader = UpdateApkDownloader(context, network, UpdateApkDownloader.APP_DIRECTORY),
            verifier = UpdateApkVerifier(context),
            installer = AppUpdateInstaller(context),
            mpvAddon = mpvAddon,
            mpvAddonSource = mpvAddonSource,
            mpvAddonController = mpvAddonController,
            scope = scope,
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun mpvAddonReleaseSource(network: NetworkHelper, json: Json): MpvAddonReleaseSource =
            MpvAddonReleaseSource(GithubReleaseSource(network, json), network, json)

        @Provides
        @SingleIn(AppScope::class)
        internal fun mpvAddonController(
            context: Context,
            network: NetworkHelper,
            mpvAddon: MpvAddon,
            source: MpvAddonReleaseSource,
            preferences: AppUpdatePreferences,
            @AppCoroutineScope scope: CoroutineScope,
        ): MpvAddonController = MpvAddonController(
            addon = mpvAddon,
            source = source,
            downloader = UpdateApkDownloader(context, network, UpdateApkDownloader.MPV_ADDON_DIRECTORY),
            verifier = UpdateApkVerifier(context),
            installer = AppUpdateInstaller(context, MpvAddonPackage.NAME),
            preferences = preferences,
            scope = scope,
        )

        @Provides
        internal fun mpvAddonInstallation(controller: MpvAddonController): MpvAddonInstallation = controller
    }
}
