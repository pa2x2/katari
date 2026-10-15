package mihon.entry.interactions.source

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.stateRequest
import mihon.entry.interactions.source.lifecycle.ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.source.lifecycle.EntryCoverHashProfileMoveContributor
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
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
    @IntoSet
    fun coverHashProfileMoveGraphContributor(): FeatureGraphContributor = EntryCoverHashProfileMoveContributor

    @Provides
    @IntoSet
    fun coverHashProfileMoveParticipant(
        profileMoveCoverHashStateHost: EntryProfileMoveCoverHashStateHost,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            profileMoveCoverHashStateHost.move(event.plan.stateRequest())
        },
    )

    @Provides
    @SingleIn(AppScope::class)
    fun refreshFeature(
        composition: FeatureRuntimeComposition,
        sourceManager: SourceManager,
        syncEntryWithSource: SyncEntryWithSource,
        downloadRuntime: () -> EntryDownloadRuntimeFeature,
        titlePolicy: EntryLibraryTitleUpdatePolicy,
    ): EntrySourceRefreshFeature = DefaultEntrySourceRefreshFeature(
        evaluation = composition.evaluation,
        executions = composition.executions,
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

    @Provides
    @IntoSet
    fun exportSettingsFeature(feature: () -> EntrySourceSettingsFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportHomeFeature(feature: () -> EntrySourceHomeFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportCoverNetworkFeature(feature: () -> EntryCoverNetworkFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportRefreshFeature(feature: () -> EntrySourceRefreshFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportWebViewFeature(feature: () -> EntryWebViewFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportDeepLinkFeature(feature: () -> EntryDeepLinkFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportTrackerSourceAdapterFeature(feature: () -> EntryTrackerSourceAdapterFeature): InjektExport<*> =
        injektExport(feature)
}
