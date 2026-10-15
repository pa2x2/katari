package mihon.entry.interactions.state

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.state.backup.EntryProgressBackupParticipant
import mihon.entry.interactions.state.migration.EntryProgressMigrationParticipant
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences

@ContributesTo(AppScope::class)
@BindingContainer
object EntryStateBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun consumptionFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        downloadLifecycle: EntryDownloadLifecycleEventSink,
        history: EntryHistoryFeature,
    ): EntryConsumptionFeature = DefaultEntryConsumptionFeature(
        evaluation = composition.evaluation,
        interaction = interactions.consumption,
        downloadLifecycle = downloadLifecycle,
        history = history,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun bookmarkFeature(composition: FeatureRuntimeComposition, interactions: EntryInteractions): EntryBookmarkFeature =
        DefaultEntryBookmarkFeature(
            evaluation = composition.evaluation,
            interaction = interactions.bookmark,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun updateEligibilityFeature(composition: FeatureRuntimeComposition): EntryUpdateEligibilityFeature =
        DefaultEntryUpdateEligibilityFeature(evaluation = composition.evaluation)

    @Provides
    @SingleIn(AppScope::class)
    fun progressFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        repository: EntryProgressRepository,
        getEntryWithChapters: GetEntryWithChapters,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): EntryProgressFeature = DefaultEntryProgressFeature(
        evaluation = composition.evaluation,
        interaction = interactions.progress,
        repository = repository,
        getEntryWithChapters = getEntryWithChapters,
        globalLibraryPreferences = globalLibraryPreferences,
    )

    @Provides
    @IntoSet
    fun consumptionGraphContributor(): FeatureGraphContributor = EntryConsumptionFeatureContributor

    @Provides
    @IntoSet
    fun bookmarkGraphContributor(): FeatureGraphContributor = EntryBookmarkFeatureContributor

    @Provides
    @IntoSet
    fun updateEligibilityGraphContributor(): FeatureGraphContributor = EntryUpdateEligibilityFeatureContributor

    @Provides
    @IntoSet
    fun progressGraphContributor(): FeatureGraphContributor = EntryProgressFeatureContributor

    @Provides
    @IntoSet
    internal fun progressMigrationParticipant(
        feature: () -> EntryProgressFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryProgressMigrationParticipant(feature)

    @Provides
    @IntoSet
    internal fun progressBackupState(
        feature: () -> EntryProgressFeature,
    ): EntryBackupStateParticipant = EntryProgressBackupParticipant(feature)
}
