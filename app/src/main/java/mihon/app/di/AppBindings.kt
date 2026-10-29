package mihon.app.di

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.cache.AndroidCoverCacheDirectories
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.DefaultSourceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.app.localnetwork.localNetworkAccessDeniedMessage
import mihon.app.logging.LogcatLogDestinations
import mihon.app.network.AndroidNetworkConnectivity
import mihon.app.network.LocalizedWebViewInterceptorMessages
import mihon.app.storage.AndroidStorageFolderProvider
import mihon.app.storage.ContextAppPrivateDirectories
import mihon.app.ui.di.UncaughtErrorReporter
import mihon.app.ui.host.LogDestinations
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.network.NetworkConnectivity
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.data.database.DatabaseRepositoryImpl
import mihon.data.database.createSqlDriver
import mihon.domain.app.AppIdentity
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
import mihon.feature.appupdate.check.GithubReleaseSource
import mihon.feature.appupdate.install.AppUpdateInstaller
import mihon.feature.appupdate.install.UpdateApkDownloader
import mihon.feature.appupdate.install.UpdateApkVerifier
import mihon.feature.appupdate.mpvaddon.MpvAddonController
import mihon.feature.appupdate.mpvaddon.MpvAddonReleaseSource
import mihon.feature.migration.work.AndroidSourceMigrationReviewVisibility
import mihon.feature.migration.work.AndroidSourceMigrationWork
import mihon.feature.migration.work.SourceMigrationReviewVisibility
import mihon.feature.migration.work.SourceMigrationWork
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.AndroidSourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.telemetry.TelemetryConfig
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.addon.MpvAddonPackage
import mihon.video.engine.mpv.install.MpvAddonInstallation
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.storage.UniFileStorageFile
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.AndroidMediaScanner
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.io.UniFileArchiveOpener

/** Android's process-wide services: database, network, storage, sources and app update. */
@BindingContainer
interface AppBindings {
    @Binds
    val Application.context: Context

    @Binds
    val LogcatLogDestinations.logDestinations: LogDestinations

    @Binds
    val ContextAppPrivateDirectories.appPrivateDirectories: AppPrivateDirectories

    @Binds
    val AndroidNetworkConnectivity.networkConnectivity: NetworkConnectivity

    @Binds
    val CoverCache.customCoverWriter: EntryCustomCoverWriter

    @Binds
    val AndroidSourcePreferenceFileStore.sourcePreferenceFileStore: SourcePreferenceFileStore

    @Binds
    val AndroidSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility

    @Binds
    val AndroidSourceMigrationWork.sourceMigrationWork: SourceMigrationWork

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun sqlDriver(context: Context, databaseRepository: DatabaseRepositoryImpl): SqlDriver =
            createSqlDriver(context, databaseRepository)

        @Provides
        @IntoSet
        fun telemetryErrorReporter(): UncaughtErrorReporter = UncaughtErrorReporter(TelemetryConfig::recordException)

        @Provides
        fun appIdentity(): AppIdentity = AppIdentity(
            applicationId = BuildConfig.APPLICATION_ID,
            versionName = BuildConfig.VERSION_NAME,
            commitSha = BuildConfig.COMMIT_SHA,
            platformName = "Android",
        )

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
        fun coverCache(@AppCoroutineScope scope: CoroutineScope, context: Context): CoverCache =
            CoverCache(scope, AndroidCoverCacheDirectories(context))

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
            LocalSourceFileSystem { storageManager.getLocalSourceDirectory()?.let(::UniFileStorageFile) }

        @Provides
        @SingleIn(AppScope::class)
        fun localCoverManager(context: Context, fileSystem: LocalSourceFileSystem): LocalCoverManager =
            LocalCoverManager(fileSystem, AndroidMediaScanner(context))

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
            json: Json,
            xml: XML,
        ): DefaultSourceManager = DefaultSourceManager(
            extensionManager = extensionManager,
            sourceRepository = sourceRepository,
            scope = scope,
            downloadMaintenance = downloadMaintenance,
            localSource = {
                LocalSource(
                    fileSystem = localSourceFileSystem.value,
                    coverManager = localCoverManager.value,
                    archives = UniFileArchiveOpener(context),
                    json = json,
                    xml = xml,
                    localize = { context.stringResource(it) },
                )
            },
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
