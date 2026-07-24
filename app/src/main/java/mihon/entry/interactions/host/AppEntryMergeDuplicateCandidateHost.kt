package mihon.entry.interactions.host

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.data.track.TrackMapper
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.DuplicateConfig
import tachiyomi.domain.entry.service.DuplicateEntryMetadata
import tachiyomi.domain.entry.service.DuplicateLibraryCandidate
import tachiyomi.domain.entry.service.DuplicateLibrarySupport
import tachiyomi.domain.entry.service.toDuplicateConfig
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.track.model.EntryTrack

internal class AppEntryMergeDuplicateCandidateHost(
    private val handler: DatabaseHandler,
    private val preferences: DuplicatePreferences,
) {
    suspend fun candidates(
        profileId: Long,
        entry: Entry,
        memberships: List<EntryMergeMembershipSnapshot>,
    ): List<DuplicateEntryCandidate> {
        return detect(
            entry = entry,
            libraryEntries = libraryEntries(profileId),
            memberships = memberships,
            tracks = tracks(profileId),
            config = preferences.toDuplicateConfig(),
        )
    }

    fun observeCandidates(
        profileId: Long,
        entry: Flow<Entry>,
        memberships: Flow<List<EntryMergeMembershipSnapshot>>,
    ): Flow<List<DuplicateEntryCandidate>> {
        val libraryEntries = handler.subscribeToList {
            libraryViewQueries.library(profileId, EntryMapper::mapLibraryEntry)
        }
        val tracks = handler.subscribeToList {
            entry_syncQueries.getTracks(profileId, TrackMapper::mapTrack)
        }
        return combine(
            entry,
            libraryEntries,
            memberships,
            tracks,
            duplicateConfig(),
        ) { current, library, currentMemberships, currentTracks, config ->
            detect(current, library, currentMemberships, currentTracks, config)
        }
    }

    private fun duplicateConfig(): Flow<DuplicateConfig> {
        return combine(
            preferences.extendedDuplicateDetectionEnabled.changes(),
            preferences.minimumMatchScore.changes(),
            combine(
                preferences.descriptionWeight.changes(),
                preferences.authorWeight.changes(),
                preferences.artistWeight.changes(),
                preferences.coverWeight.changes(),
            ) { _, _, _, _ -> Unit },
            combine(
                preferences.genreWeight.changes(),
                preferences.statusWeight.changes(),
                preferences.chapterCountWeight.changes(),
                preferences.titleWeight.changes(),
            ) { _, _, _, _ -> Unit },
            preferences.titleExclusionPatterns.changes(),
        ) { _, _, _, _, _ -> preferences.toDuplicateConfig() }
    }

    private suspend fun libraryEntries(profileId: Long): List<Entry> {
        return handler.awaitList { libraryViewQueries.library(profileId, EntryMapper::mapLibraryEntry) }
    }

    private suspend fun tracks(profileId: Long): List<EntryTrack> {
        return handler.awaitList { entry_syncQueries.getTracks(profileId, TrackMapper::mapTrack) }
    }

    private suspend fun detect(
        entry: Entry,
        libraryEntries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
        tracks: List<EntryTrack>,
        config: DuplicateConfig,
    ): List<DuplicateEntryCandidate> {
        require(libraryEntries.all { it.profileId == entry.profileId }) {
            "Merge candidate data cannot cross profiles"
        }
        val sameTypeLibraryEntries = libraryEntries.filter { it.type == entry.type }
        val entryIds = (sameTypeLibraryEntries.map(Entry::id) + entry.id).distinct()
        val counts = if (entryIds.isEmpty()) {
            emptyMap()
        } else {
            handler.awaitList {
                chaptersQueries.getCountsByEntryIds(entryIds) { entryId, count -> entryId to count }
            }.toMap()
        }
        return withContext(Dispatchers.Default) {
            val membershipByEntry = memberships
                .flatMap { membership -> membership.orderedEntryIds.map { it to membership } }
                .toMap()
            val current = entry.toMetadata(counts[entry.id])
            val excludedIds = membershipByEntry[entry.id]?.orderedEntryIds?.toSet() ?: setOf(entry.id)
            val libraryMemberIds = sameTypeLibraryEntries.mapTo(mutableSetOf(), Entry::id)
            val trackerDuplicates = trackerDuplicateIds(entry.id, libraryMemberIds, tracks)
            val candidates = buildCandidates(sameTypeLibraryEntries, memberships, counts)
            DuplicateLibrarySupport.detectDuplicates(
                currentEntry = current,
                libraryEntries = candidates,
                excludedIds = excludedIds,
                trackerDuplicateIds = trackerDuplicates,
                config = config,
            ).map { match ->
                DuplicateEntryCandidate(
                    entry = match.item,
                    count = match.count,
                    cheapScore = match.cheapScore,
                    scoreMax = match.scoreMax,
                    score = match.score,
                    reasons = match.reasons,
                    contentSignature = match.contentSignature,
                )
            }
        }
    }

    private fun buildCandidates(
        libraryEntries: List<Entry>,
        memberships: List<EntryMergeMembershipSnapshot>,
        counts: Map<Long, Long>,
    ): List<DuplicateLibraryCandidate<Entry>> {
        val entriesById = libraryEntries.associateBy(Entry::id)
        val membershipByEntry = memberships
            .flatMap { membership -> membership.orderedEntryIds.map { it to membership } }
            .toMap()
        val consumed = mutableSetOf<Long>()
        return libraryEntries.mapNotNull { entry ->
            if (!consumed.add(entry.id)) return@mapNotNull null
            val membership = membershipByEntry[entry.id]
            val members = membership?.orderedEntryIds.orEmpty().mapNotNull(entriesById::get)
            if (members.size > 1) {
                consumed += members.map(Entry::id)
                val target = entriesById[membership!!.targetEntryId] ?: members.first()
                val total = members.sumOf { counts[it.id] ?: 0L }
                DuplicateLibraryCandidate(
                    item = target,
                    sortTitle = target.displayTitle,
                    memberIds = members.map(Entry::id),
                    memberEntries = members.map { it.toMetadata(total) },
                    count = total,
                    contentSignature = target.lastModifiedAt,
                )
            } else {
                val count = counts[entry.id] ?: 0L
                DuplicateLibraryCandidate(
                    item = entry,
                    sortTitle = entry.displayTitle,
                    memberIds = listOf(entry.id),
                    memberEntries = listOf(entry.toMetadata(count)),
                    count = count,
                    contentSignature = entry.lastModifiedAt,
                )
            }
        }
    }

    private fun trackerDuplicateIds(
        entryId: Long,
        libraryMemberIds: Set<Long>,
        tracks: List<EntryTrack>,
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

    private fun Entry.toMetadata(count: Long?): DuplicateEntryMetadata {
        return DuplicateEntryMetadata(
            id = id,
            title = title,
            description = description,
            primaryCreator = author,
            secondaryCreator = artist,
            genres = genre,
            status = status.value.toLong(),
            count = count,
        )
    }
}
