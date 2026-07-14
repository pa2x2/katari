package eu.kanade.tachiyomi.di

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConfiguration
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDriver
import com.eygraber.sqldelight.androidx.driver.FileProvider
import eu.kanade.domain.scanlator.interactor.GetExcludedScanlators
import eu.kanade.domain.scanlator.interactor.SetExcludedScanlators
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.MangaPageCache
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.entry.AppEntryDownloadNotificationActions
import eu.kanade.tachiyomi.entry.AppEntryMetadataUpdateHooks
import eu.kanade.tachiyomi.entry.AppEntryRemovalCleanupInteraction
import eu.kanade.tachiyomi.entry.AppMangaPageImageCache
import eu.kanade.tachiyomi.entry.AppReaderIncognitoState
import eu.kanade.tachiyomi.entry.AppReaderTracking
import eu.kanade.tachiyomi.entry.EntryRemovalCleanupInteraction
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.ui.base.delegate.ThemingDelegateImpl
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.entry.interactions.EntryChildGroupFilterDataSource
import mihon.entry.interactions.EntryInteractionActivityTheme
import mihon.entry.interactions.EntryInteractionRuntimeDependencies
import mihon.entry.interactions.EntryInteractionRuntimeWarmup
import mihon.entry.interactions.addEntryInteractionRuntime
import mihon.feature.profiles.core.EntryProfileMoveService
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileSourcePreferenceProvider
import mihon.feature.profiles.core.ProfileStore
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlConfig
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
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.EntryMetadataUpdateHooks
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
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
        addSingletonFactory { EntryProfileMoveService(get(), get()) }
        addSingletonFactory { ProfileManager(app, get(), get(), get()) }
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
        addSingletonFactory<EntryRemovalCleanupInteraction> { AppEntryRemovalCleanupInteraction(get()) }
        addSingletonFactory<EntryMetadataUpdateHooks> { AppEntryMetadataUpdateHooks(get()) }
        addSingletonFactory { NetworkHelper(app, get()) }
        addSingletonFactory { JavaScriptEngine(app) }

        addSingletonFactory<SourceManager> { AndroidSourceManager(app, get(), get()) }
        addSingletonFactory { ExtensionManager(app) }
        addSingletonFactory { TrackerManager(get(), get()) }
        addSingletonFactory { DelayedTrackingStore(app) }

        val mangaPageImageCache = AppMangaPageImageCache(get())
        addEntryInteractionRuntime(
            app = app,
            dependencies = EntryInteractionRuntimeDependencies(
                activityTheme = EntryInteractionActivityTheme(ThemingDelegateImpl()::applyAppTheme),
                notificationActions = AppEntryDownloadNotificationActions(),
                pageImageCache = mangaPageImageCache,
                mangaChildGroupFilterDataSource = object : EntryChildGroupFilterDataSource {
                    private val entryChapterRepository = get<EntryChapterRepository>()
                    private val getExcludedScanlators = get<GetExcludedScanlators>()
                    private val setExcludedScanlators = get<SetExcludedScanlators>()

                    override fun availableGroupsChanged(entryId: Long) =
                        entryChapterRepository.getScanlatorsByEntryIdAsFlow(entryId).map { Unit }

                    override suspend fun availableGroups(entryIds: Collection<Long>): Set<String> {
                        return entryIds.flatMapTo(linkedSetOf()) { entryId ->
                            entryChapterRepository.getScanlatorsByEntryId(entryId)
                        }
                    }

                    override fun excludedGroupsChanged(entryId: Long) =
                        getExcludedScanlators.subscribe(entryId).map { Unit }

                    override suspend fun excludedGroups(entryIds: Collection<Long>): Set<String> {
                        return entryIds.flatMapTo(linkedSetOf()) { entryId ->
                            getExcludedScanlators.await(entryId)
                        }
                    }

                    override suspend fun setExcludedGroups(entryIds: Collection<Long>, excluded: Set<String>) {
                        entryIds.forEach { entryId ->
                            setExcludedScanlators.await(entryId, excluded)
                        }
                    }
                },
                readerIncognitoState = AppReaderIncognitoState(get()),
                readerTracking = AppReaderTracking(get(), get()),
                profilePreferenceStore = get<ProfileStore>().profileStore(),
                basePreferenceStore = get<ProfileStore>().basePreferenceStore(),
                privatePreferenceStore = get<ProfileStore>().privateStore(),
                mediaCacheBuckets = listOf(mangaPageImageCache),
            ),
        )

        addSingletonFactory { ImageSaver(app) }

        addSingletonFactory { AndroidStorageFolderProvider(app) }
        addSingletonFactory { LocalSourceFileSystem(get()) }
        addSingletonFactory { LocalCoverManager(app, get()) }
        addSingletonFactory { StorageManager(app, get()) }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()

            get<SourceManager>()

            get<Database>()

            get<EntryInteractionRuntimeWarmup>().warmup()
        }
    }
}
