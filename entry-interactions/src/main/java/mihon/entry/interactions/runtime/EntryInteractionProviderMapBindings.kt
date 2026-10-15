package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryBulkDownloadCandidateCapability
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadCapability
import mihon.entry.interactions.download.EntryDownloadOptionsCapability
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.library.EntryLibraryProgressCapability
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.EntryMediaCacheCapability
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsCapability
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueCapability
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenCapability
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.state.EntryBookmarkCapability
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionCapability
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesCapability
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressCapability
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.entry.interactions.translate.EntryTranslateCapability
import mihon.entry.interactions.translate.EntryTranslateProcessor

/** Each capability's providers keyed by the type that contributes them, read from the type modules' plugins. */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionProviderMapBindings {
    @Provides
    fun mediaSessionProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryMediaSessionProcessor> =
        plugins.providersOf(EntryMediaSessionCapability)

    @Provides
    fun progressProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryProgressProcessor> =
        plugins.providersOf(EntryProgressCapability)

    @Provides
    fun downloadProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryDownloadProcessor> =
        plugins.providersOf(EntryDownloadCapability)

    @Provides
    fun childGroupFilterProcessors(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryChildGroupFilterProcessor> = plugins.providersOf(EntryChildGroupFilterCapability)

    @Provides
    fun translateProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryTranslateProcessor> =
        plugins.providersOf(EntryTranslateCapability)

    @Provides
    fun bookmarkProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryBookmarkProcessor> =
        plugins.providersOf(EntryBookmarkCapability)

    @Provides
    fun bulkDownloadCandidateProcessors(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryBulkDownloadCandidateProcessor> = plugins.providersOf(EntryBulkDownloadCandidateCapability)

    @Provides
    fun downloadOptionsProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryDownloadOptionsProcessor> =
        plugins.providersOf(EntryDownloadOptionsCapability)

    @Provides
    fun openProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryOpenProcessor> =
        plugins.providersOf(EntryOpenCapability)

    @Provides
    fun continueProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryContinueProcessor> =
        plugins.providersOf(EntryContinueCapability)

    @Provides
    fun libraryProgressProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryLibraryProgressProvider> =
        plugins.providersOf(EntryLibraryProgressCapability)

    @Provides
    fun migrationProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryMigrationProvider> =
        plugins.providersOf(EntryMigrationCapability)

    @Provides
    fun consumptionProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryConsumptionProcessor> =
        plugins.providersOf(EntryConsumptionCapability)

    @Provides
    fun playbackPreferencesProcessors(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryPlaybackPreferencesProcessor> = plugins.providersOf(EntryPlaybackPreferencesCapability)

    @Provides
    fun translationLanguagesProviders(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryTranslationLanguagesProvider> = plugins.providersOf(EntryTranslationLanguagesCapability)

    @Provides
    fun mediaCacheProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryMediaCacheProvider> =
        plugins.providersOf(EntryMediaCacheCapability)

    @Provides
    fun viewerSettingsProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryViewerSettingsProvider> =
        plugins.providersOf(EntryViewerSettingsCapability)

    @Provides
    fun immersiveProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryImmersiveProcessor> =
        plugins.providersOf(EntryImmersiveCapability)

    @Provides
    fun childListProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryChildListProcessor> =
        plugins.providersOf(EntryChildListCapability)

    @Provides
    fun childProgressProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryChildProgressProcessor> =
        plugins.providersOf(EntryChildProgressCapability)

    @Provides
    fun missingChildGapProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryMissingChildGapProcessor> =
        plugins.providersOf(EntryMissingChildGapCapability)

    @Provides
    fun outsideReleasePeriodFilterProviders(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryOutsideReleasePeriodFilterProvider> = plugins.providersOf(
        EntryOutsideReleasePeriodFilterCapability,
    )

    @Provides
    fun previewProcessors(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryPreviewProcessor> =
        plugins.providersOf(EntryPreviewCapability)

    @Provides
    fun previewConfigurationProviders(
        plugins: Set<EntryInteractionPlugin>,
    ): Map<EntryType, EntryPreviewConfigurationProvider> = plugins.providersOf(EntryPreviewConfigurationCapability)

    @Provides
    fun typePresentationProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryTypePresentationProvider> =
        plugins.providersOf(EntryTypePresentationCapability)

    @Provides
    fun statisticsProviders(plugins: Set<EntryInteractionPlugin>): Map<EntryType, EntryStatisticsProvider> =
        plugins.providersOf(EntryStatisticsCapability)
}
