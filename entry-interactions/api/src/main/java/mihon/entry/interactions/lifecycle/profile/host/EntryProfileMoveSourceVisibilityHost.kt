package mihon.entry.interactions.lifecycle.profile.host

fun interface EntryProfileMoveSourceVisibilityHost {
    fun makeSourcesVisible(profileId: Long, sourceIds: Set<Long>)
}
