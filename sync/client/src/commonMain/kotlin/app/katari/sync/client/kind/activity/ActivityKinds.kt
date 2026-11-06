package app.katari.sync.client.kind.activity

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/** Reading and watching sessions, keyed by their random session id. Local ref: `[session_id]`. */
object ActivitySessionKind : SyncedKind<ActivitySessionPayload>(
    "activity.session",
    1,
    ActivitySessionPayload.serializer(),
    Absence.KEEP,
) {
    override val capture = listOf(
        CaptureTrigger("activity_sessions", Event.INSERT, listOf("NEW.session_id")),
        CaptureTrigger(
            "activity_sessions",
            Event.UPDATE,
            listOf("NEW.session_id"),
            fields = listOf(Field(3, "started_at"), Field(4, "ended_at"), Field(5, "duration")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ActivitySessionPayload> {
        val row = reads.database.sync_local_rowsQueries.activitySessionRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, row.session_id),
            parents = listOf(RecordRef(EntryKind, EntryKind.ref(row.entry_id))),
        ) { _, _ ->
            ActivitySessionPayload(row.session_id, entryKey, row.started_at, row.ended_at, row.duration)
        }
    }

    /** A session only grows while it lasts, so the copy that ends later holds all of the other. */
    override fun merge(local: ActivitySessionPayload, incoming: ActivitySessionPayload) =
        maxOf(local, incoming, compareBy<ActivitySessionPayload>({ it.endedAt }, { it.duration }, { it.startedAt }))
}

/**
 * Session segments, keyed by session id, chapter key, local date and time zone. Local ref:
 * `[session_id, chapter_id, local_date, time_zone_id]`, with `chapter_id` null for time spent outside a chapter.
 */
object ActivitySegmentKind : SyncedKind<ActivitySegmentPayload>(
    "activity.segment",
    1,
    ActivitySegmentPayload.serializer(),
    Absence.KEEP,
) {
    private val ref = listOf("NEW.session_id", "NEW.chapter_id", "NEW.local_date", "NEW.time_zone_id")

    override val capture = listOf(
        CaptureTrigger("activity_segments", Event.INSERT, ref),
        CaptureTrigger(
            "activity_segments",
            Event.UPDATE,
            ref,
            fields = listOf(Field(5, "started_at"), Field(6, "ended_at"), Field(7, "duration")),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ActivitySegmentPayload> {
        val row = reads.database.sync_local_rowsQueries
            .activitySegmentRow(ref.text(0), ref.longOrNull(1), ref.text(2), ref.text(3))
            .awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val session = reads.database.sync_local_rowsQueries.activitySessionRow(row.session_id).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        if (reads.entryKey(session.entry_id) == null) return LocalRead.NotSynced
        val chapterKey = row.chapter_id?.let { reads.chapterKey(it) }
        return LocalRead.Found(
            key = RecordKeys.derive(name, row.session_id, chapterKey.orEmpty(), row.local_date, row.time_zone_id),
            parents = listOfNotNull(
                RecordRef(ActivitySessionKind, LocalRef.of(row.session_id)),
                row.chapter_id?.let { RecordRef(ChapterKind, ChapterKind.ref(it)) },
            ),
        ) { _, _ ->
            ActivitySegmentPayload(
                sessionId = row.session_id,
                chapterKey = chapterKey,
                localDate = row.local_date,
                timeZoneId = row.time_zone_id,
                startedAt = row.started_at,
                endedAt = row.ended_at,
                duration = row.duration,
            )
        }
    }

    override fun merge(local: ActivitySegmentPayload, incoming: ActivitySegmentPayload) =
        maxOf(local, incoming, compareBy<ActivitySegmentPayload>({ it.endedAt }, { it.duration }, { it.startedAt }))
}

/**
 * Completion events. The local event id is built from local row ids, which differ per device, so the key is built
 * again from the entry and chapter keys, the time and the cause. Local ref: `[event_id]`.
 */
object ActivityCompletionKind : SyncedKind<ActivityCompletionPayload>(
    "activity.completion",
    1,
    ActivityCompletionPayload.serializer(),
    Absence.KEEP,
) {
    override val capture = listOf(CaptureTrigger("activity_completion_events", Event.INSERT, listOf("NEW.event_id")))

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ActivityCompletionPayload> {
        val row = reads.database.sync_local_rowsQueries.activityCompletionRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        val chapterKey = row.chapter_id?.let { reads.chapterKey(it) }
        return LocalRead.Found(
            key = RecordKeys.derive(name, entryKey, chapterKey.orEmpty(), row.occurred_at.toString(), row.cause),
            parents = listOfNotNull(
                RecordRef(EntryKind, EntryKind.ref(row.entry_id)),
                row.chapter_id?.let { RecordRef(ChapterKind, ChapterKind.ref(it)) },
                row.session_id?.let { RecordRef(ActivitySessionKind, LocalRef.of(it)) },
            ),
        ) { _, _ ->
            ActivityCompletionPayload(
                entryKey = entryKey,
                chapterKey = chapterKey,
                sessionId = row.session_id,
                occurredAt = row.occurred_at,
                localDate = row.local_date,
                timeZoneId = row.time_zone_id,
                cause = row.cause,
            )
        }
    }

    /** An event never changes once it happened. */
    override fun merge(local: ActivityCompletionPayload, incoming: ActivityCompletionPayload) = local
}
