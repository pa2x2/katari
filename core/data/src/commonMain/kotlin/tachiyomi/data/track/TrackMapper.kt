package tachiyomi.data.track

import tachiyomi.domain.track.model.EntryTrack

object TrackMapper {
    fun mapTrack(
        id: Long,
        @Suppress("UNUSED_PARAMETER")
        profileId: Long,
        entryId: Long,
        syncId: Long,
        remoteId: Long,
        libraryId: Long?,
        title: String,
        lastChapterRead: Double,
        totalChapters: Long,
        status: Long,
        score: Double,
        remoteUrl: String,
        startDate: Long,
        finishDate: Long,
        private: Boolean,
    ): EntryTrack = EntryTrack(
        id = id,
        entryId = entryId,
        trackerId = syncId,
        remoteId = remoteId,
        libraryId = libraryId,
        title = title,
        progress = lastChapterRead,
        total = totalChapters,
        status = status,
        score = score,
        remoteUrl = remoteUrl,
        startDate = startDate,
        finishDate = finishDate,
        private = private,
    )
}
