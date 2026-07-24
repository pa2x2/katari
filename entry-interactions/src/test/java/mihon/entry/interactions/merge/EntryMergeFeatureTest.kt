package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.EntryMergeConsequenceStatusSnapshot
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.host.EntryMergePendingConsequence
import mihon.entry.interactions.host.EntryMergeProfileHost
import mihon.entry.interactions.host.EntryMergeProfileMoveHostTransition
import mihon.entry.interactions.validation.productionSubjectEvaluation
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

class EntryMergeFeatureTest {
    @Test
    fun `download ownership projection returns ordered concrete owners for the explicit profile`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val projection = EntryMergeDownloadOwnershipCoordinator(FakeEntryMergeHost(entries, listOf(membership)))

        projection.resolveDownloadOwners(EntryMergeSubject(7L, 2L)) shouldBe EntryMergeDownloadOwners(
            profileId = 7L,
            visibleEntryId = 1L,
            orderedOwners = entries,
        )
    }

    @Test
    fun `legacy notification resolution recovers profile without ambient state`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val navigation = EntryMergeNavigationCoordinator(FakeEntryMergeHost(entries, listOf(membership)))

        navigation.resolveLegacyNotification(2L) shouldBe EntryMergeNavigationProjection(
            requestedSubject = EntryMergeSubject(7L, 2L),
            visibleEntryId = 1L,
        )
        navigation.resolveLegacyNotification(99L) shouldBe null
    }

    @Test
    fun `migration replacement transfers one member and dissolves a depleted source group`() {
        val replacements = replacementGroups(
            currentEntryId = 2L,
            replacementEntryId = 3L,
            currentGroup = EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 2L)),
            replacementGroup = EntryMergeMembershipSnapshot(7L, 3L, listOf(3L, 4L)),
        )

        replacements shouldContainExactly listOf(
            EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 3L)),
        )
    }

    @Test
    fun `shared workflow prepares and commits Book entries without a type marker`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"))
        val host = FakeEntryMergeHost(entries)
        val feature = feature(host)

        val ready = feature.prepare(EntryMergePrepareIntent(entries))
            .shouldBeInstanceOf<EntryMergePreparationResult.Ready>()
        val result = feature.execute(
            EntryMergeCommitIntent(
                editReference = ready.editor.editReference,
                target = ready.editor.target,
                orderedEntries = ready.editor.entries.map(EntryMergeEditorEntry::reference),
            ),
        )

        result.shouldBeInstanceOf<EntryMergeExecutionResult.Applied>()
            .outcome.followUp shouldBe EntryMergeFollowUp.COMPLETE
        host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.CommitEditor>()
            .orderedEntries.size shouldBe 2
    }

    @Test
    fun `shared workflow rejects a mixed-type selection`() = runTest {
        val book = entry(1L, "book")
        val anime = entry(2L, "anime").copy(type = EntryType.ANIME)
        val feature = feature(FakeEntryMergeHost(listOf(book, anime)))

        feature.prepare(EntryMergePrepareIntent(listOf(book, anime))) shouldBe
            EntryMergePreparationResult.Rejected(EntryMergeRejection.MIXED_ENTRY_TYPES)
    }

    @Test
    fun `preparation rejects a persisted selection that is absent from authoritative state`() = runTest {
        val selected = entry(1L, "missing")
        val feature = feature(FakeEntryMergeHost(emptyList()))

        feature.prepare(EntryMergePrepareIntent(listOf(selected))) shouldBe
            EntryMergePreparationResult.Rejected(EntryMergeRejection.ENTRY_NOT_IN_EDITOR)
    }

    @Test
    fun `preparation rejects members of multiple existing groups`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"), entry(3L, "three"), entry(4L, "four"))
        val memberships = listOf(
            EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 2L)),
            EntryMergeMembershipSnapshot(7L, 3L, listOf(3L, 4L)),
        )
        val feature = feature(FakeEntryMergeHost(entries, memberships))

        feature.prepare(EntryMergePrepareIntent(listOf(entries[0], entries[2]))) shouldBe
            EntryMergePreparationResult.Rejected(EntryMergeRejection.MULTIPLE_EXISTING_GROUPS)
    }

    @Test
    fun `preparation rejects a standalone selection with no second editor member`() = runTest {
        val selected = entry(1L, "one")
        val feature = feature(FakeEntryMergeHost(listOf(selected)))

        feature.prepare(EntryMergePrepareIntent(listOf(selected))) shouldBe
            EntryMergePreparationResult.Rejected(EntryMergeRejection.TOO_FEW_ENTRIES)
    }

    @Test
    fun `unpersisted selection remains read only until the owned commit transition`() = runTest {
        val persisted = entry(1L, "persisted")
        val remote = entry(-1L, "remote").copy(favorite = false)
        val host = FakeEntryMergeHost(listOf(persisted))
        val feature = feature(host)

        val ready = feature.prepare(
            EntryMergePrepareIntent(
                selectedEntries = listOf(persisted, remote),
                preparations = listOf(EntryMergeMemberPreparationIntent(remote, listOf(4L, 5L))),
            ),
        ).shouldBeInstanceOf<EntryMergePreparationResult.Ready>()

        host.transitions.shouldBeEmpty()

        feature.execute(
            EntryMergeCommitIntent(
                editReference = ready.editor.editReference,
                target = ready.editor.target,
                orderedEntries = ready.editor.entries.map(EntryMergeEditorEntry::reference),
            ),
        )

        val transition = host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.CommitEditor>()
        transition.preparations.single().categoryIds shouldContainExactly listOf(4L, 5L)
        transition.expected.entries.single { it.persistedEntryId == null }.entry.url shouldBe remote.url
        transition.consequenceRequests.map { it.payload } shouldContainExactly
            listOf("ADDED_TO_LIBRARY:download=false")
    }

    @Test
    fun `existing membership expands where its selected member appears`() = runTest {
        val existing = listOf(entry(1L, "one"), entry(2L, "two"))
        val added = entry(3L, "three")
        val membership = EntryMergeMembershipSnapshot(7L, 1L, existing.map(Entry::id))
        val feature = feature(FakeEntryMergeHost(existing + added, listOf(membership)))

        val editor = feature.prepare(EntryMergePrepareIntent(listOf(added, existing.first())))
            .shouldBeInstanceOf<EntryMergePreparationResult.Ready>()
            .editor

        editor.entries.map { it.entry.id } shouldContainExactly listOf(3L, 1L, 2L)
        editor.entries.single { it.reference == editor.target }.entry.id shouldBe 1L
    }

    @Test
    fun `single existing member prepares the whole group for editing`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val feature = feature(FakeEntryMergeHost(entries, listOf(membership)))

        val editor = feature.prepare(EntryMergePrepareIntent(listOf(entries.last())))
            .shouldBeInstanceOf<EntryMergePreparationResult.Ready>()
            .editor

        editor.entries.map { it.entry.id } shouldContainExactly listOf(1L, 2L)
        editor.entries.single { it.reference == editor.target }.entry.id shouldBe 1L
    }

    @Test
    fun `editing an existing group can replace its target and remove the previous target`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"), entry(3L, "three"))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val host = FakeEntryMergeHost(entries, listOf(membership))
        val feature = feature(host)
        val editor = feature.prepare(EntryMergePrepareIntent(listOf(entries.first())))
            .shouldBeInstanceOf<EntryMergePreparationResult.Ready>()
            .editor
        val referencesById = editor.entries.associate { it.entry.id to it.reference }

        feature.execute(
            EntryMergeCommitIntent(
                editReference = editor.editReference,
                target = referencesById.getValue(2L),
                orderedEntries = editor.entries.map(EntryMergeEditorEntry::reference),
                removedEntries = setOf(referencesById.getValue(1L)),
            ),
        ).shouldBeInstanceOf<EntryMergeExecutionResult.Applied>()

        val transition = host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.CommitEditor>()
        val keysByEntryId = transition.expected.entries.associate { it.entry.id to it.key }
        transition.target shouldBe keysByEntryId.getValue(2L)
        transition.removedEntries shouldBe setOf(keysByEntryId.getValue(1L))
    }

    @Test
    fun `library removal activates only its applicable shared cleanup consequence`() = runTest {
        val entries = listOf(entry(1L, "one"), entry(2L, "two"))
        val membership = EntryMergeMembershipSnapshot(7L, 1L, entries.map(Entry::id))
        val host = FakeEntryMergeHost(entries, listOf(membership))
        val feature = feature(host)
        val editor = feature.prepare(EntryMergePrepareIntent(listOf(entries.first())))
            .shouldBeInstanceOf<EntryMergePreparationResult.Ready>()
            .editor
        val removed = editor.entries.single { it.entry.id == 2L }.reference

        feature.execute(
            EntryMergeCommitIntent(
                editReference = editor.editReference,
                target = editor.target,
                orderedEntries = editor.entries.map(EntryMergeEditorEntry::reference),
                libraryRemovalEntries = setOf(removed),
            ),
        ).shouldBeInstanceOf<EntryMergeExecutionResult.Applied>()

        host.transitions.single().shouldBeInstanceOf<EntryMergeHostTransition.CommitEditor>()
            .consequenceRequests.map { it.payload } shouldContainExactly
            listOf("REMOVED_FROM_LIBRARY,REMOVED_FROM_GROUP:download=false")
    }

    @Test
    fun `existing group execution rejects incomplete authoritative membership`() = runTest {
        val existing = entry(1L, "one")
        val membership = EntryMergeMembershipSnapshot(7L, 1L, listOf(1L, 2L))
        val feature = feature(FakeEntryMergeHost(listOf(existing), listOf(membership)))

        feature.execute(
            EntryMergeRemoveEntriesIntent(
                subject = EntryMergeSubject(7L, 1L),
                entryIds = setOf(2L),
                removeFromLibrary = false,
                removeDownloads = false,
            ),
        ) shouldBe EntryMergeExecutionResult.Conflict
    }

    private fun feature(host: FakeEntryMergeHost): EntryMergeFeature {
        val evaluation = productionSubjectEvaluation(
            type = EntryType.BOOK,
            feature = EntryMergeFeatureContributor,
            additionalContributors = listOf(
                EntryTrackingMergeContributor,
                EntryMergeCustomCoverContributor,
                EntryDownloadMergeContributor,
            ),
        )
        val durable = RecordingMergeDurableGateway()
        val delivery = mockk<EntryMergeConsequenceDelivery> {
            coEvery { deliverOperation(any()) } returns EntryMergeFollowUp.COMPLETE
        }
        return EntryMergeWorkflowCoordinator(evaluation, host, durable, delivery)
    }

    private fun entry(id: Long, suffix: String): Entry {
        return Entry.create().copy(
            id = id,
            profileId = 7L,
            type = EntryType.BOOK,
            source = 10L,
            url = "/$suffix",
            title = suffix,
            favorite = true,
        )
    }

    private class FakeEntryMergeHost(
        entries: List<Entry>,
        private val memberships: List<EntryMergeMembershipSnapshot> = emptyList(),
    ) : EntryMergeHost {
        private val entriesById = entries.filter { it.id > 0L }.associateBy(Entry::id)
        val transitions = mutableListOf<EntryMergeHostTransition>()

        override suspend fun resolveLegacyNotificationEntry(entryId: Long): Entry? = entriesById[entryId]

        override fun profile(profileId: Long): EntryMergeProfileHost {
            return object : EntryMergeProfileHost {
                override val profileId = profileId

                override suspend fun entries(entryIds: List<Long>): List<Entry> = entryIds.mapNotNull(entriesById::get)

                override suspend fun resolveEntryIdentity(entry: Entry): Entry? {
                    return entriesById.values.firstOrNull {
                        it.profileId == profileId && it.type == entry.type && it.source == entry.source &&
                            it.url == entry.url
                    }
                }

                override suspend fun membership(entryId: Long): EntryMergeMembershipSnapshot? {
                    return memberships.singleOrNull { entryId in it.orderedEntryIds }
                }

                override fun observeMembership(entryId: Long): Flow<EntryMergeMembershipSnapshot?> {
                    return flowOf(memberships.singleOrNull { entryId in it.orderedEntryIds })
                }

                override suspend fun memberships(): List<EntryMergeMembershipSnapshot> = memberships

                override fun observeMemberships(): Flow<List<EntryMergeMembershipSnapshot>> = flowOf(memberships)

                override suspend fun duplicateCandidates(entry: Entry): List<DuplicateEntryCandidate> = emptyList()

                override fun observeDuplicateCandidates(
                    entry: Flow<Entry>,
                ): Flow<List<DuplicateEntryCandidate>> = emptyFlow()

                override suspend fun applyTransition(
                    transition: EntryMergeHostTransition,
                ): EntryMergeHostTransitionResult {
                    transitions += transition
                    val visibleEntryId = when (transition) {
                        is EntryMergeHostTransition.CommitEditor -> {
                            transition.expected.entries.single { it.key == transition.target }.persistedEntryId ?: 99L
                        }
                        is EntryMergeHostTransition.ChangeExistingGroup -> transition.visibleEntryId
                        else -> error("Unsupported transition in workflow test: $transition")
                    }
                    return EntryMergeHostTransitionResult.Applied(visibleEntryId)
                }

                override suspend fun beginProfileMove(
                    transition: EntryMergeProfileMoveHostTransition,
                ): EntryMergeHostTransitionResult = EntryMergeHostTransitionResult.Applied(null)

                override suspend fun completeProfileMove(
                    transition: EntryMergeProfileMoveHostTransition,
                ): EntryMergeHostTransitionResult = EntryMergeHostTransitionResult.Applied(null)
            }
        }

        override suspend fun pendingConsequences(limit: Int): List<EntryMergePendingConsequence> = emptyList()

        override suspend fun acknowledgeConsequence(consequenceId: String) = Unit

        override suspend fun recordConsequenceFailure(
            consequenceId: String,
            message: String,
            retryAtMillis: Long,
        ) = Unit

        override suspend fun pendingConsequenceCount(operationId: String): Long = 0L

        override fun observeConsequenceStatus(): Flow<EntryMergeConsequenceStatusSnapshot> {
            return flowOf(EntryMergeConsequenceStatusSnapshot(0, 0, null))
        }

        override suspend fun makeConsequencesRetryable() = Unit
    }
}

private class RecordingMergeDurableGateway : EntryMergeDurablePreparationGateway {
    override suspend fun prepare(
        preparations: List<EntryMergeDurablePreparation>,
    ): EntryMergeDurablePreparationResult {
        return EntryMergeDurablePreparationResult.Prepared(
            preparations.map { preparation ->
                val target = preparation.target
                mihon.entry.interactions.host.EntryMergeConsequenceRequest(
                    memberKey = (target as? EntryMergeConsequenceTarget.EditorMember)?.key,
                    entryId = (target as? EntryMergeConsequenceTarget.PersistedEntry)?.id,
                    participantId = "test.merge-durable",
                    schemaVersion = 1,
                    payload = preparation.event.changes.joinToString(separator = ",") { it.name } +
                        ":download=${preparation.event.downloadRemovalRequested}",
                )
            },
        )
    }

    override suspend fun discard(requests: List<mihon.entry.interactions.host.EntryMergeConsequenceRequest>) = Unit
}
