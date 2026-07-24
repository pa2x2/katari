package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.host.EntryMergeConsequenceStatusSnapshot
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMergeHostTransition
import mihon.entry.interactions.host.EntryMergeHostTransitionResult
import mihon.entry.interactions.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.host.EntryMergePendingConsequence
import mihon.entry.interactions.host.EntryMergeProfileHost
import mihon.entry.interactions.host.EntryMergeProfileMoveHostTransition
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

internal class RecordingEntryMergeHost(
    entries: List<Entry>,
    private val memberships: List<EntryMergeMembershipSnapshot> = emptyList(),
    initialStatus: EntryMergeConsequenceStatusSnapshot = EntryMergeConsequenceStatusSnapshot(0, 0, null),
) : EntryMergeHost {
    private val entriesByProfile = entries.groupBy(Entry::profileId).mapValues { (_, values) ->
        values.associateBy(Entry::id)
    }
    val transitions = mutableListOf<EntryMergeHostTransition>()
    val profileMoveTransitions = mutableListOf<EntryMergeProfileMoveHostTransition>()
    val status = MutableStateFlow(initialStatus)
    var madeRetryable = false
    var transitionResult: EntryMergeHostTransitionResult = EntryMergeHostTransitionResult.Applied(null)

    override fun profile(profileId: Long): EntryMergeProfileHost {
        return object : EntryMergeProfileHost {
            override val profileId = profileId

            override suspend fun entries(entryIds: List<Long>): List<Entry> {
                val values = entriesByProfile[profileId].orEmpty()
                return entryIds.mapNotNull(values::get)
            }

            override suspend fun resolveEntryIdentity(entry: Entry): Entry? {
                return entriesByProfile[profileId].orEmpty().values.firstOrNull { candidate ->
                    candidate.source == entry.source && candidate.url == entry.url && candidate.type == entry.type
                }
            }

            override suspend fun membership(entryId: Long): EntryMergeMembershipSnapshot? {
                return memberships.singleOrNull { it.profileId == profileId && entryId in it.orderedEntryIds }
            }

            override fun observeMembership(entryId: Long): Flow<EntryMergeMembershipSnapshot?> {
                return flowOf(memberships.singleOrNull { it.profileId == profileId && entryId in it.orderedEntryIds })
            }

            override suspend fun memberships(): List<EntryMergeMembershipSnapshot> {
                return memberships.filter { it.profileId == profileId }
            }

            override fun observeMemberships(): Flow<List<EntryMergeMembershipSnapshot>> {
                return flowOf(memberships.filter { it.profileId == profileId })
            }

            override suspend fun duplicateCandidates(entry: Entry): List<DuplicateEntryCandidate> = emptyList()

            override fun observeDuplicateCandidates(
                entry: Flow<Entry>,
            ): Flow<List<DuplicateEntryCandidate>> = emptyFlow()

            override suspend fun applyTransition(
                transition: EntryMergeHostTransition,
            ): EntryMergeHostTransitionResult {
                transitions += transition
                return transitionResult
            }

            override suspend fun beginProfileMove(
                transition: EntryMergeProfileMoveHostTransition,
            ): EntryMergeHostTransitionResult {
                profileMoveTransitions += transition
                return transitionResult
            }

            override suspend fun completeProfileMove(
                transition: EntryMergeProfileMoveHostTransition,
            ): EntryMergeHostTransitionResult = transitionResult
        }
    }

    override suspend fun resolveLegacyNotificationEntry(entryId: Long): Entry? {
        return entriesByProfile.values.firstNotNullOfOrNull { it[entryId] }
    }

    override suspend fun pendingConsequences(limit: Int): List<EntryMergePendingConsequence> = emptyList()

    override suspend fun acknowledgeConsequence(consequenceId: String) = Unit

    override suspend fun recordConsequenceFailure(
        consequenceId: String,
        message: String,
        retryAtMillis: Long,
    ) = Unit

    override suspend fun pendingConsequenceCount(operationId: String): Long = 0

    override fun observeConsequenceStatus(): Flow<EntryMergeConsequenceStatusSnapshot> = status

    override suspend fun makeConsequencesRetryable() {
        madeRetryable = true
    }
}
