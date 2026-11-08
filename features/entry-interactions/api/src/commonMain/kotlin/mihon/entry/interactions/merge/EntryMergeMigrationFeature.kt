package mihon.entry.interactions.merge

import tachiyomi.domain.entry.model.Entry

/** Narrow cooperation boundary used by Entry Migration; it does not expose Merge membership. */
interface EntryMergeMigrationFeature {
    /** Applies Merge-owned membership changes while participating in the caller's database transaction. */
    suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult

    /**
     * Pairs the members [current]'s source supplies with the ones [replacement]'s source supplies, when both are
     * merged entries: by position when both list as many, otherwise by title. Members without a pair are left out.
     */
    suspend fun pairLockedMembers(current: Entry, replacement: Entry): List<EntryMergeMemberPair>
}

data class EntryMergeMemberPair(
    val current: Entry,
    val replacement: Entry,
)

data class EntryMergeMigrationReplacementIntent(
    val current: Entry,
    val replacement: Entry,
)

sealed interface EntryMergeMigrationReplacementResult {
    data object Applied : EntryMergeMigrationReplacementResult
    data object NoMembership : EntryMergeMigrationReplacementResult
    data object Conflict : EntryMergeMigrationReplacementResult

    data class OperationalFailure(
        val retryable: Boolean,
    ) : EntryMergeMigrationReplacementResult
}
