package app.katari.sync.client.kind.chapter

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.fieldOfNewer
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Chapters, keyed by their entry's key and URL. Local ref: `[chapters._id]`.
 *
 * Rows come from source refreshes, which are no change: only read and bookmark are captured, and a new row only when
 * it arrives already marked (a restore, a migration). A chapter the source dropped keeps its record, since a refresh
 * never deletes anything on other devices.
 */
object ChapterKind : SyncedKind<ChapterPayload>(LocalReads.CHAPTER, 1, ChapterPayload.serializer(), Absence.KEEP) {
    override val capture = listOf(
        CaptureTrigger("chapters", Event.INSERT, listOf("NEW._id"), condition = "NEW.read = 1 OR NEW.bookmark = 1"),
        CaptureTrigger(
            "chapters",
            Event.UPDATE,
            listOf("NEW._id"),
            fields = listOf(Field(3, "read"), Field(4, "bookmark")),
        ),
    )

    fun ref(chapterId: Long): LocalRef = LocalRef.of(chapterId)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ChapterPayload> {
        val row = reads.database.sync_local_rowsQueries.chapterRow(ref.long(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val entryKey = reads.entryKey(row.entry_id) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = reads.chapterKey(entryKey, row.url),
            parents = listOf(RecordRef(EntryKind, EntryKind.ref(row.entry_id))),
        ) { previous, stamps ->
            ChapterPayload(
                entryKey = entryKey,
                url = row.url,
                read = stamps.field(3, row.read, previous?.read),
                bookmark = stamps.field(4, row.bookmark, previous?.bookmark),
                metadata = ChapterMetadata(
                    name = row.name,
                    number = row.chapter_number,
                    scanlator = row.scanlator,
                    dateUpload = row.date_upload,
                ),
            )
        }
    }
    override fun merge(local: ChapterPayload, incoming: ChapterPayload) = local.copy(
        read = latest(local.read, incoming.read),
        bookmark = latest(local.bookmark, incoming.bookmark),
        metadata = fieldOfNewer(serializer, local, incoming) { it.metadata },
    )

    /** Creates the row from the record's metadata when the entry wasn't refreshed here; a refresh overrides it. */
    override suspend fun apply(writes: LocalWrites, key: String, payload: ChapterPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        val existing = queries.chapterId(entryId, payload.url).awaitAsOneOrNull()
        val chapterId = if (existing == null) {
            queries.insertChapter(
                entryId = entryId,
                url = payload.url,
                name = payload.metadata.name,
                read = payload.read.value,
                bookmark = payload.bookmark.value,
                chapterNumber = payload.metadata.number,
                scanlator = payload.metadata.scanlator,
                dateUpload = payload.metadata.dateUpload,
            ).awaitAsOne()
        } else {
            queries.updateChapterMarks(
                read = payload.read.value,
                bookmark = payload.bookmark.value,
                chapterId = existing,
            )
            existing
        }
        return ApplyOutcome.Applied(ref(chapterId))
    }

    override fun entryKeyOf(payload: ChapterPayload) = payload.entryKey
}
