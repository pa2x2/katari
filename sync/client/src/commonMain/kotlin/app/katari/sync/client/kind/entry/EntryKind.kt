package app.katari.sync.client.kind.entry

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.fieldOfNewer
import app.katari.sync.client.merge.latest
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import tachiyomi.domain.source.identity.SourceIdScheme

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
    override fun merge(local: EntryPayload, incoming: EntryPayload) = local.copy(
        favorite = latest(local.favorite, incoming.favorite),
        dateAdded = latest(local.dateAdded, incoming.dateAdded),
        displayName = latest(local.displayName, incoming.displayName),
        notes = latest(local.notes, incoming.notes),
        libraryPinned = latest(local.libraryPinned, incoming.libraryPinned),
        viewerFlags = latest(local.viewerFlags, incoming.viewerFlags),
        chapterFlags = latest(local.chapterFlags, incoming.chapterFlags),
        metadata = fieldOfNewer(serializer, local, incoming) { it.metadata },
    )

    /**
     * Writes the person's fields, and the source's metadata only into a row it creates: a device that has the row
     * keeps the metadata of its own refreshes. An entry another one replaced stays out of the library; its changes
     * carry over to the replacement instead. An entry moved to another profile has no row of its own any more, as
     * after moving it here, so its record makes none. Local-source entries never sync.
     */
    override suspend fun apply(writes: LocalWrites, key: String, payload: EntryPayload): ApplyOutcome {
        if (payload.source.idScheme == SourceIdScheme.LOCAL.key) return ApplyOutcome.Applied(null)
        if (writes.movedTo(key) != null) return ApplyOutcome.Applied(null)
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        val source = writes.sourceKey(payload.source)
        val favorite = payload.favorite.value && writes.replacementOf(key) == null
        val pinned = payload.libraryPinned.value && favorite
        val queries = writes.database.sync_applyQueries
        val existing = queries.entryId(profileId, source, payload.url, payload.type).awaitAsOneOrNull()
        val entryId = if (existing == null) {
            val metadata = payload.metadata
            queries.insertEntry(
                profileId = profileId,
                source = source,
                url = payload.url,
                type = payload.type,
                title = metadata.title,
                author = metadata.author,
                artist = metadata.artist,
                description = metadata.description,
                genre = metadata.genres,
                status = metadata.status,
                thumbnailUrl = metadata.thumbnailUrl,
                backgroundUrl = metadata.backgroundUrl,
                favorite = favorite,
                dateAdded = payload.dateAdded.value,
                displayName = payload.displayName.value,
                notes = payload.notes.value,
                libraryPinned = pinned,
                viewer = payload.viewerFlags.value,
                chapterFlags = payload.chapterFlags.value,
            ).awaitAsOne()
        } else {
            queries.updateEntry(
                favorite = favorite,
                dateAdded = payload.dateAdded.value,
                displayName = payload.displayName.value,
                notes = payload.notes.value,
                libraryPinned = pinned,
                viewer = payload.viewerFlags.value,
                chapterFlags = payload.chapterFlags.value,
                entryId = existing,
            )
            existing
        }
        return ApplyOutcome.Applied(ref(entryId))
    }
}
