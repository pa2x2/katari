package tachiyomi.data.entry

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.Episode_subtitle_timings
import tachiyomi.domain.entry.model.SubtitleTiming
import tachiyomi.domain.entry.repository.SubtitleTimingRepository

@Inject
@SingleIn(AppScope::class)
class SubtitleTimingRepositoryImpl(
    private val handler: DatabaseHandler,
) : SubtitleTimingRepository {

    override suspend fun get(chapterId: Long): SubtitleTiming? =
        handler.awaitOneOrNull { episode_subtitle_timingsQueries.get(chapterId) }?.toTiming()

    override suspend fun getByEntryId(entryId: Long): List<SubtitleTiming> =
        handler.awaitList { episode_subtitle_timingsQueries.getByEntryId(entryId) }.map { it.toTiming() }

    override suspend fun set(timing: SubtitleTiming) {
        require(timing.delayMs != 0L) { "A timing of 0 is no timing; delete it instead" }
        handler.await {
            episode_subtitle_timingsQueries.upsert(
                chapterId = timing.chapterId,
                subtitleKey = timing.subtitleKey,
                delayMs = timing.delayMs,
            )
        }
    }

    override suspend fun delete(chapterId: Long) {
        handler.await { episode_subtitle_timingsQueries.delete(chapterId) }
    }

    private fun Episode_subtitle_timings.toTiming() = SubtitleTiming(
        chapterId = chapter_id,
        subtitleKey = subtitle_key,
        delayMs = delay_ms,
    )
}
