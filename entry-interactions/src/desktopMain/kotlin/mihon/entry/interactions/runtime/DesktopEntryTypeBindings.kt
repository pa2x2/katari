package mihon.entry.interactions.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadSetting
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.translate.EntryTranslateProcessor

/**
 * The registries that only the entry types' viewers and downloads fill, which no type contributes on desktop yet, so
 * each is declared empty here and the shared features answer for every entry as for a type they don't apply to. The
 * rest, such as reading state, the chapter list, library progress, labels and statistics, the type modules contribute
 * on both platforms, so those stay undeclared and a type that stops contributing them fails to build on both.
 *
 * - Opening, continuing, previews, the immersive view and the page cache they fill need the readers and players, which
 *   desktop doesn't have yet.
 * - The download registries stay empty (D071): the manga, anime and book download providers, caches and jobs are built
 *   on UniFile, Android's document-file access, and move onto the app-wide storage interface only in the storage
 *   review, with the readers. Until then the shared download features report every type as inapplicable, so desktop
 *   offers no download actions, no "Downloaded only" and no download queue, and translating downloaded chapters, which
 *   needs a download, applies to no type either.
 * - Playback preferences only anime contributes, and anime, like book, compiles for Android only so far. Its
 *   declaration goes when anime does compile for desktop.
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
    fun downloadSettings(): Set<EntryDownloadSetting>

    @Multibinds(allowEmpty = true)
    fun bulkDownloadCandidateProcessors(): Map<EntryType, EntryBulkDownloadCandidateProcessor>

    @Multibinds(allowEmpty = true)
    fun playbackPreferencesProcessors(): Map<EntryType, EntryPlaybackPreferencesProcessor>

    @Multibinds(allowEmpty = true)
    fun previewProcessors(): Map<EntryType, EntryPreviewProcessor>

    @Multibinds(allowEmpty = true)
    fun previewConfigurationProviders(): Map<EntryType, EntryPreviewConfigurationProvider>

    @Multibinds(allowEmpty = true)
    fun immersiveProcessors(): Map<EntryType, EntryImmersiveProcessor>

    @Multibinds(allowEmpty = true)
    fun mediaCacheProviders(): Map<EntryType, EntryMediaCacheProvider>

    @Multibinds(allowEmpty = true)
    fun translateProcessors(): Map<EntryType, EntryTranslateProcessor>
}
