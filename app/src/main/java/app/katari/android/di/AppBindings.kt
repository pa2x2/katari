package app.katari.android.di

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.katari.android.localnetwork.localNetworkAccessDeniedMessage
import app.katari.android.logging.LogcatLogDestinations
import app.katari.android.network.AndroidNetworkConnectivity
import app.katari.android.network.LocalizedWebViewInterceptorMessages
import app.katari.android.power.AndroidPowerSource
import app.katari.android.storage.AndroidStorageFolderProvider
import app.katari.android.storage.ContextAppPrivateDirectories
import app.katari.appupdate.AndroidAppUpdatePlatform
import app.katari.appupdate.AppUpdatePlatform
import app.katari.appupdate.AppUpdatePreferences
import app.katari.appupdate.check.GithubReleaseSource
import app.katari.appupdate.download.UpdateDownloader
import app.katari.appupdate.install.AppUpdateInstaller
import app.katari.appupdate.install.UpdateApkVerifier
import app.katari.appupdate.mpvaddon.MpvAddonController
import app.katari.appupdate.mpvaddon.MpvAddonReleaseSource
import app.katari.appupdate.mpvaddon.MpvAddonUpdatePreferences
import app.katari.core.archive.AndroidArchiveOpener
import app.katari.core.archive.ArchiveOpener
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.power.PowerSource
import app.katari.core.platform.storage.AppPrivateDirectories
import app.katari.data.database.DatabaseRepositoryImpl
import app.katari.data.database.TemporarySchema
import app.katari.data.database.createSqlDriver
import app.katari.domain.app.AppIdentity
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.profiles.core.ProfileDatabase
import app.katari.profiles.core.ProfileStore
import app.katari.profiles.sourcepreferences.AndroidSourcePreferenceFileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import app.katari.sourcemigration.work.AndroidSourceMigrationReviewVisibility
import app.katari.sourcemigration.work.AndroidSourceMigrationWork
import app.katari.sourcemigration.work.SourceMigrationReviewVisibility
import app.katari.sourcemigration.work.SourceMigrationWork
import app.katari.ui.host.LogDestinations
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.addon.MpvAddonPackage
import app.katari.video.engine.mpv.install.MpvAddonInstallation
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
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.storage.AndroidMediaScanner
import tachiyomi.core.common.storage.MediaScanner
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.core.common.storage.UniFileStorageFileResolver
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.File

/** Android's process-wide services: database, network, storage, sources and app update. */
@BindingContainer
interface AppBindings {
    @Binds
    val AndroidAppUpdatePlatform.appUpdatePlatform: AppUpdatePlatform

    @Binds
    val Application.context: Context

    @Binds
    val LogcatLogDestinations.logDestinations: LogDestinations

    @Binds
    val ContextAppPrivateDirectories.appPrivateDirectories: AppPrivateDirectories

    @Binds
    val AndroidNetworkConnectivity.networkConnectivity: NetworkConnectivity

    @Binds
    val AndroidPowerSource.powerSource: PowerSource

    @Binds
    val AndroidSourcePreferenceFileStore.sourcePreferenceFileStore: SourcePreferenceFileStore

    @Binds
    val AndroidSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility

    @Binds
    val AndroidSourceMigrationWork.sourceMigrationWork: SourceMigrationWork

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun sqlDriver(
            context: Context,
            databaseRepository: DatabaseRepositoryImpl,
            temporarySchema: TemporarySchema,
        ): SqlDriver = createSqlDriver(context, databaseRepository, temporarySchema)

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
        fun storageFileResolver(context: Context): StorageFileResolver = UniFileStorageFileResolver(context)

        @Provides
        fun mediaScanner(context: Context): MediaScanner = AndroidMediaScanner(context)

        @Provides
        fun archiveOpener(context: Context): ArchiveOpener = AndroidArchiveOpener(context)

        @Provides
        @SingleIn(AppScope::class)
        fun storageManager(
            storageFiles: StorageFileResolver,
            mediaScanner: MediaScanner,
            storagePreferences: StoragePreferences,
            @AppCoroutineScope scope: CoroutineScope,
        ): StorageManager = StorageManager(storageFiles, mediaScanner, storagePreferences, scope)

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
            archives: ArchiveOpener,
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
                    archives = archives,
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
        internal fun appUpdatePlatform(
            context: Context,
            network: NetworkHelper,
            mpvAddon: MpvAddon,
            mpvAddonSource: MpvAddonReleaseSource,
            mpvAddonController: MpvAddonController,
        ): AndroidAppUpdatePlatform = AndroidAppUpdatePlatform(
            context = context,
            downloader = UpdateDownloader(File(context.cacheDir, "app-update"), network),
            verifier = UpdateApkVerifier(context),
            installer = AppUpdateInstaller(context),
            mpvAddon = mpvAddon,
            mpvAddonSource = mpvAddonSource,
            mpvAddonController = mpvAddonController,
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
            addonPreferences: MpvAddonUpdatePreferences,
            @AppCoroutineScope scope: CoroutineScope,
        ): MpvAddonController = MpvAddonController(
            addon = mpvAddon,
            source = source,
            downloader = UpdateDownloader(File(context.cacheDir, "mpv-addon"), network),
            verifier = UpdateApkVerifier(context),
            installer = AppUpdateInstaller(context, MpvAddonPackage.NAME),
            preferences = preferences,
            addonPreferences = addonPreferences,
            scope = scope,
        )

        @Provides
        internal fun mpvAddonInstallation(controller: MpvAddonController): MpvAddonInstallation = controller
    }
}
