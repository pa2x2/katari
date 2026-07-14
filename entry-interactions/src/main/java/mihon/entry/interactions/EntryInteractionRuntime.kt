package mihon.entry.interactions

import android.app.Application
import coil3.ComponentRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mihon.domain.chapter.interactor.FilterEntryChaptersForDownload
import mihon.entry.interactions.anime.AnimeEntryInteractionDependencies
import mihon.entry.interactions.anime.addAnimeEntryInteractionRuntime
import mihon.entry.interactions.anime.animeEntryInteractionPlugin
import mihon.entry.interactions.book.BookEntryInteractionDependencies
import mihon.entry.interactions.book.addBookEntryInteractionRuntime
import mihon.entry.interactions.book.bookEntryInteractionPlugin
import mihon.entry.interactions.manga.MangaEntryInteractionDependencies
import mihon.entry.interactions.manga.addMangaEntryInteractionRuntime
import mihon.entry.interactions.manga.mangaEntryInteractionPlugin
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.reader.settings.ReaderTrackPreferences
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.settings.DefaultViewerSettingBinder
import mihon.entry.interactions.settings.DefaultViewerSettingsInteraction
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.settings.EntryMediaCachePreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingsInteraction
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

data class EntryInteractionRuntimeDependencies(
    val activityTheme: EntryInteractionActivityTheme,
    val notificationActions: EntryDownloadNotificationActions,
    val pageImageCache: EntryPageImageCache,
    val mangaChildGroupFilterDataSource: EntryChildGroupFilterDataSource,
    val readerIncognitoState: EntryReaderIncognitoState,
    val readerTracking: EntryReaderTracking,
    val profilePreferenceStore: PreferenceStore,
    val basePreferenceStore: PreferenceStore,
    val privatePreferenceStore: PreferenceStore,
    val mediaCacheBuckets: List<EntryMediaCacheBucket> = emptyList(),
)

fun interface EntryInteractionRuntimeWarmup {
    fun warmup()
}

fun InjektRegistrar.addEntryInteractionRuntime(
    app: Application,
    dependencies: EntryInteractionRuntimeDependencies,
) {
    addSingletonFactory<EntryInteractionActivityTheme> { dependencies.activityTheme }
    addSingletonFactory<EntryDownloadNotificationActions> { dependencies.notificationActions }
    addSingletonFactory<EntryPageImageCache> { dependencies.pageImageCache }
    addSingletonFactory<EntryReaderIncognitoState> { dependencies.readerIncognitoState }
    addSingletonFactory<EntryReaderTracking> { dependencies.readerTracking }

    addSingletonFactory { MangaReaderSettingsProvider(dependencies.profilePreferenceStore) }
    addSingletonFactory { ReaderBasePreferences(dependencies.basePreferenceStore) }
    addSingletonFactory { ReaderTrackPreferences(dependencies.privatePreferenceStore) }
    addSingletonFactory { EntryInteractionPreferences(dependencies.profilePreferenceStore) }
    addSingletonFactory { AnimePlayerPreferences(dependencies.profilePreferenceStore) }
    addSingletonFactory { EntryMediaCachePreferences(dependencies.basePreferenceStore) }
    addSingletonFactory<ViewerSettingBinder> {
        DefaultViewerSettingBinder(
            overrideRepository = get<ViewerSettingOverrideRepository>(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    val mangaWarmup = addMangaEntryInteractionRuntime(app)
    val animeWarmup = addAnimeEntryInteractionRuntime(app)
    val bookRuntime = addBookEntryInteractionRuntime(app, dependencies.profilePreferenceStore)

    addSingletonFactory<ViewerSettingsInteraction> {
        DefaultViewerSettingsInteraction(
            providers = listOf(
                get<MangaReaderSettingsProvider>(),
                get<AnimePlayerPreferences>(),
            ) + bookRuntime.viewerSettingsProviders,
        )
    }

    addSingletonFactory<EntryMediaCacheMaintenance> {
        DefaultEntryMediaCacheMaintenance(
            buckets = dependencies.mediaCacheBuckets +
                bookRuntime.mediaCacheBuckets +
                LazyEntryMediaCacheBucket(
                    key = EntryMediaCacheBucketKeys.ANIME_PLAYBACK,
                    delegateProvider = { get<EntryPlayerCache>() },
                ),
        )
    }

    addSingletonFactory<EntryInteractions> {
        createEntryInteractions(
            plugins = listOf(
                mangaEntryInteractionPlugin(
                    MangaEntryInteractionDependencies(
                        getEntryWithChapters = get(),
                        entryChapterRepository = get(),
                        entryProgressRepository = get(),
                        filterEntryChaptersForDownload = FilterEntryChaptersForDownload(get(), get(), get()),
                        childGroupFilterDataSource = dependencies.mangaChildGroupFilterDataSource,
                        downloadPreferences = get(),
                        sourceManager = get(),
                        entryInteractionPreferences = get<EntryInteractionPreferences>(),
                    ),
                ),
                animeEntryInteractionPlugin(
                    AnimeEntryInteractionDependencies(
                        entryChapterRepository = get(),
                        getEntryWithChapters = get(),
                        entryProgressRepository = get(),
                        playbackPreferencesRepository = get(),
                        downloadPreferences = get(),
                        downloadPreferencesRepository = get(),
                        sourceManager = get(),
                        entryInteractionPreferences = get<EntryInteractionPreferences>(),
                        historyRepository = get(),
                    ),
                ),
                bookEntryInteractionPlugin(
                    BookEntryInteractionDependencies(
                        getEntryWithChapters = get(),
                        entryChapterRepository = get(),
                        entryProgressRepository = get(),
                        sourceManager = get(),
                    ),
                ),
            ),
        )
    }
    addSingletonFactory<EntryOpenInteraction> { get<EntryInteractions>().open }
    addSingletonFactory<EntryContinueInteraction> { get<EntryInteractions>().continueEntry }
    addSingletonFactory<EntryDownloadInteraction> { get<EntryInteractions>().download }
    addSingletonFactory<EntryCapabilityInteraction> { get<EntryInteractions>().capability }
    addSingletonFactory<EntryConsumptionInteraction> { get<EntryInteractions>().consumption }
    addSingletonFactory<EntryUpdateEligibilityInteraction> { get<EntryInteractions>().updateEligibility }
    addSingletonFactory<EntryProgressInteraction> { get<EntryInteractions>().progress }
    addSingletonFactory<EntryPlaybackPreferencesInteraction> { get<EntryInteractions>().playbackPreferences }
    addSingletonFactory<EntryChildListInteraction> { get<EntryInteractions>().childList }
    addSingletonFactory<EntryChildGroupFilterInteraction> { get<EntryInteractions>().childGroupFilter }
    addSingletonFactory<EntryLibraryFilterInteraction> { get<EntryInteractions>().libraryFilter }
    addSingletonFactory<EntryPreviewInteraction> { get<EntryInteractions>().preview }
    addSingletonFactory<EntryImmersiveInteraction> { get<EntryInteractions>().immersive }
    addSingletonFactory<EntryInteractionRuntimeWarmup> {
        EntryInteractionRuntimeWarmup {
            mangaWarmup()
            animeWarmup()
        }
    }
}

fun ComponentRegistry.Builder.addEntryInteractionImageComponents(): ComponentRegistry.Builder {
    return addMangaReaderImageComponents(this)
}

private class DefaultEntryMediaCacheMaintenance(
    buckets: List<EntryMediaCacheBucket>,
) : EntryMediaCacheMaintenance {
    private val bucketsByKey = buckets.associateBy { it.key }

    init {
        check(bucketsByKey.size == buckets.size) {
            "Duplicate entry media cache bucket keys: ${buckets.groupingBy { it.key }.eachCount().duplicates()}"
        }
    }

    override fun buckets(): List<EntryMediaCacheBucket> {
        return bucketsByKey.values.toList()
    }

    override fun bucket(key: String): EntryMediaCacheBucket? {
        return bucketsByKey[key]
    }

    override fun clear(key: String): Int {
        return bucketsByKey[key]?.clear()
            ?: error("No entry media cache bucket registered for key $key")
    }
}

private class LazyEntryMediaCacheBucket(
    override val key: String,
    private val delegateProvider: () -> EntryMediaCacheBucket,
) : EntryMediaCacheBucket {
    private val delegate: EntryMediaCacheBucket by lazy(delegateProvider)

    override val readableSize: String
        get() = delegate.readableSize

    override fun clear(): Int {
        return delegate.clear()
    }
}

private fun Map<String, Int>.duplicates(): String {
    return entries
        .filter { it.value > 1 }
        .joinToString { it.key }
}
