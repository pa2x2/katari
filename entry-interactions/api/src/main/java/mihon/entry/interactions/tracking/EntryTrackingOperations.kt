package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryTrackingOperations {
    suspend fun refresh(entry: Entry): EntryTrackingRefreshResult

    suspend fun search(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        query: String,
    ): EntryTrackingSearchResult

    suspend fun register(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        candidate: EntryTrackingSearchCandidate,
        private: Boolean,
    ): EntryTrackingOperationResult

    suspend fun registerAutomatically(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
    ): EntryTrackingAutomaticRegistrationResult

    suspend fun mutate(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        mutation: EntryTrackingMutation,
    ): EntryTrackingOperationResult

    suspend fun remove(
        entry: Entry,
        serviceId: EntryTrackingServiceId,
        removeRemote: Boolean,
    ): EntryTrackingRemovalResult
}

sealed interface EntryTrackingOperationResult {
    data object Completed : EntryTrackingOperationResult

    data class Unavailable(
        val reason: EntryTrackingOperationUnavailableReason,
    ) : EntryTrackingOperationResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingOperationResult
}

enum class EntryTrackingOperationUnavailableReason {
    SERVICE_NOT_REGISTERED,
    UNSUPPORTED_ENTRY_TYPE,
    NOT_LOGGED_IN,
    SOURCE_NOT_ACCEPTED,
    TRACK_NOT_FOUND,
    AUTOMATIC_BINDING_UNSUPPORTED,
    READING_DATES_UNSUPPORTED,
    PRIVATE_TRACKING_UNSUPPORTED,
    REMOTE_DELETION_UNSUPPORTED,
    INVALID_STATUS,
    INVALID_SCORE,
    SEARCH_CANDIDATE_SERVICE_MISMATCH,
}

sealed interface EntryTrackingMutation {
    data class Status(val value: Long) : EntryTrackingMutation

    data class Progress(val value: Int) : EntryTrackingMutation

    data class Score(val value: String) : EntryTrackingMutation

    data class StartDate(val epochMillis: Long) : EntryTrackingMutation

    data class FinishDate(val epochMillis: Long) : EntryTrackingMutation

    data class Private(val enabled: Boolean) : EntryTrackingMutation
}

sealed interface EntryTrackingAutomaticRegistrationResult {
    data object Registered : EntryTrackingAutomaticRegistrationResult

    data object NoMatch : EntryTrackingAutomaticRegistrationResult

    data class Unavailable(
        val reason: EntryTrackingOperationUnavailableReason,
    ) : EntryTrackingAutomaticRegistrationResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingAutomaticRegistrationResult
}

sealed interface EntryTrackingRemovalResult {
    data class Removed(
        val remoteDeletionFailure: Throwable? = null,
    ) : EntryTrackingRemovalResult

    data class Unavailable(
        val reason: EntryTrackingOperationUnavailableReason,
    ) : EntryTrackingRemovalResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingRemovalResult
}
