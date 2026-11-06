package app.katari.sync.client.kind.chapter

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.kind.entry.EntryKind
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
}
