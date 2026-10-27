package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
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
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressProcessor
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.entry.interactions.translate.EntryTranslateProcessor

/**
 * The type modules (manga, anime, book) contribute to these registries on Android only, so on desktop each is declared
 * empty: the shared features run with no entry type and answer for every entry as for a type they don't apply to.
 * Android leaves them undeclared, so a type module that stops contributing fails its build there.
 */
@ContributesTo(AppScope::class)
@BindingContainer
interface DesktopEntryTypeBindings {
    @Multibinds(allowEmpty = true)
    fun openProcessors(): Map<EntryType, EntryOpenProcessor>

    @Multibinds(allowEmpty = true)
    fun continueProcessors(): Map<EntryType, EntryContinueProcessor>

    @Multibinds(allowEmpty = true)
    fun downloadProcessors(): Map<EntryType, EntryDownloadProcessor>

    @Multibinds(allowEmpty = true)
    fun downloadOptionsProcessors(): Map<EntryType, EntryDownloadOptionsProcessor>

    @Multibinds(allowEmpty = true)
    fun bulkDownloadCandidateProcessors(): Map<EntryType, EntryBulkDownloadCandidateProcessor>

    @Multibinds(allowEmpty = true)
    fun consumptionProcessors(): Map<EntryType, EntryConsumptionProcessor>

    @Multibinds(allowEmpty = true)
    fun bookmarkProcessors(): Map<EntryType, EntryBookmarkProcessor>

    @Multibinds(allowEmpty = true)
    fun progressProcessors(): Map<EntryType, EntryProgressProcessor>

    @Multibinds(allowEmpty = true)
    fun playbackPreferencesProcessors(): Map<EntryType, EntryPlaybackPreferencesProcessor>

    @Multibinds(allowEmpty = true)
    fun childListProcessors(): Map<EntryType, EntryChildListProcessor>

    @Multibinds(allowEmpty = true)
    fun childProgressProcessors(): Map<EntryType, EntryChildProgressProcessor>

    @Multibinds(allowEmpty = true)
    fun missingChildGapProcessors(): Map<EntryType, EntryMissingChildGapProcessor>

    @Multibinds(allowEmpty = true)
    fun scanlatorFilterProcessors(): Map<EntryType, EntryScanlatorFilterProcessor>

    @Multibinds(allowEmpty = true)
    fun previewProcessors(): Map<EntryType, EntryPreviewProcessor>

    @Multibinds(allowEmpty = true)
    fun previewConfigurationProviders(): Map<EntryType, EntryPreviewConfigurationProvider>

    @Multibinds(allowEmpty = true)
    fun immersiveProcessors(): Map<EntryType, EntryImmersiveProcessor>

    @Multibinds(allowEmpty = true)
    fun libraryProgressProviders(): Map<EntryType, EntryLibraryProgressProvider>

    @Multibinds(allowEmpty = true)
    fun typePresentationProviders(): Map<EntryType, EntryTypePresentationProvider>

    @Multibinds(allowEmpty = true)
    fun viewerSettingsProviders(): Map<EntryType, EntryViewerSettingsProvider>

    @Multibinds(allowEmpty = true)
    fun mediaCacheProviders(): Map<EntryType, EntryMediaCacheProvider>

    @Multibinds(allowEmpty = true)
    fun statisticsProviders(): Map<EntryType, EntryStatisticsProvider>

    @Multibinds(allowEmpty = true)
    fun translateProcessors(): Map<EntryType, EntryTranslateProcessor>

    @Multibinds(allowEmpty = true)
    fun translationLanguagesProviders(): Map<EntryType, EntryTranslationLanguagesProvider>

    @Multibinds(allowEmpty = true)
    fun migrationProviders(): Map<EntryType, EntryMigrationProvider>
}
