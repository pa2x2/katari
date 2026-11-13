package app.katari.sync.client.kind.subtitletiming

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * An episode's subtitle timing, keyed by its chapter's key. Local ref: `[episode_subtitle_timings.chapter_id]`. Setting
 * the timing back to none deletes the row, and with it the record.
 */
object SubtitleTimingKind : SyncedKind<SubtitleTimingPayload>(
    "episode.subtitle-timing",
    1,
    SubtitleTimingPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.TITLE_CHOICES

    override val capture = listOf(
        CaptureTrigger("episode_subtitle_timings", Event.INSERT, listOf("NEW.chapter_id")),
        CaptureTrigger(
            "episode_subtitle_timings",
            Event.UPDATE,
            listOf("NEW.chapter_id"),
            fields = listOf(Field(3, "subtitle_key", "delay_ms")),
        ),
        CaptureTrigger(
            "episode_subtitle_timings",
            Event.DELETE,
            listOf("OLD.chapter_id"),
            owners = listOf(Owner.chapter()),
        ),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<SubtitleTimingPayload> {
        val row = reads.database.sync_local_rowsQueries.subtitleTimingRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        val chapterKey = reads.chapterKey(row.chapter_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = RecordKeys.derive(name, chapterKey),
            parents = listOf(RecordRef(ChapterKind, ChapterKind.ref(row.chapter_id))),
        ) { previous, stamps ->
            SubtitleTimingPayload(
                entryKey = entryKey,
                chapterKey = chapterKey,
                timing = stamps.field(3, SubtitleDelay(row.subtitle_key, row.delay_ms), previous?.timing),
            )
        }
    }

    override fun merge(local: SubtitleTimingPayload, incoming: SubtitleTimingPayload) = local.copy(
        timing = latest(local.timing, incoming.timing),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: SubtitleTimingPayload): ApplyOutcome {
        val chapterId = writes.chapterId(payload.chapterKey).idOr { return it }
        writes.database.sync_applyQueries.upsertSubtitleTiming(
            chapterId = chapterId,
            subtitleKey = payload.timing.value.subtitleKey,
            delayMs = payload.timing.value.delayMs,
        )
        return ApplyOutcome.Applied(LocalRef.of(chapterId))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteSubtitleTiming(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: SubtitleTimingPayload) = payload.entryKey
}
