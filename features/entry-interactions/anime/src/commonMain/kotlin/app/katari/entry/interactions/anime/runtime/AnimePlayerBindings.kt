package app.katari.entry.interactions.anime.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.entry.interactions.anime.download.AnimeDownloadProvider
import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisodeReader
import app.katari.entry.interactions.anime.introtimes.aniskip.AniSkipLookup
import app.katari.entry.interactions.anime.introtimes.theintrodb.TheIntroDbLookup
import app.katari.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import app.katari.entry.interactions.anime.stream.PreferredHeight
import app.katari.entry.interactions.anime.stream.StreamTypeProbe
import app.katari.entry.interactions.child.EntryChildListFeature
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.media.EntryMediaSessionProcessor
import app.katari.entry.interactions.media.handoff.EntryProgressOffers
import app.katari.entry.interactions.media.session.EntryMediaSessionEventSink
import app.katari.entry.interactions.runtime.EntryTypeKey
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import app.katari.entry.interactions.source.EntryWebViewFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.ResolveVideoStream
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.ChapterGroupExpansionRepository
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedMemberExpansionRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceInfoNames
import tachiyomi.domain.source.service.SourceManager

/**
 * The video player's stream resolution, view model and media session events, alike on every platform; each platform
 * binds its engine. The view model is internal to this module, which a contributed class can't be, so its factory joins
 * the app's view model factories through this container.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimePlayerBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun resolveVideoStream(
        videoRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
        sourceManager: SourceManager,
        animeDownloadProvider: AnimeDownloadProvider,
        streamTypeProbe: StreamTypeProbe,
        networkConnectivity: NetworkConnectivity,
        animePlayerPreferences: AnimePlayerPreferences,
    ): ResolveVideoStream = ResolveVideoStream(
        videoRepository = videoRepository,
        entryChapterRepository = entryChapterRepository,
        playbackPreferencesRepository = playbackPreferencesRepository,
        videoSourceManager = sourceManager,
        animeDownloadProvider = animeDownloadProvider,
        streamTypeProbe = streamTypeProbe,
        isOnline = { networkConnectivity.current().hasNetwork },
        preferredHeight = { PreferredHeight.ofPlayerSetting(animePlayerPreferences.preferredQualityHeight.get()) },
    )

    @Provides
    internal fun videoStreamResolver(resolveVideoStream: ResolveVideoStream): VideoStreamResolver = resolveVideoStream

    @Provides
    @SingleIn(AppScope::class)
    internal fun basePreferences(preferenceStore: PreferenceStore): AnimePlayerBasePreferences =
        AnimePlayerBasePreferences(preferenceStore)

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): AnimeMediaSessionProcessor =
        AnimeMediaSessionProcessor(sink)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun mediaSessionProcessor(processor: AnimeMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(VideoPlayerViewModel::class)
    internal fun videoPlayerViewModelFactory(
        resolveVideoStream: VideoStreamResolver,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
        entryChapterRepository: EntryChapterRepository,
        getEntryWithChapters: GetEntryWithChapters,
        entryRepository: EntryRepository,
        sourceNames: SourceInfoNames,
        mergedMemberExpansionRepository: MergedMemberExpansionRepository,
        entryChapterGroupRepository: EntryChapterGroupRepository,
        chapterGroupExpansionRepository: ChapterGroupExpansionRepository,
        childListFeature: EntryChildListFeature,
        entryProgressRepository: EntryProgressRepository,
        mediaSession: AnimeMediaSessionProcessor,
        webViewFeature: EntryWebViewFeature,
        sourceSettingsFeature: EntrySourceSettingsFeature,
        downloadRuntime: EntryDownloadRuntimeFeature,
        libraryPreferences: LibraryPreferences,
        introTimesEpisodes: IntroTimesEpisodeReader,
        aniSkipLookup: AniSkipLookup,
        theIntroDbLookup: TheIntroDbLookup,
        progressOffers: EntryProgressOffers,
        @AppCoroutineScope appScope: CoroutineScope,
    ): ViewModelAssistedFactory = object : ViewModelAssistedFactory {
        override fun create(extras: CreationExtras): ViewModel = VideoPlayerViewModel(
            savedState = extras.createSavedStateHandle(),
            resolveVideoStream = resolveVideoStream,
            playbackPreferencesRepository = playbackPreferencesRepository,
            entryChapterRepository = entryChapterRepository,
            getEntryWithChapters = getEntryWithChapters,
            entryRepository = entryRepository,
            sourceNames = sourceNames,
            mergedMemberExpansionRepository = mergedMemberExpansionRepository,
            entryChapterGroupRepository = entryChapterGroupRepository,
            chapterGroupExpansionRepository = chapterGroupExpansionRepository,
            childListFeature = childListFeature,
            entryProgressRepository = entryProgressRepository,
            mediaSession = mediaSession,
            webViewFeature = webViewFeature,
            sourceSettingsFeature = sourceSettingsFeature,
            downloadRuntime = downloadRuntime,
            libraryPreferences = libraryPreferences,
            introTimesEpisodes = introTimesEpisodes,
            aniSkipLookup = aniSkipLookup,
            theIntroDbLookup = theIntroDbLookup,
            progressOffers = progressOffers,
            persistenceScope = appScope,
        )
    }
}
