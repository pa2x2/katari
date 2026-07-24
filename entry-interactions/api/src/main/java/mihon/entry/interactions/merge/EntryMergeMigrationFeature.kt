package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

/** Narrow cooperation boundary used by Entry Migration; it does not expose Merge membership. */
interface EntryMergeMigrationFeature {
    /** Applies Merge-owned membership changes while participating in the caller's database transaction. */
    suspend fun participateInReplacementTransaction(
        intent: EntryMergeMigrationReplacementIntent,
    ): EntryMergeMigrationReplacementResult
}

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
