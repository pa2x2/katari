package mihon.entry.interactions.lifecycle.profile.host

data class EntryProfileMoveStateRequest(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val entryIds: List<Long>,
)
