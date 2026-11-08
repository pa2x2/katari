package tachiyomi.data.entry

import tachiyomi.domain.entry.model.EntrySync

object EntrySyncMapper {
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
    ): EntrySync = EntrySync(
        id = id,
        entryId = entryId,
        syncId = syncId,
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
