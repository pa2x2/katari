package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryTrackingAutomation {
    suspend fun bindAutomatically(entry: Entry): EntryTrackingAutomaticBindingResult

    suspend fun inspectProgressSynchronization(
        entry: Entry,
        progress: Double,
    ): EntryTrackingProgressInspection

    suspend fun synchronizeProgress(
        entry: Entry,
        progress: Double,
        scheduleRetry: Boolean = true,
    ): EntryTrackingProgressSynchronizationResult

    suspend fun prepareMigrationTracks(
        source: Entry,
        target: Entry,
        tracks: List<EntryTrackingRecord>,
    ): EntryTrackingMigrationPreparationResult
}

sealed interface EntryTrackingAutomaticBindingResult {
    data class Completed(
        val boundServices: Set<EntryTrackingServiceId>,
        val unmatchedServices: Set<EntryTrackingServiceId>,
        val failures: List<EntryTrackingServiceFailure>,
    ) : EntryTrackingAutomaticBindingResult

    data class Unavailable(
        val reason: EntryTrackingAutomationUnavailableReason,
    ) : EntryTrackingAutomaticBindingResult
}

sealed interface EntryTrackingProgressInspection {
    data object UpdateRequired : EntryTrackingProgressInspection

    data object Current : EntryTrackingProgressInspection

    data class Unavailable(
        val reason: EntryTrackingAutomationUnavailableReason,
    ) : EntryTrackingProgressInspection
}

sealed interface EntryTrackingProgressSynchronizationResult {
    data class Completed(
        val failures: List<EntryTrackingServiceFailure>,
    ) : EntryTrackingProgressSynchronizationResult

    data class Unavailable(
        val reason: EntryTrackingAutomationUnavailableReason,
    ) : EntryTrackingProgressSynchronizationResult
}

sealed interface EntryTrackingMigrationPreparationResult {
    data class Prepared(
        val tracks: List<EntryTrackingRecord>,
    ) : EntryTrackingMigrationPreparationResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingMigrationPreparationResult
}

data class EntryTrackingServiceFailure(
    val serviceId: EntryTrackingServiceId,
    val serviceName: String,
    val cause: Throwable,
)

enum class EntryTrackingAutomationUnavailableReason {
    UNSUPPORTED_ENTRY_TYPE,
    NOT_LOGGED_IN,
    SOURCE_NOT_ACCEPTED,
    NO_AUTOMATIC_SERVICE,
    NO_TRACKED_SERVICE,
}
