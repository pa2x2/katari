package app.katari.sync.client.kind.progress

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
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Reading and playback positions, keyed by entry key, content key and resource key. Local ref:
 * `[entry_progress_state._id]`.
 */
object ProgressKind : SyncedKind<ProgressPayload>("progress", 1, ProgressPayload.serializer(), Absence.TOMBSTONE) {
    private val locatorColumns = arrayOf(
        "locator_kind",
        "position",
        "extent",
        "progression",
        "total_progression",
        "extensions",
        "resource_revision",
    )

    override val capture = listOf(
        CaptureTrigger("entry_progress_state", Event.INSERT, listOf("NEW._id")),
        CaptureTrigger(
            "entry_progress_state",
            Event.UPDATE,
            listOf("NEW._id"),
            fields = listOf(Field(5, *locatorColumns), Field(6, "completed")),
        ),
        CaptureTrigger("entry_progress_state", Event.DELETE, listOf("OLD._id")),
    )

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ProgressPayload> {
        val row = reads.database.sync_local_rowsQueries.progressRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        val chapterKey = row.chapter_id?.let { reads.chapterKey(it) }
        return LocalRead.Found(
            key = RecordKeys.derive(name, entryKey, row.content_key, row.resource_key),
            parents = listOfNotNull(
                RecordRef(EntryKind, EntryKind.ref(row.entry_id)),
                row.chapter_id?.let { RecordRef(ChapterKind, ChapterKind.ref(it)) },
            ),
        ) { previous, stamps ->
            ProgressPayload(
                entryKey = entryKey,
                contentKey = row.content_key,
                resourceKey = row.resource_key,
                chapterKey = chapterKey,
                locator = stamps.fieldEditedAt(
                    5,
                    ProgressLocator(
                        kind = row.locator_kind,
                        position = row.position,
                        extent = row.extent,
                        progression = row.progression,
                        totalProgression = row.total_progression,
                        extensions = row.extensions,
                        resourceRevision = row.resource_revision,
                    ),
                    previous?.locator,
                    row.locator_updated_at,
                    reads.device,
                ),
                completed = stamps.fieldEditedAt(
                    6,
                    row.completed,
                    previous?.completed,
                    row.completion_updated_at,
                    reads.device,
                ),
            )
        }
    }
    override fun merge(local: ProgressPayload, incoming: ProgressPayload) = local.copy(
        chapterKey = local.chapterKey ?: incoming.chapterKey,
        locator = latest(local.locator, incoming.locator),
        completed = latest(local.completed, incoming.completed),
    )

    override suspend fun apply(writes: LocalWrites, key: String, payload: ProgressPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val chapterId = payload.chapterKey?.let { writes.chapterId(it).idOr { outcome -> return outcome } }
        val locator = payload.locator.value
        val queries = writes.database.sync_applyQueries
        queries.upsertProgress(
            entryId = entryId,
            chapterId = chapterId,
            contentKey = payload.contentKey,
            resourceKey = payload.resourceKey,
            resourceRevision = locator.resourceRevision,
            locatorKind = locator.kind,
            position = locator.position,
            extent = locator.extent,
            progression = locator.progression,
            totalProgression = locator.totalProgression,
            extensions = locator.extensions,
            completed = payload.completed.value,
            locatorUpdatedAt = payload.locator.at.physicalMillis,
            completionUpdatedAt = payload.completed.at.physicalMillis,
        )
        val id = queries.progressId(entryId, payload.contentKey, payload.resourceKey).awaitAsOne()
        return ApplyOutcome.Applied(LocalRef.of(id))
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.sync_applyQueries.deleteProgress(it.long(0)) }
        return ApplyOutcome.Applied(null)
    }

    override fun entryKeyOf(payload: ProgressPayload) = payload.entryKey
}
