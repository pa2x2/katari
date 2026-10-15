package mihon.entry.interactions.state

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.download.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.media.session.EntryMediaSessionConsequence
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.state.backup.ENTRY_PROGRESS_BACKUP_STATE_ID
import mihon.entry.interactions.state.backup.EntryProgressBackupContributor
import mihon.entry.interactions.state.migration.EntryProgressMigrationContributor
import mihon.entry.interactions.state.migration.entryProgressMigrationBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionContextResolver
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
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
    fun progressBackupGraphContributor(): FeatureGraphContributor = EntryProgressBackupContributor

    @Provides
    @IntoSet
    fun progressMigrationGraphContributor(): FeatureGraphContributor = EntryProgressMigrationContributor

    @Provides
    @IntoSet
    fun progressMediaSessionGraphContributor(): FeatureGraphContributor = EntryProgressMediaSessionContributor

    @Provides
    @IntoSet
    fun progressMigrationParticipant(
        feature: () -> EntryProgressFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryProgressMigrationBinding(feature)

    @Provides
    @IntoSet
    fun progressMediaSessionParticipant(feature: () -> EntryProgressFeature): FeatureExecutionParticipantBinding<*> =
        entryProgressMediaSessionBinding(feature)

    @Provides
    @IntoSet
    fun progressBackupSnapshotParticipant(
        feature: () -> EntryProgressFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PROGRESS_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            if (!event.selection.includeContentState) return@FeatureExecutionHandler
            val result = feature().snapshot(event.entry)
            if (result is EntryProgressSnapshotResult.Available && result.snapshot.states.isNotEmpty()) {
                event.contributions.add(
                    entryBackupStateEnvelope(
                        ENTRY_PROGRESS_BACKUP_STATE_ID,
                        ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                        EntryProgressSnapshot.serializer(),
                        result.snapshot,
                    ),
                )
            }
        },
    )

    @Provides
    @IntoSet
    fun progressBackupRestoreParticipant(
        feature: () -> EntryProgressFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_PROGRESS_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_PROGRESS_BACKUP_STATE_ID,
                ENTRY_PROGRESS_BACKUP_SCHEMA_VERSION,
                EntryProgressSnapshot.serializer(),
            ) ?: return@FeatureExecutionHandler
            feature().restore(event.entry, state)
        },
    )

    @Provides
    @IntoSet
    fun exportConsumptionFeature(feature: () -> EntryConsumptionFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportBookmarkFeature(feature: () -> EntryBookmarkFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportUpdateEligibilityFeature(feature: () -> EntryUpdateEligibilityFeature): InjektExport<*> =
        injektExport(feature)

    @Provides
    @IntoSet
    fun exportProgressFeature(feature: () -> EntryProgressFeature): InjektExport<*> = injektExport(feature)
}

internal fun entryProgressMediaSessionBinding(
    feature: () -> EntryProgressFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT,
    contextResolver = FeatureExecutionContextResolver { execution ->
        listOf(
            contextEvidence(
                ENTRY_MEDIA_SESSION_PROGRESS_ALLOWED,
                execution.permits(EntryMediaSessionConsequence.RECORD_PROGRESS),
            ),
        )
    },
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event as? EntryMediaSessionEvent.Progressed
            ?: return@FeatureExecutionHandler
        execution.progressResult = feature().recordMediaProgress(event)
    },
)
