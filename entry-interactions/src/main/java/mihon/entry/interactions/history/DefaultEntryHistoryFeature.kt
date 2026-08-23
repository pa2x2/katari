package mihon.entry.interactions.history

import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import tachiyomi.domain.history.model.activity.HistoryActivityUpdate
import tachiyomi.domain.history.model.activity.HistoryCompletionCause
import tachiyomi.domain.history.model.activity.HistoryCompletionUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal class DefaultEntryHistoryFeature(
    private val repository: HistoryRepository,
) : EntryHistoryFeature {
    override suspend fun record(event: EntryMediaSessionEvent, activity: EntryMediaSessionActivity) {
        if (activity.durationMillis <= 0L) return
        repository.recordActivity(
            HistoryActivityUpdate(
                entryId = event.visibleEntry.id,
                chapterId = event.child.id,
                sessionId = activity.sessionId,
                sequence = activity.sequence,
                startedAtEpochMillis = activity.startedAtEpochMillis,
                endedAtEpochMillis = activity.recordedAtEpochMillis,
                durationMillis = activity.durationMillis,
                timeZoneId = activity.timeZoneId,
            ),
        )
    }

    override suspend fun recordCompletion(
        event: EntryMediaSessionEvent.Progressed,
        progress: tachiyomi.domain.entry.model.EntryProgressState,
    ) {
        val occurredAt = progress.completionUpdatedAt.takeIf { it > 0L } ?: progress.locatorUpdatedAt
        val timeZone = ZoneId.systemDefault()
        val identity = listOf(
            event.visibleEntry.id,
            progress.contentKey,
            progress.resourceKey,
            occurredAt,
        ).joinToString(separator = "\u001f")
        repository.recordCompletion(
            HistoryCompletionUpdate(
                eventId = UUID.nameUUIDFromBytes(identity.toByteArray(StandardCharsets.UTF_8)).toString(),
                entryId = event.visibleEntry.id,
                chapterId = event.child.id,
                sessionId = event.activity?.sessionId,
                occurredAtEpochMillis = occurredAt,
                localDate = Instant.ofEpochMilli(occurredAt).atZone(timeZone).toLocalDate().toString(),
                timeZoneId = timeZone.id,
                cause = HistoryCompletionCause.CONSUMPTION,
            ),
        )
    }
}
