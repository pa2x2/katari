package app.katari.sync.client.kind.entry

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
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.want
import app.katari.sync.client.file.local.wanted
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.fieldOfNewer
import app.katari.sync.client.merge.latest
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileReference
import kotlinx.serialization.json.JsonObject
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.model.StubSource

/**
 * Entries, keyed by profile uuid, source identity, URL and type ([LocalReads.entryKey]). Local ref: `[entries._id]`.
 *
 * Only the person's fields are captured, so a source refresh rewriting the title or description is no change; the
 * metadata travels with the next change the person makes. A refresh that replaces stand-ins for the source's details
 * (`entries_placeholder_details`) has the record read again at once, as no edit. The custom cover is the file the
 * person set ([FileOwner.CustomCover]), captured from the app's file store rather than by a trigger
 * ([app.katari.sync.client.file.capture.FileCapture]). An entry joins sync when it is added to the library, or
 * when a record of it (a read chapter, history) needs it as a parent. Entries are never deleted as records: rows of
 * entries outside the library are removed as cache, and a move to another profile is an entry replacement record.
 */
object EntryKind : SyncedKind<EntryPayload>(LocalReads.ENTRY, 1, EntryPayload.serializer(), Absence.KEEP) {
    override fun profileOf(payload: EntryPayload) = payload.profileUuid

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
        // Its details were fetched where it held stand-ins for them, so devices that create it get the source's
        CaptureTrigger(
            "entries_placeholder_details",
            Event.DELETE,
            listOf("OLD.entry_id"),
            owners = listOf(Owner.entry()),
            personsEdit = false,
        ),
    )

    /** `sync_outbox.fields` for a change of the custom cover alone. */
    internal const val CUSTOM_COVER_FIELDS: Long = 1L shl 13

    fun ref(entryId: Long): LocalRef = LocalRef.of(entryId)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<EntryPayload> {
        val row = reads.entry(ref.long(0)) ?: return LocalRead.Missing
        val key = reads.entryKeyOf(row) ?: return LocalRead.NotSynced
        // A cover sync knows nothing of here, as after a restore wrote the row under a new ID, leaves the record's.
        val cover = reads.database.wanted(FileOwner.CustomCover(row._id))
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
                customCover = cover?.let { stamps.field(13, it.file, previous?.customCover) }
                    ?: previous?.customCover
                    ?: Stamped(null, Stamped.UNKNOWN_EDIT),
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
                sourceName = row.source_name?.takeIf { StubSource.isKnownName(it) } ?: previous?.sourceName,
                // Stand-ins would replace the source's details on every device that creates the entry from the record
                details = if (row.placeholder_details) {
                    previous?.details
                } else {
                    EntryDetails(
                        memo = row.memo,
                        updateStrategy = UpdateStrategyColumnAdapter.encode(row.update_strategy),
                    )
                },
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
        customCover = latest(local.customCover, incoming.customCover),
        // A record from a device that doesn't know the name or the details, or holds only stand-ins for the details,
        // never takes them away
        sourceName = fieldOfNewer(serializer, local, incoming) { it.sourceName }
            ?: local.sourceName
            ?: incoming.sourceName,
        details = fieldOfNewer(serializer, local, incoming) { it.details } ?: local.details ?: incoming.details,
    )

    override fun files(payload: EntryPayload): List<FileReference> =
        listOfNotNull(payload.customCover.value?.reference(FileCategory.COVER))

    /**
     * Writes the person's fields, and the source's metadata and details only into a row it creates: a device that has
     * the row keeps those of its own refreshes, but takes the record's details in place of stand-ins a record without
     * them left (`entries_placeholder_details`). The source's name is stored for a source no extension here named. The
     * custom cover is put in place after the transaction, fetched first when no file here holds it. An entry another
     * one replaced stays out of the library; its changes carry over to the replacement instead. An entry moved to
     * another profile has no row of its own any more, as after moving it here, so its record makes none. Local-source
     * entries never sync.
     */
    override suspend fun apply(writes: LocalWrites, key: String, payload: EntryPayload): ApplyOutcome {
        if (payload.source.idScheme == SourceIdScheme.LOCAL.key) return ApplyOutcome.Applied(null)
        if (writes.movedTo(key) != null) return ApplyOutcome.Applied(null)
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        val source = writes.sourceKey(payload.source)
        val queries = writes.database.sync_applyQueries
        payload.sourceName?.let { queries.nameSource(source, it) }
        val favorite = payload.favorite.value && writes.replacementOf(key) == null
        val pinned = payload.libraryPinned.value && favorite
        val existing = queries.entryId(profileId, source, payload.url, payload.type).awaitAsOneOrNull()
        val details = payload.details
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
                memo = details?.memo ?: JsonObject.EMPTY,
                updateStrategy = UpdateStrategyColumnAdapter.decode(details?.updateStrategy ?: 0),
            ).awaitAsOne().also { if (details == null) queries.notePlaceholderEntryDetails(it) }
        } else {
            if (details != null && queries.entryHoldsPlaceholderDetails(existing).awaitAsOne()) {
                queries.fillEntryDetails(
                    memo = details.memo,
                    updateStrategy = UpdateStrategyColumnAdapter.decode(details.updateStrategy),
                    entryId = existing,
                ).await()
            }
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
        writes.database.want(FileOwner.CustomCover(entryId), payload.customCover.value)
        return ApplyOutcome.Applied(ref(entryId))
    }
}
