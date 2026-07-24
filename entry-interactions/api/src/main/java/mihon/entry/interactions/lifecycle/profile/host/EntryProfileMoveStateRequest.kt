package mihon.entry.interactions

data class EntryProfileMoveStateRequest(
    val sourceProfileId: Long,
    val destinationProfileId: Long,
    val entryIds: List<Long>,
)
