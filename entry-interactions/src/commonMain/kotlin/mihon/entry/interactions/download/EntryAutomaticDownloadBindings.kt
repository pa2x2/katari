package mihon.entry.interactions.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryInteractions
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.repository.EntryChapterRepository

/**
 * Downloading what a refresh or a library update finds, kept apart from [EntryDownloadBindings] so that a platform
 * which doesn't host downloads still refreshes entries from their sources, its entry types answering as types
 * automatic downloads don't apply to.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryAutomaticDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticDownloadPolicy(
        entryChapterRepository: EntryChapterRepository,
        downloadPreferences: DownloadPreferences,
        getCategories: GetCategories,
    ): EntryAutomaticDownloadPolicy = EntryAutomaticDownloadPolicy(
        entryChapterRepository = entryChapterRepository,
        downloadPreferences = downloadPreferences,
        getCategories = getCategories,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticDownloadCoordinator(
        downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
        interactions: EntryInteractions,
        sharedPolicy: EntryAutomaticDownloadPolicy,
    ): EntryAutomaticDownloadCoordinator = DefaultEntryAutomaticDownloadFeature(
        applicableTypes = downloadProcessors.keys,
        interaction = interactions.download,
        sharedPolicy = sharedPolicy,
    )

    @Provides
    internal fun automaticDownloadFeature(
        coordinator: EntryAutomaticDownloadCoordinator,
    ): EntryAutomaticDownloadFeature = coordinator
}
