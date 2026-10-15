package mihon.app.di.injekt

import android.app.Application
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.GlobalSourcePreferences
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.GlobalTrackPreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.ui.translator.session.TranslatorLanguagePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.core.common.CustomPreferences
import mihon.core.common.GlobalCustomPreferences
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.common.image.progressive.ProgressiveImagePreferences
import mihon.core.metro.AppCoroutineScope
import mihon.data.database.DatabaseRepositoryImpl
import mihon.domain.database.repository.DatabaseRepository
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.feature.appupdate.AppUpdateController
import mihon.feature.appupdate.AppUpdatePreferences
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
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.statistics.service.StatisticsPreferences
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem

/**
 * Serves the app services and preferences to code that still resolves them through Injekt. Goes away with Injekt.
 * Injekt has no qualifiers, so the app coroutine scope is served as the plain `CoroutineScope` it was registered as.
 */
@BindingContainer
object AppInjektExports {
    @Provides
    @IntoSet
    fun appCoroutineScope(@AppCoroutineScope scope: () -> CoroutineScope): InjektExport<*> = injektExport(scope)

    @Provides
    @IntoSet
    fun application(x: () -> Application): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun context(x: () -> Context): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun databaseRepositoryImpl(x: () -> DatabaseRepositoryImpl): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun databaseRepository(x: () -> DatabaseRepository): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sqlDriver(x: () -> SqlDriver): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun database(x: () -> Database): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun databaseHandler(x: () -> DatabaseHandler): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationCandidateDiscovery(x: () -> SourceMigrationCandidateDiscovery): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationDiscoveryRunner(x: () -> SourceMigrationDiscoveryRunner): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationExecutionPlanner(x: () -> SourceMigrationExecutionPlanner): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationExecutionRunner(x: () -> SourceMigrationExecutionRunner): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationSessionStore(x: () -> SourceMigrationSessionStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    internal fun sourceMigrationCandidateEntryResolver(
        x: () -> SourceMigrationCandidateEntryResolver,
    ): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    internal fun sourceMigrationTargetSelector(
        x: () -> SourceMigrationTargetSelector,
    ): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceMigrationWorkScheduler(x: () -> SourceMigrationWorkScheduler): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileDatabase(x: () -> ProfileDatabase): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profilePreferenceOwnership(x: () -> ProfilePreferenceOwnership): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileManager(x: () -> ProfileManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryPreferenceProvider(x: () -> EntryPreferenceProvider): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun json(x: () -> Json): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun xML(x: () -> XML): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun protoBuf(x: () -> ProtoBuf): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun coverCache(x: () -> CoverCache): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryCustomCoverWriter(x: () -> EntryCustomCoverWriter): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryMetadataChangeNotifier(x: () -> EntryMetadataChangeNotifier): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun networkHelper(x: () -> NetworkHelper): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun javaScriptEngine(x: () -> JavaScriptEngine): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun androidStorageFolderProvider(x: () -> AndroidStorageFolderProvider): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun localSourceFileSystem(x: () -> LocalSourceFileSystem): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun localCoverManager(x: () -> LocalCoverManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun storageManager(x: () -> StorageManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceManager(x: () -> SourceManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourceInfoNames(x: () -> SourceInfoNames): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun extensionManager(x: () -> ExtensionManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun trackerManager(x: () -> TrackerManager): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun delayedTrackingStore(x: () -> DelayedTrackingStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun imageSaver(x: () -> ImageSaver): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    internal fun appUpdateController(x: () -> AppUpdateController): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun preferenceStore(x: () -> PreferenceStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profilesPreferences(x: () -> ProfilesPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileStoreImpl(x: () -> ProfileStoreImpl): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileStore(x: () -> ProfileStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileAwareStore(x: () -> ProfileAwareStore): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun activeProfileProvider(x: () -> ActiveProfileProvider): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun progressiveImagePreferences(x: () -> ProgressiveImagePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun progressiveImageEngine(x: () -> ProgressiveImageEngine): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profilePreferenceOwnerRegistry(x: () -> ProfilePreferenceOwnerRegistry): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun networkPreferences(x: () -> NetworkPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun sourcePreferences(x: () -> SourcePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun profileSourcePreferences(x: () -> ProfileSourcePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun globalSourcePreferences(x: () -> GlobalSourcePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun securityPreferences(x: () -> SecurityPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun privacyPreferences(x: () -> PrivacyPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun libraryPreferences(x: () -> LibraryPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun globalLibraryPreferences(x: () -> GlobalLibraryPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun duplicatePreferences(x: () -> DuplicatePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun upcomingPreferences(x: () -> UpcomingPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun statisticsPreferences(x: () -> StatisticsPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun updatesPreferences(x: () -> UpdatesPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun trackPreferences(x: () -> TrackPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun globalTrackPreferences(x: () -> GlobalTrackPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun downloadPreferences(x: () -> DownloadPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun entryTranslatePreferences(x: () -> EntryTranslatePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun backupPreferences(x: () -> BackupPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun storagePreferences(x: () -> StoragePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun uiPreferences(x: () -> UiPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun basePreferences(x: () -> BasePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun customPreferences(x: () -> CustomPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun translatorLanguagePreferences(x: () -> TranslatorLanguagePreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    fun globalCustomPreferences(x: () -> GlobalCustomPreferences): InjektExport<*> = injektExport(x)

    @Provides
    @IntoSet
    internal fun appUpdatePreferences(x: () -> AppUpdatePreferences): InjektExport<*> = injektExport(x)
}
