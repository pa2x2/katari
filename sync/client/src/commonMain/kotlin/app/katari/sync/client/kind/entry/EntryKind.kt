package app.katari.sync.client.kind.entry

import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind

/**
 * Entries, keyed by profile uuid, source identity, URL and type ([LocalReads.entryKey]). Local ref: `[entries._id]`.
 *
 * Only the person's fields are captured, so a source refresh rewriting the title or description is no change; the
 * metadata travels with the next change the person makes. An entry joins sync when it is added to the library, or
 * when a record of it (a read chapter, history) needs it as a parent. Entries are never deleted as records: rows of
 * entries outside the library are removed as cache, and a move to another profile is an entry replacement record.
 */
object EntryKind : SyncedKind<EntryPayload>(LocalReads.ENTRY, 1, EntryPayload.serializer(), Absence.KEEP) {
    override val capture = listOf(
        CaptureTrigger("entries", Event.INSERT, listOf("NEW._id"), condition = "NEW.favorite = 1"),
        CaptureTrigger(
            "entries",
            Event.UPDATE,
            listOf("NEW._id"),
            fields = listOf(
                Field(5, "favorite"),
                Field(6, "date_added"),
                Field(7, "display_name"),
                Field(8, "notes"),
                Field(9, "library_pinned"),
                Field(10, "viewer"),
                Field(11, "chapter_flags"),
                // A move to another profile gives the entry a new key, and so a new record.
                Field(Field.EVERY, "profile_id"),
            ),
        ),
    )

    fun ref(entryId: Long): LocalRef = LocalRef.of(entryId)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<EntryPayload> {
        val row = reads.entry(ref.long(0)) ?: return LocalRead.Missing
        val key = reads.entryKeyOf(row) ?: return LocalRead.NotSynced
        return LocalRead.Found(
            key = key,
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(row.profile_uuid))),
        ) { previous, stamps ->
            EntryPayload(
                profileUuid = row.profile_uuid,
                source = SourceReference(row.id_scheme, row.external_id),
                url = row.url,
                type = row.type,
                favorite = stamps.field(5, row.favorite, previous?.favorite),
                dateAdded = stamps.field(6, row.date_added, previous?.dateAdded),
                displayName = stamps.field(7, row.display_name, previous?.displayName),
                notes = stamps.field(8, row.notes, previous?.notes),
                libraryPinned = stamps.field(9, row.library_pinned, previous?.libraryPinned),
                viewerFlags = stamps.field(10, row.viewer, previous?.viewerFlags),
                chapterFlags = stamps.field(11, row.chapter_flags, previous?.chapterFlags),
                metadata = EntryMetadata(
                    title = row.title,
                    author = row.author,
                    artist = row.artist,
                    description = row.description,
                    genres = row.genre.orEmpty(),
                    status = row.status,
                    thumbnailUrl = row.thumbnail_url,
                    backgroundUrl = row.background_url,
                ),
            )
        }
    }
}
