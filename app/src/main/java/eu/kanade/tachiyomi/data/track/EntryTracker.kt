package eu.kanade.tachiyomi.data.track

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack

interface EntryTracker : TrackerCapabilities {
    val id: Long
    val name: String

    suspend fun search(query: String, entryType: EntryType): List<EntryTrackSearch>

    suspend fun bind(entry: Entry, remote: EntryTrackSearch): EntryTrack

    suspend fun update(track: EntryTrack): EntryTrack
}

class LegacyEntryTrackerAdapter(
    private val tracker: Tracker,
) : EntryTracker {

    override val id: Long
        get() = tracker.id

    override val name: String
        get() = tracker.name

    override val supportedEntryTypes: Set<EntryType>
        get() = tracker.supportedEntryTypes

    override suspend fun search(query: String, entryType: EntryType): List<EntryTrackSearch> {
        if (entryType !in supportedEntryTypes) return emptyList()
        return tracker.search(query).map { it.toEntryTrackSearch() }
    }

    override suspend fun bind(entry: Entry, remote: EntryTrackSearch): EntryTrack {
        require(entry.type in supportedEntryTypes) {
            "Tracker $name does not support ${entry.type} entries"
        }
        val dbTrack = remote.toDbTrack(entry.id)
        return tracker.bind(dbTrack).toDomainTrack(idRequired = false)!!
    }

    override suspend fun update(track: EntryTrack): EntryTrack {
        return tracker.update(track.toDbTrack()).toDomainTrack(idRequired = false)!!
    }
}

private fun EntryTrackSearch.toDbTrack(entryId: Long): eu.kanade.tachiyomi.data.database.models.Track =
    eu.kanade.tachiyomi.data.database.models.Track.create(trackerId).also {
        it.manga_id = entryId
        it.remote_id = remoteId
        it.library_id = libraryId
        it.title = title
        it.progress = progress
        it.total = total
        it.status = status
        it.score = score
        it.tracking_url = remoteUrl
        it.started_reading_date = startDate
        it.finished_reading_date = finishDate
        it.private = private
    }
