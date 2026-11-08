package app.katari.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveScanlatorFilterStateHost {
    suspend fun move(request: EntryProfileMoveStateRequest)
}
