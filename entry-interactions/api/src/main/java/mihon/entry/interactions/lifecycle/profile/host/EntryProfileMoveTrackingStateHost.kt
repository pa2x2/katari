package mihon.entry.interactions

fun interface EntryProfileMoveTrackingStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
