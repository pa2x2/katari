package mihon.entry.interactions.anime

import mihon.entry.interactions.EntryInteractionPlugin
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.entry.repository.PlaybackStateRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun animeEntryInteractionPlugin(
    dependencies: AnimeEntryInteractionDependencies,
): EntryInteractionPlugin {
    return animeEntryInteractionPlugin(
        AnimeEntryInteractionRuntimeDependencies(
            entryChapterRepository = dependencies.entryChapterRepository,
            getEntryWithChapters = dependencies.getEntryWithChapters,
            playbackStateRepository = dependencies.playbackStateRepository,
            playbackPreferencesRepository = dependencies.playbackPreferencesRepository,
            animeDownloadManager = Injekt.get(),
            animeDownloadCache = Injekt.get(),
            downloadPreferences = dependencies.downloadPreferences,
            downloadPreferencesRepository = dependencies.downloadPreferencesRepository,
            sourceManager = dependencies.sourceManager,
            entryInteractionPreferences = dependencies.entryInteractionPreferences,
            historyRepository = dependencies.historyRepository,
        ),
    )
}

internal fun animeEntryInteractionPlugin(
    dependencies: AnimeEntryInteractionRuntimeDependencies,
): EntryInteractionPlugin {
    return EntryInteractionPlugin { registry ->
        val openProcessor = AnimeOpenProcessor()
        registry.registerOpenProcessor(openProcessor)
        registry.registerCapabilityProcessor(AnimeCapabilityProcessor())
        registry.registerChildListProcessor(
            AnimeChildListProcessor(
                playbackStateRepository = dependencies.playbackStateRepository,
            ),
        )
        registry.registerContinueProcessor(
            AnimeContinueProcessor(
                getEntryWithChapters = dependencies.getEntryWithChapters,
                playbackStateRepository = dependencies.playbackStateRepository,
                openProcessor = openProcessor,
            ),
        )
        registry.registerDownloadProcessor(
            AnimeDownloadProcessor(
                dependencies = dependencies,
            ),
        )
        registry.registerConsumptionProcessor(
            AnimeConsumptionProcessor(
                entryChapterRepository = dependencies.entryChapterRepository,
                playbackStateRepository = dependencies.playbackStateRepository,
            ),
        )
        registry.registerUpdateEligibilityProcessor(AnimeUpdateEligibilityProcessor())
        registry.registerPlaybackProcessor(
            AnimePlaybackProcessor(
                playbackStateRepository = dependencies.playbackStateRepository,
                playbackPreferencesRepository = dependencies.playbackPreferencesRepository,
            ),
        )
        registry.registerChildGroupFilterProcessor(AnimeChildGroupFilterProcessor())
        registry.registerLibraryFilterProcessor(AnimeLibraryFilterProcessor())
        registry.registerPreviewProcessor(
            AnimePreviewInteraction(
                entryInteractionPreferences = dependencies.entryInteractionPreferences,
                sourceManager = dependencies.sourceManager,
            ),
        )
        registry.registerImmersiveProcessor(
            AnimeImmersiveProcessor(
                playbackStateRepository = dependencies.playbackStateRepository,
                historyRepository = dependencies.historyRepository,
                resolveVideoStream = { Injekt.get() },
            ),
        )
    }
}

data class AnimeEntryInteractionDependencies(
    val entryChapterRepository: EntryChapterRepository,
    val getEntryWithChapters: GetEntryWithChapters,
    val playbackStateRepository: PlaybackStateRepository,
    val playbackPreferencesRepository: PlaybackPreferencesRepository,
    val downloadPreferences: DownloadPreferences,
    val downloadPreferencesRepository: DownloadPreferencesRepository,
    val sourceManager: SourceManager,
    val entryInteractionPreferences: EntryInteractionPreferences,
    val historyRepository: HistoryRepository? = null,
)

internal data class AnimeEntryInteractionRuntimeDependencies(
    val entryChapterRepository: EntryChapterRepository,
    val getEntryWithChapters: GetEntryWithChapters,
    val playbackStateRepository: PlaybackStateRepository,
    val playbackPreferencesRepository: PlaybackPreferencesRepository,
    val animeDownloadManager: AnimeDownloadManager,
    val animeDownloadCache: AnimeDownloadCache,
    val downloadPreferences: DownloadPreferences,
    val downloadPreferencesRepository: DownloadPreferencesRepository,
    val sourceManager: SourceManager,
    val entryInteractionPreferences: EntryInteractionPreferences,
    val historyRepository: HistoryRepository? = null,
)
