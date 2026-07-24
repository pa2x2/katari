package mihon.entry.interactions

sealed interface EntryTrackingRefreshResult {
    data class Completed(
        val failures: List<EntryTrackingRefreshFailure>,
    ) : EntryTrackingRefreshResult

    data class Unavailable(
        val reason: EntryTrackingOperationUnavailableReason,
    ) : EntryTrackingRefreshResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingRefreshResult
}

data class EntryTrackingRefreshFailure(
    val serviceId: EntryTrackingServiceId,
    val serviceName: String,
    val cause: Throwable,
)
