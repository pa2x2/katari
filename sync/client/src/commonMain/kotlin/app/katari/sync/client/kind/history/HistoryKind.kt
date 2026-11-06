package app.katari.sync.client.kind.history

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import java.util.Date

/**
 * History, keyed by its chapter's key. Local ref: `[history.chapter_id]`. Removing a chapter from history deletes the
 * record.
 *
 * The local `time_read` is the device's total: its own share plus every other device's share as last merged. This
 * device's share is that total minus the others, which the stored payload holds.
 */
object HistoryKind : SyncedKind<HistoryPayload>("history", 1, HistoryPayload.serializer(), Absence.TOMBSTONE) {
    override val capture = listOf(
        CaptureTrigger("history", Event.INSERT, listOf("NEW.chapter_id")),
        CaptureTrigger(
            "history",
            Event.UPDATE,
            listOf("NEW.chapter_id"),
            fields = listOf(Field(3, "last_read"), Field(4, "time_read")),
        ),
        CaptureTrigger("history", Event.DELETE, listOf("OLD.chapter_id")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<HistoryPayload> {
        val row = reads.database.sync_local_rowsQueries.historyRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        val chapterKey = reads.chapterKey(row.chapter_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, chapterKey),
            parents = listOf(RecordRef(ChapterKind, ChapterKind.ref(row.chapter_id))),
        ) { previous, stamps ->
            val device = reads.device
            val others = previous?.timeRead.orEmpty().filter { it.device != device }
            val ownMillis = (row.time_read - others.sumOf { it.millis }).coerceAtLeast(0)
            val ownPrevious = previous?.timeRead?.firstOrNull { it.device == device }
            val own = stamps.field(4, ownMillis, ownPrevious?.let { Stamped(it.millis, it.at) })
            HistoryPayload(
                entryKey = entryKey,
                chapterKey = chapterKey,
                lastRead = stamps.field(3, row.last_read?.time, previous?.lastRead),
                timeRead = (others + TimeReadShare(device, own.value, own.at)).sortedBy { it.device.value },
            )
        }
    }

    /** Each device's share is written only by that device, so the share it changed last is its current one. */
    override fun merge(local: HistoryPayload, incoming: HistoryPayload) = local.copy(
        lastRead = latest(local.lastRead, incoming.lastRead),
        timeRead = (local.timeRead + incoming.timeRead)
            .groupBy { it.device }
            .map { (_, shares) ->
                shares.reduce { a, b ->
                    latest(Stamped(a.millis, a.at), Stamped(b.millis, b.at)).let {
                        a.copy(millis = it.value, at = it.at)
                    }
                }
            }
            .sortedBy { it.device.value },
    )

    /** The row holds the sum of every device's share, this device's own included. */
    override suspend fun apply(writes: LocalWrites, key: String, payload: HistoryPayload): ApplyOutcome {
        val chapterId = writes.chapterId(payload.chapterKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        queries.upsertHistory(
            entryId = queries.chapterEntry(chapterId).awaitAsOne(),
            chapterId = chapterId,
            lastRead = payload.lastRead.value?.let(::Date),
            timeRead = payload.timeRead.sumOf { it.millis },
        )
        return ApplyOutcome.Applied(LocalRef.of(chapterId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteHistory(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: HistoryPayload) = payload.entryKey
}
