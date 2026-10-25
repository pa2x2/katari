package mihon.entry.interactions.child

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.child.backup.EntryScanlatorFilterBackupParticipant
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryMissingChildGapProcessor
import mihon.entry.interactions.runtime.EntryScanlatorFilterDataSource
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
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
    fun scanlatorFilterFeature(
        scanlatorFilterProcessors: Map<EntryType, EntryScanlatorFilterProcessor>,
        interactions: EntryInteractions,
        dataSource: EntryScanlatorFilterDataSource,
    ): EntryScanlatorFilterFeature = DefaultEntryScanlatorFilterFeature(
        applicableTypes = scanlatorFilterProcessors.keys,
        interaction = interactions.scanlatorFilter,
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
    internal fun scanlatorFilterBackupState(
        feature: () -> EntryScanlatorFilterFeature,
    ): EntryBackupStateParticipant = EntryScanlatorFilterBackupParticipant(feature)
}
