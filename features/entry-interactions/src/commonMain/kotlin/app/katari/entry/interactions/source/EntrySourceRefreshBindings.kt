package app.katari.entry.interactions.source

import app.katari.entry.interactions.download.EntryAutomaticDownloadCoordinator
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.merge.EntryMergeSourceGroupFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.domain.entry.interactor.FinishChapterListWalk
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Refreshing entries from their sources and opening deep links, which refresh what they open. Kept apart from
 * [EntrySourceBindings] because refreshing reaches the download and merge features, so a graph without those can
 * leave it out.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntrySourceRefreshBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun refreshFeature(
        sourceManager: SourceManager,
        syncEntryWithSource: SyncEntryWithSource,
        downloadRuntime: () -> EntryDownloadRuntimeFeature,
        titlePolicy: EntryLibraryTitleUpdatePolicy,
        automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
        networkToLocalEntry: NetworkToLocalEntry,
        mergeSourceGroups: () -> EntryMergeSourceGroupFeature,
        finishChapterListWalk: FinishChapterListWalk,
        updateReport: LibraryUpdateReportRepository,
    ): EntrySourceRefreshFeature = DefaultEntrySourceRefreshFeature(
        automaticDownloads = automaticDownloads,
        sourceManager = sourceManager,
        syncEntryWithSource = syncEntryWithSource,
        downloadRuntime = downloadRuntime,
        updateLibraryTitles = titlePolicy::updatesTitles,
        networkToLocalEntry = networkToLocalEntry,
        mergeSourceGroups = mergeSourceGroups,
        finishChapterListWalk = finishChapterListWalk,
        updateReport = updateReport,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun deepLinkFeature(
        sourceManager: SourceManager,
        networkToLocalEntry: NetworkToLocalEntry,
        entryChapterRepository: EntryChapterRepository,
        sourceRefresh: EntrySourceRefreshFeature,
    ): EntryDeepLinkFeature = DefaultEntryDeepLinkFeature(
        sourceManager = sourceManager,
        networkToLocalEntry = networkToLocalEntry,
        entryChapterRepository = entryChapterRepository,
        sourceRefresh = sourceRefresh,
    )
}
