package mihon.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveCoverHashStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
