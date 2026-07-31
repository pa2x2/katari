package mihon.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveChildGroupFilterStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
