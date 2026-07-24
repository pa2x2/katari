package mihon.entry.interactions

sealed interface EntryTrackingAvailability {
    data class Available(
        val services: List<EntryTrackingServiceDescriptor>,
    ) : EntryTrackingAvailability

    data object Unsupported : EntryTrackingAvailability
}

sealed interface EntryTrackingSession {
    data class Available(
        val services: List<EntryTrackingSessionService>,
    ) : EntryTrackingSession

    data class Unavailable(
        val reasons: Set<EntryTrackingSessionUnavailableReason>,
    ) : EntryTrackingSession
}

data class EntryTrackingSessionService(
    val service: EntryTrackingServiceDescriptor,
    val track: EntryTrackingRecord?,
    val displayScore: String?,
)

enum class EntryTrackingSessionUnavailableReason {
    UNSUPPORTED_ENTRY_TYPE,
    NOT_LOGGED_IN,
    SOURCE_NOT_ACCEPTED,
}
