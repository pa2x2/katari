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
        .filter { it.entryId == entryId }
        .map { it.trackerId to it.remoteId }
        .toSet()
    if (keys.isEmpty()) return emptySet()
    return tracks.asSequence()
        .filter { it.entryId != entryId && it.entryId in libraryMemberIds }
        .filter { (it.trackerId to it.remoteId) in keys }
        .mapTo(linkedSetOf()) { it.entryId }
}
