package mihon.feature.library.update.planning

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.entry.service.FetchInterval
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository
import kotlin.time.Instant

data class LibraryUpdatePlanningContext(
    val now: Long,
    /** End of the window in which a predicted release counts as expected now. */
    val fetchWindowUpperBound: Long,
    /** When each entry was last checked by a library update. */
    val lastCheckedAt: Map<Long, Long>,
) {
    /** Reads the context of an update starting at a given time, for screens that show what it would do. */
    class Reader(
        private val reportRepository: LibraryUpdateReportRepository,
        private val fetchInterval: FetchInterval,
    ) {
        suspend fun read(now: Instant): LibraryUpdatePlanningContext {
            val timeZone = TimeZone.currentSystemDefault()
            return LibraryUpdatePlanningContext(
                now = now.toEpochMilliseconds(),
                fetchWindowUpperBound = fetchInterval.getWindow(now.toLocalDateTime(timeZone).date, timeZone).second,
                lastCheckedAt = reportRepository.getStatuses().values
                    .mapNotNull { status -> status.lastCheckedAt?.let { status.entryId to it } }
                    .toMap(),
            )
        }
    }
}
