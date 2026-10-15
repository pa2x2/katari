package mihon.entry.interactions.source

/** Whether a source refresh replaces library entries' titles with the source's, per profile. */
fun interface EntryLibraryTitleUpdatePolicy {
    fun updatesTitles(profileId: Long): Boolean
}
