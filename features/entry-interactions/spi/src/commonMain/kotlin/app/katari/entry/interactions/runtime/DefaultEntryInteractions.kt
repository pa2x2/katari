package app.katari.entry.interactions.runtime

import app.katari.entry.interactions.child.EntryChildListInteraction
import app.katari.entry.interactions.child.EntryChildProgressInteraction
import app.katari.entry.interactions.child.EntryMissingChildGapInteraction
import app.katari.entry.interactions.child.EntryScanlatorFilterInteraction
import app.katari.entry.interactions.child.ProviderBackedEntryChildListInteraction
import app.katari.entry.interactions.child.ProviderBackedEntryChildProgressInteraction
import app.katari.entry.interactions.child.ProviderBackedEntryMissingChildGapInteraction
import app.katari.entry.interactions.child.ProviderBackedEntryScanlatorFilterInteraction
import app.katari.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import app.katari.entry.interactions.download.EntryDownloadInteraction
import app.katari.entry.interactions.download.EntryDownloadInteractionDispatch
import app.katari.entry.interactions.download.EntryDownloadOptionsProcessor
import app.katari.entry.interactions.download.EntryDownloadProcessor
import app.katari.entry.interactions.library.EntryLibraryProgressInteraction
import app.katari.entry.interactions.library.EntryLibraryProgressProvider
import app.katari.entry.interactions.library.ProviderBackedEntryLibraryProgressInteraction
import app.katari.entry.interactions.media.EntryImmersiveInteraction
import app.katari.entry.interactions.media.EntryMediaCacheInteraction
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.media.EntryPreviewInteraction
import app.katari.entry.interactions.media.EntryViewerSettingsInteraction
import app.katari.entry.interactions.media.EntryViewerSettingsProvider
import app.katari.entry.interactions.media.ProviderBackedEntryImmersiveInteraction
import app.katari.entry.interactions.media.ProviderBackedEntryMediaCacheInteraction
import app.katari.entry.interactions.media.ProviderBackedEntryPreviewInteraction
import app.katari.entry.interactions.media.ProviderBackedEntryViewerSettingsInteraction
import app.katari.entry.interactions.navigation.EntryContinueInteraction
import app.katari.entry.interactions.navigation.EntryContinueProcessor
import app.katari.entry.interactions.navigation.EntryOpenInteraction
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.navigation.ProviderBackedEntryContinueInteraction
import app.katari.entry.interactions.navigation.ProviderBackedEntryOpenInteraction
import app.katari.entry.interactions.presentation.EntryTypePresentationInteraction
import app.katari.entry.interactions.presentation.ProviderBackedEntryTypePresentationInteraction
import app.katari.entry.interactions.state.EntryBookmarkInteraction
import app.katari.entry.interactions.state.EntryBookmarkProcessor
import app.katari.entry.interactions.state.EntryConsumptionInteraction
import app.katari.entry.interactions.state.EntryConsumptionProcessor
import app.katari.entry.interactions.state.EntryPlaybackPreferencesInteraction
import app.katari.entry.interactions.state.EntryPlaybackPreferencesProcessor
import app.katari.entry.interactions.state.EntryProgressInteraction
import app.katari.entry.interactions.state.EntryProgressProcessor
import app.katari.entry.interactions.state.ProviderBackedEntryBookmarkInteraction
import app.katari.entry.interactions.state.ProviderBackedEntryConsumptionInteraction
import app.katari.entry.interactions.state.ProviderBackedEntryPlaybackPreferencesInteraction
import app.katari.entry.interactions.state.ProviderBackedEntryProgressInteraction
import app.katari.entry.interactions.statistics.EntryStatisticsInteraction
import app.katari.entry.interactions.statistics.ProviderBackedEntryStatisticsInteraction
import app.katari.entry.interactions.translate.EntryTranslateInteraction
import app.katari.entry.interactions.translate.EntryTranslateInteractionDispatch
import app.katari.entry.interactions.translate.EntryTranslateProcessor
import eu.kanade.tachiyomi.source.entry.EntryType

/** Dispatches each Entry interaction to the provider that the entry's type contributes. */
class DefaultEntryInteractions(
    openProcessors: Map<EntryType, EntryOpenProcessor> = emptyMap(),
    continueProcessors: Map<EntryType, EntryContinueProcessor> = emptyMap(),
    downloadProcessors: Map<EntryType, EntryDownloadProcessor> = emptyMap(),
    downloadOptionsProcessors: Map<EntryType, EntryDownloadOptionsProcessor> = emptyMap(),
    bulkDownloadCandidateProcessors: Map<EntryType, EntryBulkDownloadCandidateProcessor> = emptyMap(),
    consumptionProcessors: Map<EntryType, EntryConsumptionProcessor> = emptyMap(),
    bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor> = emptyMap(),
    progressProcessors: Map<EntryType, EntryProgressProcessor> = emptyMap(),
    playbackPreferencesProcessors: Map<EntryType, EntryPlaybackPreferencesProcessor> = emptyMap(),
    childListProcessors: Map<EntryType, EntryChildListProcessor> = emptyMap(),
    childProgressProcessors: Map<EntryType, EntryChildProgressProcessor> = emptyMap(),
    missingChildGapProcessors: Map<EntryType, EntryMissingChildGapProcessor> = emptyMap(),
    scanlatorFilterProcessors: Map<EntryType, EntryScanlatorFilterProcessor> = emptyMap(),
    previewProcessors: Map<EntryType, EntryPreviewProcessor> = emptyMap(),
    previewConfigurationProviders: Map<EntryType, EntryPreviewConfigurationProvider> = emptyMap(),
    immersiveProcessors: Map<EntryType, EntryImmersiveProcessor> = emptyMap(),
    libraryProgressProviders: Map<EntryType, EntryLibraryProgressProvider> = emptyMap(),
    typePresentationProviders: Map<EntryType, EntryTypePresentationProvider> = emptyMap(),
    viewerSettingsProviders: Map<EntryType, EntryViewerSettingsProvider> = emptyMap(),
    mediaCacheProviders: Map<EntryType, EntryMediaCacheProvider> = emptyMap(),
    statisticsProviders: Map<EntryType, EntryStatisticsProvider> = emptyMap(),
    translateProcessors: Map<EntryType, EntryTranslateProcessor> = emptyMap(),
) : EntryInteractions {
    override val open: EntryOpenInteraction = ProviderBackedEntryOpenInteraction(openProcessors)
    override val continueEntry: EntryContinueInteraction =
        ProviderBackedEntryContinueInteraction(continueProcessors)
    override val download: EntryDownloadInteraction =
        EntryDownloadInteractionDispatch(
            processors = downloadProcessors,
            optionsProcessors = downloadOptionsProcessors,
            bulkCandidateProcessors = bulkDownloadCandidateProcessors,
        )
    override val consumption: EntryConsumptionInteraction =
        ProviderBackedEntryConsumptionInteraction(consumptionProcessors)
    override val bookmark: EntryBookmarkInteraction = ProviderBackedEntryBookmarkInteraction(bookmarkProcessors)
    override val progress: EntryProgressInteraction = ProviderBackedEntryProgressInteraction(progressProcessors)
    override val playbackPreferences: EntryPlaybackPreferencesInteraction =
        ProviderBackedEntryPlaybackPreferencesInteraction(playbackPreferencesProcessors)
    override val childList: EntryChildListInteraction =
        ProviderBackedEntryChildListInteraction(childListProcessors)
    override val childProgress: EntryChildProgressInteraction =
        ProviderBackedEntryChildProgressInteraction(childProgressProcessors)
    override val missingChildGap: EntryMissingChildGapInteraction =
        ProviderBackedEntryMissingChildGapInteraction(missingChildGapProcessors)
    override val scanlatorFilter: EntryScanlatorFilterInteraction =
        ProviderBackedEntryScanlatorFilterInteraction(scanlatorFilterProcessors)
    override val preview: EntryPreviewInteraction =
        ProviderBackedEntryPreviewInteraction(previewProcessors, previewConfigurationProviders)
    override val immersive: EntryImmersiveInteraction =
        ProviderBackedEntryImmersiveInteraction(immersiveProcessors)
    override val libraryProgress: EntryLibraryProgressInteraction =
        ProviderBackedEntryLibraryProgressInteraction(libraryProgressProviders)
    override val typePresentation: EntryTypePresentationInteraction =
        ProviderBackedEntryTypePresentationInteraction(typePresentationProviders)
    override val viewerSettings: EntryViewerSettingsInteraction =
        ProviderBackedEntryViewerSettingsInteraction(viewerSettingsProviders)
    override val mediaCache: EntryMediaCacheInteraction =
        ProviderBackedEntryMediaCacheInteraction(mediaCacheProviders)
    override val statistics: EntryStatisticsInteraction =
        ProviderBackedEntryStatisticsInteraction(statisticsProviders)
    override val translate: EntryTranslateInteraction = EntryTranslateInteractionDispatch(translateProcessors)
}
