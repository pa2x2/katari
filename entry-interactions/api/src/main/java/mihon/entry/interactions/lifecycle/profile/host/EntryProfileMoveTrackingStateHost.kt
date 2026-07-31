package mihon.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveTrackingStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
