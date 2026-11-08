package app.katari.entry.interactions.migration

import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.download.EntryDownloadMaintenanceInspection
import app.katari.entry.interactions.download.EntryDownloadRemovalPreparation
import app.katari.entry.interactions.download.maintenance.migration.EntryDownloadMigrationParticipant
import app.katari.entry.interactions.durable.EntryDurableConsequences
import app.katari.entry.interactions.merge.EntryMergeMemberPair
import app.katari.entry.interactions.merge.EntryMergeMigrationFeature
import app.katari.entry.interactions.merge.EntryMergeMigrationReplacementIntent
import app.katari.entry.interactions.merge.EntryMergeMigrationReplacementResult
import app.katari.entry.interactions.migration.consequence.EntryMigrationConsequenceDelivery
import app.katari.entry.interactions.migration.consequence.cover.EntryMigrationCustomCoverParticipant
import app.katari.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionHost
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionInspectionResult
import app.katari.entry.interactions.migration.host.EntryMigrationExecutionProfileHost
import app.katari.entry.interactions.migration.host.EntryMigrationHostInspectionResult
import app.katari.entry.interactions.migration.host.EntryMigrationHostOperation
import app.katari.entry.interactions.migration.host.EntryMigrationHostReplayResult
import app.katari.entry.interactions.migration.host.EntryMigrationHostTransition
import app.katari.entry.interactions.migration.host.EntryMigrationHostTransitionResult
import app.katari.entry.interactions.migration.host.EntryMigrationPreparationHost
import app.katari.entry.interactions.migration.host.EntryMigrationPreparationProfileHost
import app.katari.entry.interactions.source.EntrySourceRefreshFailure
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshResult
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
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
    fun `a synchronization failure cannot enter the migration transaction`() = runTest {
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
    ): EntryMigrationFeature {
        val downloadFeature = mockk<EntryDownloadMaintenanceFeature>().also {
            coEvery { it.inspectEntry(any()) } returns
                EntryDownloadMaintenanceInspection.Inapplicable(EntryType.BOOK)
            coEvery { it.prepareRemoval(any()) } returns
                EntryDownloadRemovalPreparation.Inapplicable(EntryType.BOOK)
        }
        val customCoverHost = mockk<EntryMigrationCustomCoverHost>(relaxed = true)
        val delivery = mockk<EntryMigrationConsequenceDelivery>().also {
            coEvery { it.deliverOperation(any()) } returns EntryMigrationFollowUp.INCOMPLETE
        }
        return DefaultEntryMigrationFeature(
            types = EntryMigrationTypes(
                migration = setOf(EntryType.BOOK),
                consumption = emptySet(),
                bookmark = emptySet(),
                childStateOption = emptySet(),
            ),
            preparationHost = host,
            executionHost = host,
            sourceRefresh = sourceRefresh,
            mergeMigration = NoOpMergeMigrationFeature,
            mergeLibraryLifecycle = { mockk() },
            downloads = { downloadFeature },
            durableConsequences = EntryDurableConsequences(
                setOf(
                    EntryDownloadMigrationParticipant { downloadFeature },
                    EntryMigrationCustomCoverParticipant(customCoverHost),
                ),
            ),
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
        joinedEntryIds = emptySet(),
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
}

private object NoOpMergeMigrationFeature : EntryMergeMigrationFeature {
    override suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult = EntryMergeMigrationReplacementResult.Applied

    override suspend fun pairLockedMembers(current: Entry, replacement: Entry): List<EntryMergeMemberPair> = emptyList()

    override suspend fun lockedMembers(root: Entry): List<Entry> = emptyList()
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
