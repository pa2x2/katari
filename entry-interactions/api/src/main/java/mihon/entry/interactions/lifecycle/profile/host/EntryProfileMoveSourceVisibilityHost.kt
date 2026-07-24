package mihon.entry.interactions

fun interface EntryProfileMoveSourceVisibilityHost {
    fun makeSourcesVisible(profileId: Long, sourceIds: Set<Long>)
}
