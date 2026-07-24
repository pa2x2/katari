package mihon.entry.interactions

fun interface EntryProfileMoveCoverHashStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
