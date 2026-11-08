package eu.kanade.domain.track.model

import tachiyomi.domain.track.model.EntryTrack
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

fun EntryTrack.copyPersonalFrom(other: EntryTrack): EntryTrack {
    return this.copy(
        progress = other.progress,
        score = other.score,
        status = other.status,
        startDate = other.startDate,
        finishDate = other.finishDate,
        private = other.private,
    )
}

fun EntryTrack.toDbTrack(): DbTrack = DbTrack.create(trackerId).also {
    it.id = id
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

fun DbTrack.toDomainTrack(idRequired: Boolean = true): EntryTrack? {
    val trackId = id ?: if (!idRequired) -1 else return null
    return EntryTrack(
        id = trackId,
        entryId = manga_id,
        trackerId = tracker_id,
        remoteId = remote_id,
        libraryId = library_id,
        title = title,
        progress = this.progress,
        total = this.total,
        status = status,
        score = score,
        remoteUrl = tracking_url,
        startDate = started_reading_date,
        finishDate = finished_reading_date,
        private = private,
    )
}
