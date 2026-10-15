package mihon.entry.interactions.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntrySourceBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun settingsFeature(
        sourceManager: SourceManager,
    ): EntrySourceSettingsFeature = DefaultEntrySourceSettingsFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun homeFeature(
        sourceManager: SourceManager,
    ): EntrySourceHomeFeature = DefaultEntrySourceHomeFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun coverNetworkFeature(
        sourceManager: SourceManager,
    ): EntryCoverNetworkFeature = DefaultEntryCoverNetworkFeature(
        sourceManager = sourceManager,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun refreshFeature(
        sourceManager: SourceManager,
        syncEntryWithSource: SyncEntryWithSource,
        downloadRuntime: () -> EntryDownloadRuntimeFeature,
        titlePolicy: EntryLibraryTitleUpdatePolicy,
        automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
    ): EntrySourceRefreshFeature = DefaultEntrySourceRefreshFeature(
        automaticDownloads = automaticDownloads,
        sourceManager = sourceManager,
        syncEntryWithSource = syncEntryWithSource,
        downloadRuntime = downloadRuntime,
        updateLibraryTitles = titlePolicy::updatesTitles,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun webViewFeature(
        sourceManager: SourceManager,
    ): EntryWebViewFeature = DefaultEntryWebViewFeature(
        sourceManager = sourceManager,
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

    @Provides
    @SingleIn(AppScope::class)
    fun trackerSourceAdapterFeature(
        sourceManager: SourceManager,
        settings: EntrySourceSettingsFeature,
        home: EntrySourceHomeFeature,
    ): EntryTrackerSourceAdapterFeature = DefaultEntryTrackerSourceAdapterFeature(
        sourceManager = sourceManager,
        settings = settings,
        home = home,
    )
}
