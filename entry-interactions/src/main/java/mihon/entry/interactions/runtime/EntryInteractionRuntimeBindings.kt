package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.translate.EntryTranslateProcessor

@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionRuntimeBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun interactions(
        openProcessors: Map<EntryType, EntryOpenProcessor>,
        continueProcessors: Map<EntryType, EntryContinueProcessor>,
        downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
        downloadOptionsProcessors: Map<EntryType, EntryDownloadOptionsProcessor>,
        bulkDownloadCandidateProcessors: Map<EntryType, EntryBulkDownloadCandidateProcessor>,
        consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
        bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
        progressProcessors: Map<EntryType, EntryProgressProcessor>,
        playbackPreferencesProcessors: Map<EntryType, EntryPlaybackPreferencesProcessor>,
        childListProcessors: Map<EntryType, EntryChildListProcessor>,
        childProgressProcessors: Map<EntryType, EntryChildProgressProcessor>,
        missingChildGapProcessors: Map<EntryType, EntryMissingChildGapProcessor>,
        childGroupFilterProcessors: Map<EntryType, EntryChildGroupFilterProcessor>,
        previewProcessors: Map<EntryType, EntryPreviewProcessor>,
        previewConfigurationProviders: Map<EntryType, EntryPreviewConfigurationProvider>,
        immersiveProcessors: Map<EntryType, EntryImmersiveProcessor>,
        libraryProgressProviders: Map<EntryType, EntryLibraryProgressProvider>,
        typePresentationProviders: Map<EntryType, EntryTypePresentationProvider>,
        viewerSettingsProviders: Map<EntryType, EntryViewerSettingsProvider>,
        mediaCacheProviders: Map<EntryType, EntryMediaCacheProvider>,
        statisticsProviders: Map<EntryType, EntryStatisticsProvider>,
        translateProcessors: Map<EntryType, EntryTranslateProcessor>,
    ): EntryInteractions = DefaultEntryInteractions(
        openProcessors = openProcessors,
        continueProcessors = continueProcessors,
        downloadProcessors = downloadProcessors,
        downloadOptionsProcessors = downloadOptionsProcessors,
        bulkDownloadCandidateProcessors = bulkDownloadCandidateProcessors,
        consumptionProcessors = consumptionProcessors,
        bookmarkProcessors = bookmarkProcessors,
        progressProcessors = progressProcessors,
        playbackPreferencesProcessors = playbackPreferencesProcessors,
        childListProcessors = childListProcessors,
        childProgressProcessors = childProgressProcessors,
        missingChildGapProcessors = missingChildGapProcessors,
        childGroupFilterProcessors = childGroupFilterProcessors,
        previewProcessors = previewProcessors,
        previewConfigurationProviders = previewConfigurationProviders,
        immersiveProcessors = immersiveProcessors,
        libraryProgressProviders = libraryProgressProviders,
        typePresentationProviders = typePresentationProviders,
        viewerSettingsProviders = viewerSettingsProviders,
        mediaCacheProviders = mediaCacheProviders,
        statisticsProviders = statisticsProviders,
        translateProcessors = translateProcessors,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun imageComponentInstallers(installers: Set<EntryImageComponentInstaller>): EntryImageComponentInstallers =
        EntryImageComponentInstallers(installers.toList())
}
