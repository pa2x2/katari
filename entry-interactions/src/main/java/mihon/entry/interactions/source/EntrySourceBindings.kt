package mihon.entry.interactions.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
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
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
    ): EntrySourceSettingsFeature = DefaultEntrySourceSettingsFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
    )

    @Provides
    @IntoSet
    fun settingsGraphContributor(): FeatureGraphContributor = EntrySourceSettingsFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun homeFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
    ): EntrySourceHomeFeature = DefaultEntrySourceHomeFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
    )

    @Provides
    @IntoSet
    fun homeGraphContributor(): FeatureGraphContributor = EntrySourceHomeFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun coverNetworkFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
    ): EntryCoverNetworkFeature = DefaultEntryCoverNetworkFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
    )

    @Provides
    @IntoSet
    fun coverNetworkGraphContributor(): FeatureGraphContributor = EntryCoverNetworkFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    internal fun refreshFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
        syncEntryWithSource: SyncEntryWithSource,
        downloadRuntime: () -> EntryDownloadRuntimeFeature,
        titlePolicy: EntryLibraryTitleUpdatePolicy,
        automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
    ): EntrySourceRefreshFeature = DefaultEntrySourceRefreshFeature(
        evaluation = composition.evaluation,
        automaticDownloads = automaticDownloads,
        sourceManager = sourceManager,
        syncEntryWithSource = syncEntryWithSource,
        downloadRuntime = downloadRuntime,
        updateLibraryTitles = titlePolicy::updatesTitles,
    )

    @Provides
    @IntoSet
    fun refreshGraphContributor(): FeatureGraphContributor = EntrySourceRefreshFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun webViewFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
    ): EntryWebViewFeature = DefaultEntryWebViewFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
    )

    @Provides
    @IntoSet
    fun webViewGraphContributor(): FeatureGraphContributor = EntryWebViewFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun deepLinkFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
        networkToLocalEntry: NetworkToLocalEntry,
        entryChapterRepository: EntryChapterRepository,
        sourceRefresh: EntrySourceRefreshFeature,
    ): EntryDeepLinkFeature = DefaultEntryDeepLinkFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
        networkToLocalEntry = networkToLocalEntry,
        entryChapterRepository = entryChapterRepository,
        sourceRefresh = sourceRefresh,
    )

    @Provides
    @IntoSet
    fun deepLinkGraphContributor(): FeatureGraphContributor = EntryDeepLinkFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun trackerSourceAdapterFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
        settings: EntrySourceSettingsFeature,
        home: EntrySourceHomeFeature,
    ): EntryTrackerSourceAdapterFeature = DefaultEntryTrackerSourceAdapterFeature(
        evaluation = composition.evaluation,
        sourceManager = sourceManager,
        settings = settings,
        home = home,
    )

    @Provides
    @IntoSet
    fun trackerSourceAdapterGraphContributor(): FeatureGraphContributor = EntryTrackerSourceAdapterFeatureContributor
}
