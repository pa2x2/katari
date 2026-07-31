package mihon.entry.interactions.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.mockk
import mihon.entry.interactions.download.maintenance.migration.ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT
import mihon.entry.interactions.download.maintenance.migration.EntryDownloadMigrationContributor
import mihon.entry.interactions.download.maintenance.migration.entryDownloadMigrationBinding
import mihon.entry.interactions.merge.EntryMergeMigrationReplacementResult
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableConsequences
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverContributor
import mihon.entry.interactions.migration.consequence.cover.entryMigrationCustomCoverBinding
import mihon.entry.interactions.migration.options.EntryMigrationOptionDiscovery
import mihon.entry.interactions.migration.preparation.EntryMigrationTransitionPreparation
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.state.EntryBookmarkCapability
import mihon.entry.interactions.state.EntryConsumptionCapability
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.tracking.EntryTrackingMigrationPreparationResult
import mihon.entry.interactions.tracking.migration.EntryTrackingMigrationContributor
import mihon.entry.interactions.tracking.migration.entryTrackingMigrationBinding
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.graph.validation.FeatureContractExecutionInput

internal fun entryMigrationContractFeature(
    input: FeatureContractExecutionInput,
    type: EntryType,
    host: RecordingEntryMigrationHost,
): EntryMigrationFeature {
    val bindings = buildList {
        add(EntryMigrationCapability.bind(input.provider(EntryMigrationCapability.definition)))
        input.providerOrNull(EntryConsumptionCapability.definition)?.let { add(EntryConsumptionCapability.bind(it)) }
        input.providerOrNull(EntryBookmarkCapability.definition)?.let { add(EntryBookmarkCapability.bind(it)) }
    }
    val composition = createEntryInteractionComposition(
        plugins = listOf(
            object : EntryInteractionPlugin {
                override val type = type
                override val owner = ContributionOwner("migration-contract-type")
                override val providerBindings = bindings
            },
        ),
        featureContributors = listOf(
            EntryMigrationFeatureContributor,
            EntryDownloadMigrationContributor,
            EntryMigrationCustomCoverContributor,
            EntryTrackingMigrationContributor,
        ),
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT,
                handler = FeatureExecutionHandler { },
            ),
            entryTrackingMigrationBinding {
                mockk {
                    coEvery { prepareMigrationTracks(any(), any(), any()) } returns
                        EntryTrackingMigrationPreparationResult.Prepared(emptyList())
                }
            },
        ),
        durableExecutionBindings = listOf(
            entryDownloadMigrationBinding { mockk(relaxed = true) },
            entryMigrationCustomCoverBinding(mockk(relaxed = true)),
        ),
    )
    return DefaultEntryMigrationFeature(
        composition.featureGraphEvaluation,
        host,
        host,
        mockk { coEvery { refresh(any()) } returns refreshedResult },
        mockk {
            coEvery { participateInReplacementTransaction(any()) } returns
                EntryMergeMigrationReplacementResult.Applied
        },
        EntryMigrationOptionDiscovery(composition.featureExecutions),
        EntryMigrationTransitionPreparation(composition.featureExecutions),
        EntryMigrationDurableConsequences(composition.featureExecutions),
        mockk { coEvery { deliverOperation(any()) } returns EntryMigrationFollowUp.COMPLETE },
        clockMillis = { 999L },
    )
}

private val refreshedResult = EntrySourceRefreshResult.Refreshed(emptyList(), 0, 0, 0, false)
