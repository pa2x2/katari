package mihon.entry.interactions

fun interface EntryProfileMoveChildGroupFilterStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
