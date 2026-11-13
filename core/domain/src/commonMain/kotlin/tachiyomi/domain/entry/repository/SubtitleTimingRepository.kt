package tachiyomi.domain.entry.repository

import tachiyomi.domain.entry.model.SubtitleTiming

interface SubtitleTimingRepository {

    suspend fun get(chapterId: Long): SubtitleTiming?

    suspend fun getByEntryId(entryId: Long): List<SubtitleTiming>

    suspend fun set(timing: SubtitleTiming)

    suspend fun delete(chapterId: Long)
}
