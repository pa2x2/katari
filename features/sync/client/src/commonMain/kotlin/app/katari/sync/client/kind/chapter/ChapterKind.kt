package app.katari.sync.client.kind.chapter

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.core.common.extensions.EMPTY
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.capture.CaptureTrigger.Owner
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.fieldOfNewer
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import kotlinx.serialization.json.JsonObject

/**
 * Chapters, keyed by their entry's key and URL. Local ref: `[chapters._id]`.
 *
 * Rows come from source refreshes, which are no change but for what they find: read and bookmark are captured, and a
 * new row when it arrives already marked (a restore, a migration) or when a refresh found it as a new release of an
 * entry in the Library, which Updates shows, so every device shows it there without refreshing the entry first. A
 * refresh that lists a chapter holding stand-ins for the source's details (`chapters_placeholder_details`) has its
 * record read again, as no edit. A chapter the source dropped keeps its record, since a refresh never deletes anything
 * on other devices.
 */
object ChapterKind : SyncedKind<ChapterPayload>(LocalReads.CHAPTER, 1, ChapterPayload.serializer(), Absence.KEEP) {
    override val capture = listOf(
        CaptureTrigger("chapters", Event.INSERT, listOf("NEW._id"), condition = "NEW.read = 1 OR NEW.bookmark = 1"),
        // Unmarked, as a refresh brings it: whether it is read stays whatever any device made it
        CaptureTrigger(
            "chapters",
            Event.INSERT,
            listOf("NEW._id"),
            condition = "NEW.read = 0 AND NEW.bookmark = 0 AND " +
                "NEW.date_fetch > (SELECT date_added FROM main.entries WHERE _id = NEW.entry_id AND favorite = 1)",
            personsEdit = false,
        ),
        CaptureTrigger(
            "chapters",
            Event.UPDATE,
            listOf("NEW._id"),
            fields = listOf(Field(3, "read"), Field(4, "bookmark")),
        ),
        // Marking a chapter read notes it right after writing the read, which a drain may already have taken
        CaptureTrigger("chapters_marked_read", Event.INSERT, listOf("NEW.chapter_id"), personsEdit = false),
        // The source listed a chapter that held stand-ins for its details, so devices that create it get the source's
        CaptureTrigger(
            "chapters_placeholder_details",
            Event.DELETE,
            listOf("OLD.chapter_id"),
            owners = listOf(Owner.chapter()),
            personsEdit = false,
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
            val read = stamps.field(3, row.read, previous?.read)
            ChapterPayload(
                entryKey = entryKey,
                url = row.url,
                read = read,
                bookmark = stamps.field(4, row.bookmark, previous?.bookmark),
                metadata = ChapterMetadata(
                    name = row.name,
                    number = row.chapter_number,
                    scanlator = row.scanlator,
                    dateUpload = row.date_upload,
                ),
                dateFetch = row.date_fetch,
                sourceOrder = row.source_order,
                markedReadAt = read.at.takeIf { row.read && row.marked_read },
                // Stand-ins would replace the source's details on every device that creates the chapter from the record
                details = if (row.placeholder_details) {
                    previous?.details
                } else {
                    ChapterDetails(
                        memo = row.memo,
                        summary = row.summary,
                        previewUrl = row.preview_url,
                        kind = row.kind,
                        group = row.group_key?.let { key ->
                            ChapterGroup(key = key, title = row.group_title!!, position = row.group_position!!)
                        },
                        dateText = row.date_text,
                    )
                },
            )
        }
    }

    /**
     * A chapter two devices found at different times was found at the later one: a release either device shows in
     * Updates is shown by both, while back catalogue listed by both stays before its entry joined the Library.
     */
    override fun merge(local: ChapterPayload, incoming: ChapterPayload): ChapterPayload {
        val read = latest(local.read, incoming.read)
        return local.copy(
            read = read,
            markedReadAt = read.at.takeIf { it == local.markedReadAt || it == incoming.markedReadAt },
            bookmark = latest(local.bookmark, incoming.bookmark),
            metadata = fieldOfNewer(serializer, local, incoming) { it.metadata },
            dateFetch = maxOf(local.dateFetch, incoming.dateFetch),
            // A copy from a device that doesn't know the place or the details, or holds only stand-ins for the details,
            // never takes them away
            sourceOrder = fieldOfNewer(serializer, local, incoming) { it.sourceOrder }
                ?: local.sourceOrder
                ?: incoming.sourceOrder,
            details = fieldOfNewer(serializer, local, incoming) { it.details } ?: local.details ?: incoming.details,
        )
    }

    /**
     * Creates the row, and its group when this device lists none of that key, from the record's metadata and details
     * when the entry wasn't refreshed here, where the record's device has it in the list; a refresh overrides all but
     * when it was found, which every device takes from the record. A row that holds stand-ins a record without details
     * left (`chapters_placeholder_details`) takes the details of the first record that carries them.
     */
    override suspend fun apply(writes: LocalWrites, key: String, payload: ChapterPayload): ApplyOutcome {
        val entryId = writes.entryId(payload.entryKey).idOr { return it }
        val queries = writes.database.sync_applyQueries
        val metadata = payload.metadata
        val details = payload.details
        val existing = queries.chapterId(entryId, payload.url).awaitAsOneOrNull()
        val written = details?.takeIf {
            existing == null || queries.chapterHoldsPlaceholderDetails(existing).awaitAsOne()
        }
        written?.group?.let { queries.addChapterGroup(entryId, it.key, it.title, it.position) }
        val chapterId = if (existing == null) {
            val place = payload.sourceOrder
            if (place != null && queries.chapterHoldsPlace(entryId, place).awaitAsOne()) {
                queries.freeChapterPlace(entryId, place).await()
            }
            queries.insertChapter(
                entryId = entryId,
                url = payload.url,
                name = metadata.name,
                read = payload.read.value,
                bookmark = payload.bookmark.value,
                chapterNumber = metadata.number,
                scanlator = metadata.scanlator,
                dateUpload = metadata.dateUpload,
                dateFetch = payload.dateFetch,
                sourceOrder = place ?: queries.placeAfterChapters(entryId).awaitAsOne(),
                memo = details?.memo ?: JsonObject.EMPTY,
                summary = details?.summary,
                previewUrl = details?.previewUrl,
                kind = details?.kind ?: 0,
                groupKey = details?.group?.key,
                dateText = details?.dateText,
            ).awaitAsOne().also { if (details == null) queries.notePlaceholderChapterDetails(it) }
        } else {
            written?.let {
                queries.fillChapterDetails(
                    memo = it.memo,
                    summary = it.summary,
                    previewUrl = it.previewUrl,
                    kind = it.kind,
                    groupKey = it.group?.key,
                    dateText = it.dateText,
                    chapterId = existing,
                ).await()
            }
            val wasRead = queries.chapterRead(existing).awaitAsOne()
            queries.updateChapterMarks(
                read = payload.read.value,
                bookmark = payload.bookmark.value,
                dateFetch = payload.dateFetch,
                chapterId = existing,
            )
            if (payload.read.value && !wasRead) writes.chapterRead(entryId, existing, payload.isMarkedRead)
            existing
        }
        if (payload.isMarkedRead) queries.markChapterRead(chapterId) else queries.unmarkChapterRead(chapterId)
        queries.noteNewChapter(dateFetch = payload.dateFetch, entryId = entryId)
        return ApplyOutcome.Applied(ref(chapterId))
    }

    override fun entryKeyOf(payload: ChapterPayload) = payload.entryKey
}
