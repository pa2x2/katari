package mihon.app.di

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.entry.AppEntryMetadataChangeNotifier
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.claims.SourceClaims
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.source.getDisplayNameForEntryInfo
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.core.metro.AppCoroutineScope
import mihon.data.database.DatabaseRepositoryImpl
import mihon.data.database.createSqlDriver
import mihon.domain.database.repository.DatabaseRepository
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.lifecycle.metadata.EntryMetadataLifecycleFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.MpvAddonReleaseSource
import mihon.feature.migration.discovery.SourceMigrationCandidateDiscovery
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunner
import mihon.feature.migration.execution.SourceMigrationExecutionPlanner
import mihon.feature.migration.execution.SourceMigrationExecutionRunner
import mihon.feature.migration.review.SourceMigrationCandidateEntryResolver
import mihon.feature.migration.review.SourceMigrationTargetSelector
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.work.SourceMigrationWorkScheduler
import mihon.feature.profiles.core.ProfileAwareStore
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfilePreferenceOwnership
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.ProfileStoreImpl
import mihon.feature.profiles.core.ProfilesPreferences
import mihon.feature.profiles.sourcepreferences.ProfileSourcePreferenceProvider
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.telemetry.TelemetryConfig
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonPackage
import mihon.video.engine.mpv.install.MpvAddonInstallation
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

/** The app's process-wide services: database, serialization, storage, sources, extensions and app update. */
@BindingContainer
interface AppBindings {
    @Binds
    val Application.context: Context

    @Binds
    val DatabaseRepositoryImpl.databaseRepository: DatabaseRepository

    @Binds
    val AndroidDatabaseHandler.databaseHandler: DatabaseHandler

    @Binds
    val CoverCache.customCoverWriter: EntryCustomCoverWriter

    @Binds
    val AppEntryMetadataChangeNotifier.metadataChangeNotifier: EntryMetadataChangeNotifier

    @Binds
    val AndroidSourceManager.sourceManager: SourceManager

    @Binds
    val ProfileSourcePreferenceProvider.entryPreferenceProvider: EntryPreferenceProvider

    companion object {
        // The driver reports its migration state to the repository, which reads the driver back lazily.
        @Provides
        @SingleIn(AppScope::class)
        fun databaseRepositoryImpl(driver: Lazy<SqlDriver>): DatabaseRepositoryImpl = DatabaseRepositoryImpl(driver)

        @Provides
        @SingleIn(AppScope::class)
        fun sqlDriver(context: Context, databaseRepository: DatabaseRepositoryImpl): SqlDriver =
            createSqlDriver(context, databaseRepository)

        @Provides
        @SingleIn(AppScope::class)
        fun database(driver: SqlDriver): Database = Database(
            driver = driver,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(
                memoAdapter = MemoColumnAdapter,
            ),
            historyAdapter = History.Adapter(
                last_readAdapter = DateColumnAdapter,
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun databaseHandler(database: Database, driver: SqlDriver): AndroidDatabaseHandler =
            AndroidDatabaseHandler(database, driver)

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
        fun sourceMigrationCandidateDiscovery(catalogue: EntryCatalogueFeature): SourceMigrationCandidateDiscovery =
            SourceMigrationCandidateDiscovery(catalogue)

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationDiscoveryRunner(
            store: SourceMigrationSessionStore,
            discovery: SourceMigrationCandidateDiscovery,
            entryRepository: EntryRepository,
            networkToLocalEntry: NetworkToLocalEntry,
            migration: EntryMigrationFeature,
        ): SourceMigrationDiscoveryRunner = SourceMigrationDiscoveryRunner(
            store = store,
            discovery = discovery,
            entryRepository = entryRepository,
            networkToLocalEntry = networkToLocalEntry,
            migration = migration,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationExecutionPlanner(
            grouping: EntryMergeLibraryGroupingFeature,
            entryRepository: EntryRepository,
        ): SourceMigrationExecutionPlanner = SourceMigrationExecutionPlanner(grouping, entryRepository)

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationExecutionRunner(
            store: SourceMigrationSessionStore,
            entryRepository: EntryRepository,
            migration: EntryMigrationFeature,
        ): SourceMigrationExecutionRunner = SourceMigrationExecutionRunner(store, entryRepository, migration)

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationSessionStore(handler: DatabaseHandler): SourceMigrationSessionStore =
            SourceMigrationSessionStore(handler)

        @Provides
        @SingleIn(AppScope::class)
        internal fun sourceMigrationCandidateEntryResolver(
            entryRepository: EntryRepository,
            networkToLocalEntry: NetworkToLocalEntry,
        ): SourceMigrationCandidateEntryResolver = SourceMigrationCandidateEntryResolver(
            entryRepository,
            networkToLocalEntry,
        )

        @Provides
        @SingleIn(AppScope::class)
        internal fun sourceMigrationTargetSelector(
            sessionStore: SourceMigrationSessionStore,
            candidateEntryResolver: SourceMigrationCandidateEntryResolver,
            migration: EntryMigrationFeature,
        ): SourceMigrationTargetSelector = SourceMigrationTargetSelector(
            sessionStore,
            candidateEntryResolver,
            migration,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun sourceMigrationWorkScheduler(
            context: Context,
            store: SourceMigrationSessionStore,
            executionPlanner: SourceMigrationExecutionPlanner,
        ): SourceMigrationWorkScheduler = SourceMigrationWorkScheduler(context, store, executionPlanner)

        @Provides
        @SingleIn(AppScope::class)
        fun profileDatabase(handler: DatabaseHandler): ProfileDatabase = ProfileDatabase(handler)

        @Provides
        @SingleIn(AppScope::class)
        fun profilePreferenceOwnership(owners: ProfilePreferenceOwnerRegistry): ProfilePreferenceOwnership =
            ProfilePreferenceOwnership(owners)

        @Provides
        @SingleIn(AppScope::class)
        fun profileManager(
            application: Application,
            profileDatabase: ProfileDatabase,
            profileStore: ProfileStoreImpl,
            sourcePreferenceFiles: SourcePreferenceFiles,
            profilesPreferences: ProfilesPreferences,
            extensionManager: ExtensionManager,
            preferenceOwnership: ProfilePreferenceOwnership,
            entryRepository: EntryRepository,
            destructiveRemoval: EntryDestructiveRemovalFeature,
            @AppCoroutineScope appScope: CoroutineScope,
        ): ProfileManager = ProfileManager(
            application = application,
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            sourcePreferenceFiles = sourcePreferenceFiles,
            profilesPreferences = profilesPreferences,
            extensionManager = extensionManager,
            preferenceOwnership = preferenceOwnership,
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
            appScope = appScope,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferenceFiles(
            application: Application,
            profileStore: ProfileStore,
            profileDatabase: ProfileDatabase,
            identities: SourceIdentityRepository,
            @AppCoroutineScope appScope: CoroutineScope,
        ): SourcePreferenceFiles = SourcePreferenceFiles(
            application,
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
            CoverCache(scope, context)

        @Provides
        @SingleIn(AppScope::class)
        fun metadataChangeNotifier(metadataLifecycle: EntryMetadataLifecycleFeature): AppEntryMetadataChangeNotifier =
            AppEntryMetadataChangeNotifier(metadataLifecycle)

        // Not constructor-injected: production validation graphs substitute a controlled network.
        @Provides
        @SingleIn(AppScope::class)
        fun networkHelper(context: Context, preferences: NetworkPreferences): NetworkHelper =
            NetworkHelper(context, preferences)

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
        fun androidSourceManager(
            context: Context,
            extensionManager: ExtensionManager,
            sourceRepository: StubSourceRepository,
            @AppCoroutineScope scope: CoroutineScope,
            downloadMaintenance: Lazy<EntryDownloadMaintenanceFeature>,
            localSourceFileSystem: Lazy<LocalSourceFileSystem>,
            localCoverManager: Lazy<LocalCoverManager>,
        ): AndroidSourceManager = AndroidSourceManager(
            context = context,
            extensionManager = extensionManager,
            sourceRepository = sourceRepository,
            scope = scope,
            downloadMaintenance = downloadMaintenance,
            localSourceFileSystem = localSourceFileSystem,
            localCoverManager = localCoverManager,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun sourceInfoNames(sourceManager: SourceManager, preferences: SourcePreferences): SourceInfoNames =
            SourceInfoNames { sourceId ->
                sourceManager.getDisplayInfo(sourceId).getDisplayNameForEntryInfo(preferences)
            }

        @Provides
        @SingleIn(AppScope::class)
        fun extensionManager(
            context: Context,
            preferences: SourcePreferences,
            globalPreferences: GlobalSourcePreferences,
            trustExtension: TrustExtension,
            profileDatabase: ProfileDatabase,
            profileStore: ProfileAwareStore,
            extensionStoreRepository: ExtensionStoreRepository,
            updateExtensionStores: UpdateExtensionStores,
            customPreferences: GlobalCustomPreferences,
            basePreferences: BasePreferences,
            securityPreferences: SecurityPreferences,
            networkHelper: Lazy<NetworkHelper>,
            sourceClaims: SourceClaims,
            @AppCoroutineScope scope: CoroutineScope,
        ): ExtensionManager = ExtensionManager(
            context = context,
            preferences = preferences,
            globalPreferences = globalPreferences,
            trustExtension = trustExtension,
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            extensionStoreRepository = extensionStoreRepository,
            updateExtensionStores = updateExtensionStores,
            customPreferences = customPreferences,
            basePreferences = basePreferences,
            securityPreferences = securityPreferences,
            networkHelper = networkHelper,
            sourceClaims = sourceClaims,
            scope = scope,
        )

        @Provides
        @SingleIn(AppScope::class)
        fun delayedTrackingStore(context: Context): DelayedTrackingStore = DelayedTrackingStore(context)

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
