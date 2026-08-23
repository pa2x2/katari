package mihon.entry.interactions.anime.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.anime.child.AnimeChildListProcessor
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProcessor
import mihon.entry.interactions.anime.library.AnimeLibraryProgressProvider
import mihon.entry.interactions.anime.media.AnimeImmersiveProcessor
import mihon.entry.interactions.anime.media.AnimeMediaCacheProvider
import mihon.entry.interactions.anime.media.AnimePlaybackPreferencesProcessor
import mihon.entry.interactions.anime.media.AnimePreviewInteraction
import mihon.entry.interactions.anime.migration.AnimeMigrationProvider
import mihon.entry.interactions.anime.navigation.AnimeContinueProcessor
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
import mihon.entry.interactions.anime.player.AnimeChildWebViewHostAdapter
import mihon.entry.interactions.anime.presentation.AnimeEntryTypePresentationProvider
import mihon.entry.interactions.anime.state.AnimeConsumptionProcessor
import mihon.entry.interactions.anime.state.AnimeProgressProcessor
import mihon.entry.interactions.anime.statistics.AnimeEntryStatisticsProvider
import mihon.entry.interactions.download.EntryBulkDownloadCandidateCapability
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.download.EntryDownloadOptionsCapability
import mihon.entry.interactions.library.EntryLibraryProgressCapability
import mihon.entry.interactions.media.EntryMediaCacheCapability
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsCapability
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueCapability
import mihon.entry.interactions.navigation.EntryOpenCapability
import mihon.entry.interactions.runtime.EntryChildListCapability
import mihon.entry.interactions.runtime.EntryChildProgressCapability
import mihon.entry.interactions.runtime.EntryImmersiveCapability
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryPreviewCapability
import mihon.entry.interactions.runtime.EntryPreviewConfigurationCapability
import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.source.EntryChildWebViewHostContribution
import mihon.entry.interactions.state.EntryConsumptionCapability
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryPlaybackPreferencesCapability
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.feature.graph.ContributionOwner
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun animeEntryInteractionPlugin(
    dependencies: AnimeEntryInteractionDependencies,
    viewerSettingsProvider: EntryViewerSettingsProvider? = null,
): EntryInteractionPlugin {
    return animeEntryInteractionPlugin(
        AnimeEntryInteractionRuntimeDependencies(
            entryChapterRepository = dependencies.entryChapterRepository,
            getEntryWithChapters = dependencies.getEntryWithChapters,
            entryProgressRepository = dependencies.entryProgressRepository,
            playbackPreferencesRepository = dependencies.playbackPreferencesRepository,
            animeDownloadManager = Injekt.get(),
            animeDownloadCache = Injekt.get(),
            downloadPreferences = dependencies.downloadPreferences,
            downloadPreferencesRepository = dependencies.downloadPreferencesRepository,
            sourceManager = dependencies.sourceManager,
            entryRepository = dependencies.entryRepository,
            mediaSession = dependencies.mediaSession,
            entryInteractionPreferences = dependencies.entryInteractionPreferences,
        ),
        viewerSettingsProvider = viewerSettingsProvider,
    )
}

internal fun animeEntryInteractionPlugin(
    dependencies: AnimeEntryInteractionRuntimeDependencies,
    viewerSettingsProvider: EntryViewerSettingsProvider? = null,
): EntryInteractionPlugin {
    val openProcessor = AnimeOpenProcessor()
    val continueProcessor = AnimeContinueProcessor(
        getEntryWithChapters = dependencies.getEntryWithChapters,
        entryProgressRepository = dependencies.entryProgressRepository,
        openProcessor = openProcessor,
    )
    val consumptionProcessor = AnimeConsumptionProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
    )
    val progressProcessor = AnimeProgressProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        entryChapterRepository = dependencies.entryChapterRepository,
    )
    val playbackPreferencesProcessor = AnimePlaybackPreferencesProcessor(
        playbackPreferencesRepository = dependencies.playbackPreferencesRepository,
    )
    val downloadProcessor = AnimeDownloadProcessor(dependencies)
    val migrationProvider = AnimeMigrationProvider()
    val childListProcessor = AnimeChildListProcessor(dependencies.entryProgressRepository)
    val libraryProgressProvider = AnimeLibraryProgressProvider(dependencies.entryProgressRepository)
    val previewProcessor = AnimePreviewInteraction(
        entryInteractionPreferences = dependencies.entryInteractionPreferences,
    )
    val immersiveProcessor = AnimeImmersiveProcessor(
        entryProgressRepository = dependencies.entryProgressRepository,
        resolveVideoStream = { Injekt.get() },
        mediaSession = dependencies.mediaSession,
    )
    return object : EntryInteractionPlugin {
        override val type = EntryType.ANIME
        override val owner = ContributionOwner("entry-interactions.anime")
        override val providerBindings = buildList {
            addAll(
                listOf(
                    EntryOpenCapability.bind(openProcessor),
                    EntryContinueCapability.bind(continueProcessor),
                    EntryConsumptionCapability.bind(consumptionProcessor),
                    EntryProgressCapability.bind(progressProcessor),
                    EntryPlaybackPreferencesCapability.bind(playbackPreferencesProcessor),
                    EntryDownloadCapability.bind(downloadProcessor),
                    EntryDownloadOptionsCapability.bind(downloadProcessor),
                    EntryBulkDownloadCandidateCapability.bind(downloadProcessor),
                    EntryMigrationCapability.bind(migrationProvider),
                    EntryChildListCapability.bind(childListProcessor),
                    EntryChildProgressCapability.bind(childListProcessor),
                    EntryLibraryProgressCapability.bind(libraryProgressProvider),
                    EntryPreviewCapability.bind(previewProcessor),
                    EntryPreviewConfigurationCapability.bind(previewProcessor),
                    EntryImmersiveCapability.bind(immersiveProcessor),
                    EntryMediaSessionCapability.bind(dependencies.mediaSession),
                    EntryTypePresentationCapability.bind(AnimeEntryTypePresentationProvider),
                    EntryStatisticsCapability.bind(AnimeEntryStatisticsProvider),
                    EntryMediaCacheCapability.bind(AnimeMediaCacheProvider { Injekt.get() }),
                ),
            )
            viewerSettingsProvider?.let { add(EntryViewerSettingsCapability.bind(it)) }
        }
        override val specializedAdapters = listOf(
            EntryChildWebViewHostContribution.bind(AnimeChildWebViewHostAdapter),
        )
    }
}

data class AnimeEntryInteractionDependencies(
    val entryChapterRepository: EntryChapterRepository,
    val getEntryWithChapters: GetEntryWithChapters,
    val entryProgressRepository: EntryProgressRepository,
    val playbackPreferencesRepository: PlaybackPreferencesRepository,
    val downloadPreferences: DownloadPreferences,
    val downloadPreferencesRepository: DownloadPreferencesRepository,
    val sourceManager: SourceManager,
    val entryRepository: EntryRepository,
    val mediaSession: EntryMediaSessionProcessor,
    val entryInteractionPreferences: EntryInteractionPreferences,
)

internal data class AnimeEntryInteractionRuntimeDependencies(
    val entryChapterRepository: EntryChapterRepository,
    val getEntryWithChapters: GetEntryWithChapters,
    val entryProgressRepository: EntryProgressRepository,
    val playbackPreferencesRepository: PlaybackPreferencesRepository,
    val animeDownloadManager: AnimeDownloadManager,
    val animeDownloadCache: AnimeDownloadCache,
    val downloadPreferences: DownloadPreferences,
    val downloadPreferencesRepository: DownloadPreferencesRepository,
    val sourceManager: SourceManager,
    val entryRepository: EntryRepository,
    val mediaSession: EntryMediaSessionProcessor,
    val entryInteractionPreferences: EntryInteractionPreferences,
)
