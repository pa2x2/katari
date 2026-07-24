package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConfiguration
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDriver
import com.eygraber.sqldelight.androidx.driver.FileProvider
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.presentation.more.settings.screen.productionEntryViewerSettingsScreenProjectionResolver
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.MangaPageCache
import eu.kanade.tachiyomi.data.entry.AppEntryChildGroupFilterDataSource
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.entry.AppEntryMetadataChangeNotifier
import eu.kanade.tachiyomi.entry.AppMangaPageImageCache
import eu.kanade.tachiyomi.entry.AppMediaSessionIncognitoState
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.entry.interactions.EntryInteractionActivityTheme
import mihon.entry.interactions.EntryInteractionRuntimeDependencies
import mihon.entry.interactions.EntryInteractionRuntimeWarmup
import mihon.entry.interactions.addEntryInteractionRuntime
import mihon.entry.interactions.host.AppEntryMergeDuplicateCandidateHost
import mihon.entry.interactions.host.AppEntryMergeHost
import mihon.entry.interactions.host.AppEntryMigrationCustomCoverHost
import mihon.entry.interactions.host.AppEntryMigrationHost
import mihon.entry.interactions.host.library.AppEntryLibraryCustomCoverHost
import mihon.entry.interactions.host.library.AppEntryLibraryMembershipHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveCustomCoverHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.host.lifecycle.profile.AppEntryProfileMoveTrackingStateHost
import mihon.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.host.lifecycle.removal.AppEntryDestructiveRemovalHost
import mihon.entry.interactions.host.tracking.AppEntryTrackingHost
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileSourcePreferenceProvider
import mihon.feature.profiles.core.ProfileStore
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
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
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.EntryMetadataChangeNotifier
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import java.lang.ref.WeakReference

private val lock = Any()

class AppModule(val app: Application) : InjektModule {

    private var sqlDriverRef: WeakReference<SqlDriver>? = null

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory<SqlDriver> {
            synchronized(lock) {
                sqlDriverRef?.get()?.let { return@synchronized it }

                AndroidxSqliteDriver(
                    driver = BundledSQLiteDriver(),
                    databaseType = AndroidxSqliteDatabaseType.FileProvider(app, "tachiyomi.db"),
                    schema = Database.Schema,
                    configuration = AndroidxSqliteConfiguration(
                        isForeignKeyConstraintsEnabled = true,
                    ),
                )
                    .also { sqlDriverRef = WeakReference(it) }
            }
        }
        addSingletonFactory {
            Database(
                driver = get(),
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
        }
        addSingletonFactory<DatabaseHandler> { AndroidDatabaseHandler(get(), get()) }
        addSingletonFactory { ProfileDatabase(get()) }
        addSingletonFactory { mihon.feature.profiles.core.ProfilePreferenceOwnership(get()) }
        addSingletonFactory {
            ProfileManager(
                application = app,
                profileDatabase = get(),
                profileStore = get(),
                profilesPreferences = get(),
                extensionManager = get(),
                preferenceOwnership = get(),
                entryRepository = get(),
                destructiveRemoval = get(),
            )
        }
        addSingletonFactory<EntryPreferenceProvider> { ProfileSourcePreferenceProvider(app, get()) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
        addSingletonFactory<XML> {
            XML.v1 {
                policy {
                    ignoreUnknownChildren()
                    autoPolymorphic = true
                }
                xmlDeclMode = XmlDeclMode.Charset
                xmlVersion = XmlVersion.XML10
                setIndent(2)
            }
        }
        addSingletonFactory<ProtoBuf> {
            ProtoBuf
        }

        addSingletonFactory { MangaPageCache(app, get()) }
        addSingletonFactory { CoverCache(app) }
        addSingletonFactory<EntryMetadataChangeNotifier> { AppEntryMetadataChangeNotifier(get()) }
        addSingletonFactory { NetworkHelper(app, get()) }
        addSingletonFactory { JavaScriptEngine(app) }

        addSingletonFactory { AndroidStorageFolderProvider(app) }
        addSingletonFactory { LocalSourceFileSystem(get()) }
        addSingletonFactory { LocalCoverManager(app, get()) }
        addSingletonFactory { StorageManager(app, get()) }

        addSingletonFactory<SourceManager> { AndroidSourceManager(app, get(), get()) }
        addSingletonFactory { ExtensionManager(app) }
        addSingletonFactory { TrackerManager(get(), get()) }
        addSingletonFactory { DelayedTrackingStore(app) }

        val mangaPageImageCache = AppMangaPageImageCache(get())
        val mergeHost = AppEntryMergeHost(
            handler = get(),
            duplicateCandidates = AppEntryMergeDuplicateCandidateHost(get(), get<DuplicatePreferences>()),
            defaultChildFlags = {
                val preferences = get<LibraryPreferences>()
                Entry.SHOW_ALL or
                    preferences.sortChapterBySourceOrNumber.get() or
                    preferences.displayChapterByNameOrNumber.get() or
                    preferences.sortChapterByAscendingOrDescending.get()
            },
        )
        val migrationHost = AppEntryMigrationHost(
            handler = get(),
            hasCustomCover = { entryId -> get<CoverCache>().getCustomCoverFile(entryId).exists() },
        )
        val migrationCustomCoverHost = AppEntryMigrationCustomCoverHost(app, get())
        val libraryMembershipHost = AppEntryLibraryMembershipHost(get(), get<ProfileStore>())
        val libraryCustomCoverHost = AppEntryLibraryCustomCoverHost(get(), get())
        val destructiveRemovalHost = AppEntryDestructiveRemovalHost(get())
        val destructiveRemovalCustomCoverHost = AppEntryDestructiveRemovalCustomCoverHost(get())
        val profileMoveHost = AppEntryProfileMoveHost(get())
        val profileMoveSourceVisibilityHost = AppEntryProfileMoveSourceVisibilityHost(get())
        val profileMoveCustomCoverHost = AppEntryProfileMoveCustomCoverHost(get())
        val profileMoveTrackingStateHost = AppEntryProfileMoveTrackingStateHost(get())
        val profileMoveChildGroupFilterStateHost = AppEntryProfileMoveChildGroupFilterStateHost(get())
        val profileMoveCoverHashStateHost = AppEntryProfileMoveCoverHashStateHost(get())
        val trackingHost = AppEntryTrackingHost(
            trackerManager = get(),
            sourceManager = get(),
            getTracks = get(),
            handler = get(),
            getTracksPerEntry = get(),
            refreshTracks = get(),
            deleteTrack = get(),
            app = app,
            addTracks = get(),
            trackChapter = get(),
            syncChapterProgress = { get() },
            trackPreferences = get(),
        )
        addEntryInteractionRuntime(
            app = app,
            dependencies = EntryInteractionRuntimeDependencies(
                activityTheme = EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme),
                notificationActions = AppEntryDownloadNotificationActions(),
                pageImageCache = mangaPageImageCache,
                childGroupFilterDataSource = AppEntryChildGroupFilterDataSource(get(), get(), get()),
                mediaSessionIncognitoState = AppMediaSessionIncognitoState(get()),
                basePreferenceStore = get<ProfileStore>().basePreferenceStore(),
                profilePreferenceOwners = ProfilePreferenceOwnerInstaller(get()) {
                    get<ProfileStore>().profileStore()
                },
                viewerSettingsScreenProjectionResolver = productionEntryViewerSettingsScreenProjectionResolver(),
                sourceRefreshUpdateLibraryTitles = { profileId ->
                    LibraryPreferences(get<ProfileStore>().profileStore(profileId)).updateMangaTitles.get()
                },
                libraryMembershipHost = libraryMembershipHost,
                libraryCustomCoverHost = libraryCustomCoverHost,
                destructiveRemovalHost = destructiveRemovalHost,
                destructiveRemovalCustomCoverHost = destructiveRemovalCustomCoverHost,
                profileMoveHost = profileMoveHost,
                profileMoveSourceVisibilityHost = profileMoveSourceVisibilityHost,
                profileMoveCustomCoverHost = profileMoveCustomCoverHost,
                profileMoveTrackingStateHost = profileMoveTrackingStateHost,
                profileMoveChildGroupFilterStateHost = profileMoveChildGroupFilterStateHost,
                profileMoveCoverHashStateHost = profileMoveCoverHashStateHost,
                mergeHost = mergeHost,
                mergeCoverCleanup = { entry ->
                    libraryCustomCoverHost.cleanupAfterLibraryRemoval(entry)
                },
                migrationPreparationHost = migrationHost,
                migrationExecutionHost = migrationHost,
                migrationConsequenceHost = migrationHost,
                migrationCustomCoverHost = migrationCustomCoverHost,
                trackingHost = trackingHost,
            ),
        )

        addSingletonFactory { ImageSaver(app) }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()

            get<SourceManager>()

            get<Database>()

            get<EntryInteractionRuntimeWarmup>().warmup()
        }
    }
}
