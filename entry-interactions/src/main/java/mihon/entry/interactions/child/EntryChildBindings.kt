package mihon.entry.interactions.child

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.child.backup.EntryChildGroupFilterBackupParticipant
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryChildGroupFilterDataSource
import mihon.entry.interactions.runtime.EntryChildGroupFilterProcessor
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryMissingChildGapProcessor
import mihon.feature.graph.FeatureGraphContributor
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntryChildBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun childListFeature(
        childListProcessors: Map<EntryType, EntryChildListProcessor>,
        childProgressProcessors: Map<EntryType, EntryChildProgressProcessor>,
        missingChildGapProcessors: Map<EntryType, EntryMissingChildGapProcessor>,
        interactions: EntryInteractions,
    ): EntryChildListFeature = DefaultEntryChildListFeature(
        types = EntryChildListTypes(
            childList = childListProcessors.keys,
            progress = childListProcessors.keys intersect childProgressProcessors.keys,
            missingGap = childListProcessors.keys intersect missingChildGapProcessors.keys,
        ),
        childList = interactions.childList,
        childProgress = interactions.childProgress,
        missingChildGap = interactions.missingChildGap,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun childGroupFilterFeature(
        childGroupFilterProcessors: Map<EntryType, EntryChildGroupFilterProcessor>,
        interactions: EntryInteractions,
        dataSource: EntryChildGroupFilterDataSource,
    ): EntryChildGroupFilterFeature = DefaultEntryChildGroupFilterFeature(
        applicableTypes = childGroupFilterProcessors.keys,
        interaction = interactions.childGroupFilter,
        dataSource = dataSource,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun relatedEntriesFeature(
        sourceManager: SourceManager,
        networkToLocalEntry: NetworkToLocalEntry,
        getEntry: GetEntry,
        sourceDescription: EntrySourceDescriptionResolutionPort,
    ): EntryRelatedEntriesFeature = DefaultEntryRelatedEntriesFeature(
        sourceManager = sourceManager,
        networkToLocalEntry = networkToLocalEntry,
        getEntry = getEntry,
        sourceDescription = sourceDescription,
    )

    @Provides
    @IntoSet
    fun childListGraphContributor(): FeatureGraphContributor = EntryChildListFeatureContributor

    @Provides
    @IntoSet
    fun childGroupFilterGraphContributor(): FeatureGraphContributor = EntryChildGroupFilterFeatureContributor

    @Provides
    @IntoSet
    fun relatedEntriesGraphContributor(): FeatureGraphContributor = EntryRelatedEntriesFeatureContributor

    @Provides
    @IntoSet
    internal fun childGroupFilterBackupState(
        feature: () -> EntryChildGroupFilterFeature,
    ): EntryBackupStateParticipant = EntryChildGroupFilterBackupParticipant(feature)
}
