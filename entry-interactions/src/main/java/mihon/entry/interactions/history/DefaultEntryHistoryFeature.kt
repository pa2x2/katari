package mihon.entry.interactions

import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import java.util.Date

internal class DefaultEntryHistoryFeature(
    private val repository: HistoryRepository,
) : EntryHistoryFeature {
    override suspend fun record(event: EntryMediaSessionEvent, activity: EntryMediaSessionActivity) {
        if (activity.durationMillis <= 0L) return
        repository.upsertHistory(
            HistoryUpdate(
                chapterId = event.child.id,
                readAt = Date(activity.recordedAtEpochMillis),
                sessionReadDuration = activity.durationMillis,
            ),
        )
    }
}
