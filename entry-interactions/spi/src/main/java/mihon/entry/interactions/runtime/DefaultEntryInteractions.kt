package mihon.entry.interactions.runtime

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.child.EntryChildListInteraction
import mihon.entry.interactions.child.EntryChildProgressInteraction
import mihon.entry.interactions.child.EntryMissingChildGapInteraction
import mihon.entry.interactions.child.EntryScanlatorFilterInteraction
import mihon.entry.interactions.child.ProviderBackedEntryChildListInteraction
import mihon.entry.interactions.child.ProviderBackedEntryChildProgressInteraction
import mihon.entry.interactions.child.ProviderBackedEntryMissingChildGapInteraction
import mihon.entry.interactions.child.ProviderBackedEntryScanlatorFilterInteraction
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.download.EntryDownloadInteractionDispatch
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.library.EntryLibraryProgressInteraction
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.library.ProviderBackedEntryLibraryProgressInteraction
import mihon.entry.interactions.media.EntryImmersiveInteraction
import mihon.entry.interactions.media.EntryMediaCacheInteraction
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryPreviewInteraction
import mihon.entry.interactions.media.EntryViewerSettingsInteraction
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.media.ProviderBackedEntryImmersiveInteraction
import mihon.entry.interactions.media.ProviderBackedEntryMediaCacheInteraction
import mihon.entry.interactions.media.ProviderBackedEntryPreviewInteraction
import mihon.entry.interactions.media.ProviderBackedEntryViewerSettingsInteraction
import mihon.entry.interactions.navigation.EntryContinueInteraction
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenInteraction
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.navigation.ProviderBackedEntryContinueInteraction
import mihon.entry.interactions.navigation.ProviderBackedEntryOpenInteraction
import mihon.entry.interactions.presentation.EntryTypePresentationInteraction
import mihon.entry.interactions.presentation.ProviderBackedEntryTypePresentationInteraction
import mihon.entry.interactions.state.EntryBookmarkInteraction
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionInteraction
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressInteraction
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.ProviderBackedEntryBookmarkInteraction
import mihon.entry.interactions.state.ProviderBackedEntryConsumptionInteraction
import mihon.entry.interactions.state.ProviderBackedEntryPlaybackPreferencesInteraction
import mihon.entry.interactions.state.ProviderBackedEntryProgressInteraction
import mihon.entry.interactions.statistics.EntryStatisticsInteraction
import mihon.entry.interactions.statistics.ProviderBackedEntryStatisticsInteraction
import mihon.entry.interactions.translate.EntryTranslateInteraction
import mihon.entry.interactions.translate.EntryTranslateInteractionDispatch
import mihon.entry.interactions.translate.EntryTranslateProcessor

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
