package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch

data class EntryTrackSearch(
    val trackerId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val progress: Double,
    val total: Long,
    val status: Long,
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
)

fun TrackSearch.toEntryTrackSearch(): EntryTrackSearch =
    EntryTrackSearch(
        trackerId = tracker_id,
        remoteId = remote_id,
        libraryId = library_id,
        title = title,
        progress = last_chapter_read,
        total = total_chapters,
        status = status,
        score = score,
        remoteUrl = tracking_url,
        startDate = started_reading_date,
        finishDate = finished_reading_date,
        private = private,
    )
