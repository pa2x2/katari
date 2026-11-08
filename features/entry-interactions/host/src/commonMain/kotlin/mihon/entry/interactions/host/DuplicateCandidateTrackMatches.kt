package mihon.entry.interactions.host

internal data class DuplicateCandidateTrackKey(
    val entryId: Long,
    val trackerId: Long,
    val remoteId: Long,
)

internal fun duplicateCandidateTrackEntryIds(
    entryId: Long,
    libraryMemberIds: Set<Long>,
    tracks: List<DuplicateCandidateTrackKey>,
): Set<Long> {
    val keys = tracks.asSequence()
        // Trackers that don't set a remote id leave it at 0, which says nothing about which title is tracked.
        .filter { it.entryId == entryId && it.remoteId != 0L }
        .map { it.trackerId to it.remoteId }
        .toSet()
    if (keys.isEmpty()) return emptySet()
    return tracks.asSequence()
        .filter { it.entryId != entryId && it.entryId in libraryMemberIds }
        .filter { (it.trackerId to it.remoteId) in keys }
        .mapTo(linkedSetOf()) { it.entryId }
}
