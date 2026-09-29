package mihon.entry.interactions.migration

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceInspection
import mihon.entry.interactions.download.EntryDownloadRemovalPreparation
import mihon.entry.interactions.download.maintenance.migration.ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT
import mihon.entry.interactions.download.maintenance.migration.EntryDownloadMigrationContributor
import mihon.entry.interactions.download.maintenance.migration.entryDownloadMigrationBinding
import mihon.entry.interactions.merge.EntryMergeMigrationFeature
import mihon.entry.interactions.merge.EntryMergeMigrationReplacementIntent
import mihon.entry.interactions.merge.EntryMergeMigrationReplacementResult
import mihon.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableConsequences
import mihon.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverContributor
import mihon.entry.interactions.migration.consequence.cover.entryMigrationCustomCoverBinding
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionInspectionResult
import mihon.entry.interactions.migration.host.EntryMigrationExecutionProfileHost
import mihon.entry.interactions.migration.host.EntryMigrationHostInspectionResult
import mihon.entry.interactions.migration.host.EntryMigrationHostOperation
import mihon.entry.interactions.migration.host.EntryMigrationHostReplayResult
import mihon.entry.interactions.migration.host.EntryMigrationHostTransition
import mihon.entry.interactions.migration.host.EntryMigrationHostTransitionResult
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationProfileHost
import mihon.entry.interactions.migration.options.EntryMigrationOptionDiscovery
import mihon.entry.interactions.migration.preparation.EntryMigrationTransitionPreparation
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshResult
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMigrationPreparationResult
import mihon.entry.interactions.tracking.EntryTrackingRecord
import mihon.entry.interactions.tracking.migration.EntryTrackingMigrationContributor
import mihon.entry.interactions.tracking.migration.entryTrackingMigrationBinding
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryMigrationFeatureTest {
    private val source = entry(id = 10, sourceId = 100, favorite = true, dateAdded = 50)
    private val target = entry(id = 20, sourceId = 200, favorite = false)

    @Test
    fun `committed operation replay bypasses changed source state and target synchronization`() = runTest {
        val host = RecordingMigrationHost(source, target)
        val sourceRefresh = refreshedSourceRefresh()
        val feature = feature(host, sourceRefresh = sourceRefresh)
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()
        host.replayResult = EntryMigrationHostReplayResult.Applied(hasPendingConsequences = true)
        host.preparationSource = source.copy(favorite = false)

        val result = feature.execute(
            EntryMigrationExecuteIntent(
                preparation.reference,
                EntryMigrationMode.REPLACE,
                emptySet(),
            ),
        ).shouldBeInstanceOf<EntryMigrationExecutionResult.Applied>()

        result.outcome.followUp shouldBe EntryMigrationFollowUp.INCOMPLETE
        coVerify(exactly = 0) { sourceRefresh.refresh(any()) }
        host.transitions shouldBe emptyList()
    }

    @Test
    fun `synchronization and tracking preparation failures cannot enter the migration transaction`() = runTest {
        val syncHost = RecordingMigrationHost(source, target)
        val sourceRefresh = mockk<EntrySourceRefreshFeature> {
            coEvery { refresh(any()) } returns
                EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(IllegalStateException("refresh")))
        }
        val syncFeature = feature(syncHost, sourceRefresh = sourceRefresh)
        val syncPreparation = syncFeature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        syncFeature.execute(
            EntryMigrationExecuteIntent(syncPreparation.reference, EntryMigrationMode.COPY, emptySet()),
        ) shouldBe EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        syncHost.transitions shouldBe emptyList()

        val trackingHost = RecordingMigrationHost(source, target)
        val tracking = mockk<EntryTrackingFeature> {
            coEvery { prepareMigrationTracks(any(), any(), any()) } returns
                EntryTrackingMigrationPreparationResult.Failed(IllegalStateException("tracking unavailable"))
        }
        val trackingFeature = feature(trackingHost, tracking = tracking)
        val trackingPreparation = trackingFeature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        trackingFeature.execute(
            EntryMigrationExecuteIntent(trackingPreparation.reference, EntryMigrationMode.COPY, emptySet()),
        ) shouldBe EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        trackingHost.transitions shouldBe emptyList()
    }

    @Test
    fun `execution cancellation propagates`() = runTest {
        val host = RecordingMigrationHost(source, target)
        val sourceRefresh = mockk<EntrySourceRefreshFeature> {
            coEvery { refresh(any()) } throws CancellationException("cancelled")
        }
        val feature = feature(host, sourceRefresh = sourceRefresh)
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        shouldThrow<CancellationException> {
            feature.execute(
                EntryMigrationExecuteIntent(
                    preparation.reference,
                    EntryMigrationMode.COPY,
                    emptySet(),
                ),
            )
        }
    }

    private fun feature(
        host: RecordingMigrationHost,
        sourceRefresh: EntrySourceRefreshFeature = refreshedSourceRefresh(),
        tracking: EntryTrackingFeature? = null,
    ): EntryMigrationFeature {
        val downloadFeature = mockk<EntryDownloadMaintenanceFeature>().also {
            coEvery { it.inspectEntry(any()) } returns
                EntryDownloadMaintenanceInspection.Inapplicable(EntryType.BOOK)
            coEvery { it.prepareRemoval(any()) } returns
                EntryDownloadRemovalPreparation.Inapplicable(EntryType.BOOK)
        }
        val customCoverHost = mockk<EntryMigrationCustomCoverHost>(relaxed = true)
        val trackingFeature = tracking ?: mockk<EntryTrackingFeature>().also {
            coEvery { it.prepareMigrationTracks(any(), any(), any()) } answers {
                val target = args[1] as Entry

                @Suppress("UNCHECKED_CAST")
                val tracks = args[2] as List<EntryTrackingRecord>
                EntryTrackingMigrationPreparationResult.Prepared(
                    tracks.map { track -> track.copy(entryId = target.id) },
                )
            }
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.migration-type")
                    override val providerBindings = listOf(EntryMigrationCapability.bind(MigrationProvider()))
                },
            ),
            featureContributors = listOf(
                EntryMigrationFeatureContributor,
                EntryDownloadMigrationContributor,
                EntryMigrationCustomCoverContributor,
                EntryTrackingMigrationContributor,
            ),
            executionBindings = listOf(
                entryTrackingMigrationBinding { trackingFeature },
                FeatureExecutionParticipantBinding(
                    definition = ENTRY_DOWNLOAD_MIGRATION_OPTION_PARTICIPANT,
                    handler = FeatureExecutionHandler { event ->
                        if (downloadFeature.inspectEntry(event.source) ==
                            EntryDownloadMaintenanceInspection.HasDownloads
                        ) {
                            event.options.add(EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS)
                        }
                    },
                ),
            ),
            durableExecutionBindings = listOf(
                entryDownloadMigrationBinding { downloadFeature },
                entryMigrationCustomCoverBinding(customCoverHost),
            ),
        )
        val delivery = mockk<EntryMigrationConsequenceDelivery>().also {
            coEvery { it.deliverOperation(any()) } returns EntryMigrationFollowUp.INCOMPLETE
        }
        return DefaultEntryMigrationFeature(
            evaluation = composition.featureGraphEvaluation,
            preparationHost = host,
            executionHost = host,
            sourceRefresh = sourceRefresh,
            mergeMigration = NoOpMergeMigrationFeature,
            optionDiscovery = EntryMigrationOptionDiscovery(composition.featureExecutions),
            transitionPreparation = EntryMigrationTransitionPreparation(composition.featureExecutions),
            durableConsequences = EntryMigrationDurableConsequences(composition.featureExecutions),
            consequences = delivery,
            clockMillis = { 999 },
        )
    }

    private fun refreshedSourceRefresh() = mockk<EntrySourceRefreshFeature> {
        coEvery { refresh(any()) } returns refreshedSourceResult()
    }

    private fun refreshedSourceResult() = EntrySourceRefreshResult.Refreshed(
        insertedChildren = emptyList(),
        insertedChildrenTotal = 0,
        updatedChildren = 0,
        removedChildren = 0,
        metadataChanged = false,
    )

    private fun entry(id: Long, sourceId: Long, favorite: Boolean, dateAdded: Long = 0): Entry {
        return Entry.create().copy(
            id = id,
            profileId = 4,
            source = sourceId,
            url = "entry-$id",
            title = "Entry $id",
            favorite = favorite,
            dateAdded = dateAdded,
            type = EntryType.BOOK,
        )
    }

    private class MigrationProvider : EntryMigrationProvider {
        override val type = EntryType.BOOK
    }
}

private object NoOpMergeMigrationFeature : EntryMergeMigrationFeature {
    override suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult = EntryMergeMigrationReplacementResult.Applied
}

private class RecordingMigrationHost(
    source: Entry,
    private val target: Entry,
) : EntryMigrationPreparationHost, EntryMigrationExecutionHost {
    var preparationSource = source
    var replayResult: EntryMigrationHostReplayResult = EntryMigrationHostReplayResult.NotApplied
    val transitions = mutableListOf<EntryMigrationHostTransition>()

    override fun profile(profileId: Long): ProfileHost = ProfileHost()

    inner class ProfileHost : EntryMigrationPreparationProfileHost, EntryMigrationExecutionProfileHost {
        override suspend fun inspectPair(
            sourceEntryId: Long,
            targetEntryId: Long,
        ): EntryMigrationHostInspectionResult {
            return EntryMigrationHostInspectionResult.Ready(
                source = preparationSource,
                target = target,
                sourceCategoryIds = emptyList(),
                sourceHasCustomCover = false,
            )
        }

        override suspend fun replay(operation: EntryMigrationHostOperation): EntryMigrationHostReplayResult {
            return replayResult
        }

        override suspend fun inspectExecution(
            sourceEntryId: Long,
            targetEntryId: Long,
        ): EntryMigrationExecutionInspectionResult.Ready {
            return EntryMigrationExecutionInspectionResult.Ready(
                source = preparationSource,
                target = target,
                sourceChildren = emptyList(),
                targetChildren = emptyList(),
                sourceCategoryIds = emptyList(),
                sourceTracks = emptyList(),
            )
        }

        override suspend fun applyTransition(
            transition: EntryMigrationHostTransition,
            participateMergeReplacement: (suspend () -> EntryMergeMigrationReplacementResult)?,
        ): EntryMigrationHostTransitionResult {
            transitions += transition
            participateMergeReplacement?.invoke()
            return EntryMigrationHostTransitionResult.Applied(replayed = false, hasPendingConsequences = false)
        }
    }
}
