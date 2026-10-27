package mihon.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveScanlatorFilterStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
