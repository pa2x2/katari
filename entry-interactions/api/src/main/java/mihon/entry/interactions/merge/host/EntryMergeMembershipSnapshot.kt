package mihon.entry.interactions.host

/** Authoritative profile-scoped membership visible only across the Merge coordinator/host boundary. */
data class EntryMergeMembershipSnapshot(
    val profileId: Long,
    val targetEntryId: Long,
    val orderedEntryIds: List<Long>,
) {
    init {
        require(orderedEntryIds.size >= 2) { "A Merge membership snapshot requires at least two Entries" }
        require(orderedEntryIds.distinct().size == orderedEntryIds.size) {
            "A Merge membership snapshot cannot contain duplicate Entries"
        }
        require(targetEntryId in orderedEntryIds) { "The Merge target must be present in its membership snapshot" }
    }
}
