package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMigrationExecutionHost
import mihon.entry.interactions.host.EntryMigrationExecutionInspectionResult
import mihon.entry.interactions.host.EntryMigrationExecutionProfileHost
import mihon.entry.interactions.host.EntryMigrationHostInspectionResult
import mihon.entry.interactions.host.EntryMigrationHostOperation
import mihon.entry.interactions.host.EntryMigrationHostReplayResult
import mihon.entry.interactions.host.EntryMigrationHostTransition
import mihon.entry.interactions.host.EntryMigrationHostTransitionResult
import mihon.entry.interactions.host.EntryMigrationPreparationHost
import mihon.entry.interactions.host.EntryMigrationPreparationProfileHost
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.featureGraphContributor
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryMigrationFeatureTest {
    private val source = entry(id = 10, sourceId = 100, favorite = true, dateAdded = 50)
    private val target = entry(id = 20, sourceId = 200, favorite = false)
    private val sourceChild = child(id = 11, entryId = source.id, read = true, bookmark = false, dateFetch = 90)
    private val targetChild = child(id = 21, entryId = target.id, read = false, bookmark = true, dateFetch = 10)

    @Test
    fun `provider absence is valid and makes migration unavailable`() {
        val feature = feature(RecordingMigrationHost(source, target), bindings = emptyList())

        feature.availability(source) shouldBe
            EntryMigrationAvailability.Unavailable(EntryMigrationRejection.UNSUPPORTED_SOURCE_TYPE)
    }

    @Test
    fun `current source state context controls migration availability`() {
        val feature = feature(RecordingMigrationHost(source, target))

        feature.availability(source) shouldBe EntryMigrationAvailability.Available
        feature.availability(source.copy(favorite = false)) shouldBe
            EntryMigrationAvailability.Unavailable(EntryMigrationRejection.SOURCE_NOT_IN_LIBRARY)
        feature.availability(source.copy(id = 0L)) shouldBe
            EntryMigrationAvailability.Unavailable(EntryMigrationRejection.UNPERSISTED_ENTRY)
    }

    @Test
    fun `selection context preserves single-profile readiness and mixed-profile rejection`() {
        val feature = feature(RecordingMigrationHost(source, target))
        val second = source.copy(id = 12L, source = 101L, url = "entry-12")

        feature.prepareSelection(listOf(source, second)) shouldBe EntryMigrationSelectionResult.Ready(
            listOf(
                EntryMigrationSubject(source.profileId, source.id),
                EntryMigrationSubject(second.profileId, second.id),
            ),
        )
        feature.prepareSelection(listOf(source, second.copy(profileId = 5L))) shouldBe
            EntryMigrationSelectionResult.Rejected(EntryMigrationRejection.MIXED_SELECTION_PROFILES)
    }

    @Test
    fun `preparation derives options from current state and optional relationships`() = runTest {
        val preparedSource = source.copy(notes = "keep")
        val host = RecordingMigrationHost(preparedSource, target).apply {
            categories = listOf(3, 7)
        }
        val feature = feature(
            host,
            bindings = listOf(
                EntryMigrationCapability.bind(MigrationProvider()),
                EntryConsumptionCapability.bind(ConsumptionProvider()),
                EntryBookmarkCapability.bind(BookmarkProvider()),
            ),
        )

        val result = feature.prepare(EntryMigrationPrepareIntent(preparedSource, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        result.availableOptions.shouldContainExactlyInAnyOrder(
            EntryMigrationOption.CHILD_STATE,
            EntryMigrationOption.CATEGORIES,
            EntryMigrationOption.NOTES,
        )
    }

    @Test
    fun `pair context preserves profile type and identity rejections`() = runTest {
        val feature = feature(RecordingMigrationHost(source, target))

        feature.prepare(EntryMigrationPrepareIntent(source, target.copy(profileId = 5L))) shouldBe
            EntryMigrationPreparationResult.Rejected(EntryMigrationRejection.SOURCE_TARGET_PROFILE_MISMATCH)
        feature.prepare(EntryMigrationPrepareIntent(source, target.copy(type = EntryType.ANIME))) shouldBe
            EntryMigrationPreparationResult.Rejected(EntryMigrationRejection.SOURCE_TARGET_TYPE_MISMATCH)
        feature.prepare(EntryMigrationPrepareIntent(source, source)) shouldBe
            EntryMigrationPreparationResult.Rejected(EntryMigrationRejection.SAME_ENTRY)
    }

    @Test
    fun `download option requires both provider participation and current downloads`() = runTest {
        val downloadProvider = mockk<EntryDownloadProcessor>(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
        val downloadFeature = mockk<EntryDownloadMaintenanceFeature> {
            coEvery { inspectEntry(any()) } returns EntryDownloadMaintenanceInspection.HasDownloads
        }
        val feature = feature(
            host = RecordingMigrationHost(source, target),
            bindings = listOf(
                EntryMigrationCapability.bind(MigrationProvider()),
                EntryDownloadCapability.bind(downloadProvider),
            ),
            downloads = downloadFeature,
        )

        val result = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        result.availableOptions.shouldContainExactly(EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS)
    }

    @Test
    fun `replace commits shared primary state and delegates merge membership`() = runTest {
        val preparedSource = source.copy(notes = "keep", chapterFlags = 42)
        val host = RecordingMigrationHost(preparedSource, target).apply {
            categories = listOf(3, 7)
            sourceChildren = listOf(sourceChild)
            targetChildren = listOf(targetChild)
        }
        val merge = RecordingMergeMigrationFeature()
        val sourceRefresh = refreshedSourceRefresh()
        val feature = feature(
            host,
            merge,
            listOf(
                EntryMigrationCapability.bind(MigrationProvider()),
                EntryConsumptionCapability.bind(ConsumptionProvider()),
                EntryBookmarkCapability.bind(BookmarkProvider()),
            ),
            sourceRefresh = sourceRefresh,
        )
        val preparation = feature.prepare(EntryMigrationPrepareIntent(preparedSource, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        val result = feature.execute(
            EntryMigrationExecuteIntent(
                preparation.reference,
                EntryMigrationMode.REPLACE,
                setOf(EntryMigrationOption.CHILD_STATE, EntryMigrationOption.CATEGORIES, EntryMigrationOption.NOTES),
            ),
        ).shouldBeInstanceOf<EntryMigrationExecutionResult.Applied>()

        result.outcome.followUp shouldBe EntryMigrationFollowUp.COMPLETE
        coVerify(exactly = 1) { sourceRefresh.refresh(match { it.entry == target }) }
        val transition = host.transitions.single()
        transition.sourceUpdate shouldBe preparedSource.copy(favorite = false, dateAdded = 0)
        transition.targetUpdate shouldBe target.copy(
            favorite = true,
            chapterFlags = 42,
            dateAdded = 50,
            notes = "keep",
        )
        transition.targetCategoryIds.shouldContainExactly(3, 7)
        transition.childUpdates.single().updated shouldBe targetChild.copy(
            read = true,
            bookmark = false,
            dateFetch = 90,
        )
        merge.intents.single() shouldBe EntryMergeMigrationReplacementIntent(preparedSource, target)
    }

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
            EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.REPLACE, emptySet()),
        ).shouldBeInstanceOf<EntryMigrationExecutionResult.Applied>()

        result.outcome.followUp shouldBe EntryMigrationFollowUp.INCOMPLETE
        coVerify(exactly = 0) { sourceRefresh.refresh(any()) }
        host.transitions shouldBe emptyList()
    }

    @Test
    fun `changed live authorization blocks an uncommitted execution before synchronization`() = runTest {
        val host = RecordingMigrationHost(source, target)
        val sourceRefresh = refreshedSourceRefresh()
        val feature = feature(host, sourceRefresh = sourceRefresh)
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()
        host.preparationSource = source.copy(favorite = false)

        feature.execute(
            EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.REPLACE, emptySet()),
        ) shouldBe EntryMigrationExecutionResult.Conflict

        coVerify(exactly = 0) { sourceRefresh.refresh(any()) }
        host.transitions shouldBe emptyList()
    }

    @Test
    fun `strict synchronization failure cannot be reported as applied`() = runTest {
        val host = RecordingMigrationHost(source, target)
        val failure = IllegalStateException("refresh failed")
        val sourceRefresh = mockk<EntrySourceRefreshFeature> {
            coEvery { refresh(any()) } returns
                EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(failure))
        }
        val feature = feature(host, sourceRefresh = sourceRefresh)
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        feature.execute(
            EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.COPY, emptySet()),
        ) shouldBe EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        host.transitions shouldBe emptyList()
    }

    @Test
    fun `tracking preparation failure cannot enter the migration transaction`() = runTest {
        val host = RecordingMigrationHost(source, target)
        val tracking = mockk<EntryTrackingFeature> {
            coEvery { prepareMigrationTracks(any(), any(), any()) } returns
                EntryTrackingMigrationPreparationResult.Failed(IllegalStateException("tracking unavailable"))
        }
        val feature = feature(host, tracking = tracking)
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        feature.execute(
            EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.COPY, emptySet()),
        ) shouldBe EntryMigrationExecutionResult.OperationalFailure(retryable = true)
        host.transitions shouldBe emptyList()
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
                EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.COPY, emptySet()),
            )
        }
    }

    @Test
    fun `target refresh relationship returns structured outcomes`() = runTest {
        val sourceRefresh = mockk<EntrySourceRefreshFeature>()
        val feature = feature(RecordingMigrationHost(source, target), sourceRefresh = sourceRefresh)
        val intent = EntryMigrationTargetRefreshIntent(
            source = source,
            target = target,
            fetchDetails = false,
            fetchChildren = true,
        )

        coEvery { sourceRefresh.refresh(any()) } returns refreshedSourceResult()
        feature.refreshTarget(intent) shouldBe EntryMigrationTargetRefreshResult.Refreshed
        coVerify {
            sourceRefresh.refresh(
                match {
                    it.entry == target && !it.fetchDetails && it.fetchChildren &&
                        it.entry.profileId == target.profileId
                },
            )
        }

        coEvery { sourceRefresh.refresh(any()) } returns EntrySourceRefreshResult.SourceUnavailable(target.source)
        feature.refreshTarget(intent) shouldBe EntryMigrationTargetRefreshResult.SourceUnavailable

        coEvery { sourceRefresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)
        feature.refreshTarget(intent) shouldBe EntryMigrationTargetRefreshResult.NoChildren
    }

    @Test
    fun `one applicable owner adds only its immutable consequence`() = runTest {
        val host = RecordingMigrationHost(source, target).apply {
            transitionResult = EntryMigrationHostTransitionResult.Applied(
                replayed = false,
                hasPendingConsequences = true,
            )
        }
        val progress = mockk<EntryProgressFeature>()
        coEvery { progress.prepareMigration(any(), any(), any()) } returns
            EntryProgressMigrationPreparation.Prepared(
                EntryProgressMigrationPayload(target, EntryProgressSnapshot()),
            )
        val delivery = mockk<EntryMigrationConsequenceDelivery>()
        coEvery { delivery.deliverOperation(any()) } returns EntryMigrationFollowUp.COMPLETE
        val feature = feature(
            host = host,
            bindings = listOf(
                EntryMigrationCapability.bind(MigrationProvider()),
                EntryProgressCapability.bind(ProgressProvider()),
            ),
            progress = progress,
            consequenceDelivery = delivery,
        )
        val preparation = feature.prepare(EntryMigrationPrepareIntent(source, target))
            .shouldBeInstanceOf<EntryMigrationPreparationResult.Ready>()

        feature.execute(
            EntryMigrationExecuteIntent(preparation.reference, EntryMigrationMode.COPY, emptySet()),
        ).shouldBeInstanceOf<EntryMigrationExecutionResult.Applied>()

        host.transitions.single().consequenceRequests.map { it.participantId } shouldBe
            listOf(ENTRY_PROGRESS_MIGRATION_PARTICIPANT.id.value)
        coVerify(exactly = 1) { delivery.deliverOperation(any()) }
    }

    private fun feature(
        host: RecordingMigrationHost,
        merge: RecordingMergeMigrationFeature = RecordingMergeMigrationFeature(),
        bindings: List<EntryInteractionProviderBinding<*>> = listOf(
            EntryMigrationCapability.bind(MigrationProvider()),
        ),
        progress: EntryProgressFeature? = null,
        downloads: EntryDownloadMaintenanceFeature? = null,
        consequenceDelivery: EntryMigrationConsequenceDelivery? = null,
        sourceRefresh: EntrySourceRefreshFeature = refreshedSourceRefresh(),
        tracking: EntryTrackingFeature? = null,
    ): EntryMigrationFeature {
        val downloadFeature = downloads ?: mockk<EntryDownloadMaintenanceFeature>().also {
            coEvery { it.inspectEntry(any()) } returns
                EntryDownloadMaintenanceInspection.Inapplicable(EntryType.BOOK)
            coEvery { it.prepareRemoval(any()) } returns
                EntryDownloadRemovalPreparation.Inapplicable(EntryType.BOOK)
        }
        val customCoverHost = mockk<mihon.entry.interactions.host.EntryMigrationCustomCoverHost>(relaxed = true)
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
        val featureContributors = buildList {
            add(EntryMigrationFeatureContributor)
            add(EntryDownloadMigrationContributor)
            add(EntryMigrationCustomCoverContributor)
            add(EntryTrackingMigrationContributor)
            if (progress != null) {
                add(featureGraphContributor(ENTRY_PROGRESS_FEATURE_OWNER) { add(ENTRY_PROGRESS_MIGRATION_PARTICIPANT) })
            }
        }
        val executionBindings = buildList {
            add(entryTrackingMigrationBinding { trackingFeature })
            add(
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
            )
        }
        val durableBindings = buildList {
            progress?.let { add(entryProgressMigrationBinding { it }) }
            add(entryDownloadMigrationBinding { downloadFeature })
            add(entryMigrationCustomCoverBinding(customCoverHost))
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.migration-type")
                    override val providerBindings = bindings
                },
            ),
            featureContributors = featureContributors,
            executionBindings = executionBindings,
            durableExecutionBindings = durableBindings,
        )
        val delivery = consequenceDelivery ?: mockk<EntryMigrationConsequenceDelivery>().also {
            coEvery { it.deliverOperation(any()) } returns EntryMigrationFollowUp.INCOMPLETE
        }
        return DefaultEntryMigrationFeature(
            evaluation = composition.featureGraphEvaluation,
            preparationHost = host,
            executionHost = host,
            sourceRefresh = sourceRefresh,
            mergeMigration = merge,
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

    private fun child(
        id: Long,
        entryId: Long,
        read: Boolean,
        bookmark: Boolean,
        dateFetch: Long,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "child-$id",
            name = "Child",
            chapterNumber = 1.0,
            read = read,
            bookmark = bookmark,
            dateFetch = dateFetch,
        )
    }

    private class MigrationProvider : EntryMigrationProvider {
        override val type = EntryType.BOOK
    }

    private class ConsumptionProvider : EntryConsumptionProcessor {
        override val type = EntryType.BOOK

        override suspend fun setConsumed(
            entry: Entry,
            chapters: List<EntryChapter>,
            consumed: Boolean,
        ): List<EntryChapter> = emptyList()
    }

    private class BookmarkProvider : EntryBookmarkProcessor {
        override val type = EntryType.BOOK

        override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) = Unit
    }

    private class ProgressProvider : EntryProgressProcessor {
        override val type = EntryType.BOOK

        override suspend fun snapshot(entry: Entry) = EntryProgressSnapshot()
        override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) = Unit
        override suspend fun copy(
            sourceEntry: Entry,
            targetEntry: Entry,
            resourceMappings: List<EntryProgressResourceMapping>,
        ) = Unit
    }
}

private class RecordingMergeMigrationFeature : EntryMergeMigrationFeature {
    val intents = mutableListOf<EntryMergeMigrationReplacementIntent>()

    override suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult {
        intents += intent
        return EntryMergeMigrationReplacementResult.Applied
    }
}

private class RecordingMigrationHost(
    source: Entry,
    private val target: Entry,
) : EntryMigrationPreparationHost, EntryMigrationExecutionHost {
    var preparationSource = source
    var categories = emptyList<Long>()
    var sourceChildren = emptyList<EntryChapter>()
    var targetChildren = emptyList<EntryChapter>()
    var replayResult: EntryMigrationHostReplayResult = EntryMigrationHostReplayResult.NotApplied
    var transitionResult: EntryMigrationHostTransitionResult =
        EntryMigrationHostTransitionResult.Applied(replayed = false, hasPendingConsequences = false)
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
                sourceCategoryIds = categories,
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
                sourceChildren = sourceChildren,
                targetChildren = targetChildren,
                sourceCategoryIds = categories,
                sourceTracks = emptyList(),
            )
        }

        override suspend fun applyTransition(
            transition: EntryMigrationHostTransition,
            participateMergeReplacement: (suspend () -> EntryMergeMigrationReplacementResult)?,
        ): EntryMigrationHostTransitionResult {
            transitions += transition
            participateMergeReplacement?.invoke()
            return transitionResult
        }
    }
}
