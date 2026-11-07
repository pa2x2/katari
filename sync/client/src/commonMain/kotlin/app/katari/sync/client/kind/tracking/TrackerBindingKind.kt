package app.katari.sync.client.kind.tracking

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Tracker bindings, keyed by entry key and tracker id. Local ref: `[entry_id, sync_id]`. Rebinding replaces the row
 * (`ON CONFLICT REPLACE`), which SQLite reports as an insert alone.
 */
object TrackerBindingKind : SyncedKind<TrackerBindingPayload>(
    "tracker.binding",
    1,
    TrackerBindingPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val capture = listOf(
        CaptureTrigger("entry_sync", Event.INSERT, listOf("NEW.entry_id", "NEW.sync_id")),
        CaptureTrigger(
            "entry_sync",
            Event.UPDATE,
            listOf("NEW.entry_id", "NEW.sync_id"),
            fields = listOf(
                Field(3, "remote_id"),
                Field(4, "library_id"),
                Field(5, "remote_url"),
                Field(6, "title"),
                Field(7, "last_chapter_read"),
                Field(8, "total_chapters"),
                Field(9, "status"),
                Field(10, "score"),
                Field(11, "start_date"),
                Field(12, "finish_date"),
                Field(13, "private"),
            ),
        ),
        CaptureTrigger(
            "entry_sync",
            Event.DELETE,
            listOf("OLD.entry_id", "OLD.sync_id"),
            owners = listOf(Owner.entry()),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<TrackerBindingPayload> {
        val row = reads.database.sync_local_rowsQueries.trackerBindingRow(ref.long(0), ref.long(1)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, entryKey, row.sync_id.toString()),
            parents = listOf(RecordRef(EntryKind, EntryKind.ref(row.entry_id))),
        ) { previous, stamps ->
            TrackerBindingPayload(
                entryKey = entryKey,
                trackerId = row.sync_id,
                remoteId = stamps.field(3, row.remote_id, previous?.remoteId),
                libraryId = stamps.field(4, row.library_id, previous?.libraryId),
                remoteUrl = stamps.field(5, row.remote_url, previous?.remoteUrl),
                title = stamps.field(6, row.title, previous?.title),
                lastChapterRead = stamps.field(7, row.last_chapter_read, previous?.lastChapterRead),
                totalChapters = stamps.field(8, row.total_chapters, previous?.totalChapters),
                status = stamps.field(9, row.status, previous?.status),
                score = stamps.field(10, row.score, previous?.score),
                startDate = stamps.field(11, row.start_date, previous?.startDate),
                finishDate = stamps.field(12, row.finish_date, previous?.finishDate),
                private = stamps.field(13, row.private_, previous?.private),
            )
        }
    }
    override fun merge(local: TrackerBindingPayload, incoming: TrackerBindingPayload) = local.copy(
        remoteId = latest(local.remoteId, incoming.remoteId),
        libraryId = latest(local.libraryId, incoming.libraryId),
        remoteUrl = latest(local.remoteUrl, incoming.remoteUrl),
        title = latest(local.title, incoming.title),
        lastChapterRead = latest(local.lastChapterRead, incoming.lastChapterRead),
        totalChapters = latest(local.totalChapters, incoming.totalChapters),
        status = latest(local.status, incoming.status),
        score = latest(local.score, incoming.score),
        startDate = latest(local.startDate, incoming.startDate),
        finishDate = latest(local.finishDate, incoming.finishDate),
        private = latest(local.private, incoming.private),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: TrackerBindingPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        queries.upsertTrackerBinding(
            profileId = queries.entryProfile(entryId).awaitAsOne(),
            entryId = entryId,
            trackerId = payload.trackerId,
            remoteId = payload.remoteId.value,
            libraryId = payload.libraryId.value,
            title = payload.title.value,
            lastChapterRead = payload.lastChapterRead.value,
            totalChapters = payload.totalChapters.value,
            status = payload.status.value,
            score = payload.score.value,
            remoteUrl = payload.remoteUrl.value,
            startDate = payload.startDate.value,
            finishDate = payload.finishDate.value,
            private = payload.private.value,
        )
        return ApplyOutcome.Applied(LocalRef.of(entryId, payload.trackerId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteTrackerBinding(it.long(0), it.long(1)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: TrackerBindingPayload) = payload.entryKey
}
