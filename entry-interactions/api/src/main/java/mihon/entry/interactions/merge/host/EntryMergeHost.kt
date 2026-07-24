package mihon.entry.interactions.host

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry

/**
 * Host boundary available to the Merge coordinator, not to application feature consumers.
 *
 * Selecting a profile produces an explicitly scoped host. No operation consults ambient active-profile state.
 */
interface EntryMergeHost {
    fun profile(profileId: Long): EntryMergeProfileHost

    /** Resolves an old notification payload that predates explicit profile identity. */
    suspend fun resolveLegacyNotificationEntry(entryId: Long): Entry?

    suspend fun pendingConsequences(limit: Int): List<EntryMergePendingConsequence>

    suspend fun acknowledgeConsequence(consequenceId: String)

    suspend fun recordConsequenceFailure(
        consequenceId: String,
        message: String,
        retryAtMillis: Long,
    )

    suspend fun pendingConsequenceCount(operationId: String): Long

    fun observeConsequenceStatus(): Flow<EntryMergeConsequenceStatusSnapshot>

    suspend fun makeConsequencesRetryable()
}

interface EntryMergeProfileHost {
    val profileId: Long

    suspend fun entries(entryIds: List<Long>): List<Entry>

    suspend fun resolveEntryIdentity(entry: Entry): Entry?

    suspend fun membership(entryId: Long): EntryMergeMembershipSnapshot?

    fun observeMembership(entryId: Long): Flow<EntryMergeMembershipSnapshot?>

    suspend fun memberships(): List<EntryMergeMembershipSnapshot>

    fun observeMemberships(): Flow<List<EntryMergeMembershipSnapshot>>

    suspend fun duplicateCandidates(entry: Entry): List<DuplicateEntryCandidate>

    fun observeDuplicateCandidates(entry: Flow<Entry>): Flow<List<DuplicateEntryCandidate>>

    suspend fun applyTransition(transition: EntryMergeHostTransition): EntryMergeHostTransitionResult

    suspend fun beginProfileMove(transition: EntryMergeProfileMoveHostTransition): EntryMergeHostTransitionResult

    suspend fun completeProfileMove(transition: EntryMergeProfileMoveHostTransition): EntryMergeHostTransitionResult
}
