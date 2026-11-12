package app.katari.sync.client.kind.entry

import app.katari.sync.client.payload.SourceMemo
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.SyncedFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A title in a profile: what the person chose about it, and the source's own description of it, so another device
 * can show it before it refreshes the entry, even without the source installed. A device that refreshes the entry
 * itself keeps its own metadata.
 */
@Serializable
data class EntryPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val source: SourceReference,
    @ProtoNumber(3) val url: String,
    @ProtoNumber(4) val type: String,
    @ProtoNumber(5) val favorite: Stamped<Boolean>,
    /** Epoch milliseconds. */
    @ProtoNumber(6) val dateAdded: Stamped<Long>,
    @ProtoNumber(7) val displayName: Stamped<String?>,
    @ProtoNumber(8) val notes: Stamped<String>,
    @ProtoNumber(9) val libraryPinned: Stamped<Boolean>,
    @ProtoNumber(10) val viewerFlags: Stamped<Long>,
    @ProtoNumber(11) val chapterFlags: Stamped<Long>,
    @ProtoNumber(12) val metadata: EntryMetadata,
    /** The cover the person set for the entry; null for none, the source's own cover. */
    @ProtoNumber(13) val customCover: Stamped<SyncedFile?> = Stamped(null, Stamped.UNKNOWN_EDIT),
    /**
     * The source's name, so a device that never had its extension can name it; null when no device that wrote
     * the record knew it.
     */
    @ProtoNumber(14) val sourceName: String? = null,
    /** Null when the device that wrote the record carried none. */
    @ProtoNumber(15) val details: EntryDetails? = null,
)

/** The source's description of the entry as this device last fetched it. */
@Serializable
data class EntryMetadata(
    @ProtoNumber(1) val title: String,
    @ProtoNumber(2) val author: String? = null,
    @ProtoNumber(3) val artist: String? = null,
    @ProtoNumber(4) val description: String? = null,
    @ProtoNumber(5) val genres: List<String> = emptyList(),
    @ProtoNumber(6) val status: Long = 0,
    @ProtoNumber(7) val thumbnailUrl: String? = null,
    @ProtoNumber(8) val backgroundUrl: String? = null,
)

/**
 * The rest of the source's description of the entry, which came after [EntryMetadata] was released: a device on that
 * release rewrites the metadata message without fields it doesn't know, so these travel as one of their own.
 */
@Serializable
data class EntryDetails(
    /** What the source's extension stored with the entry and needs back to list its chapters. */
    @ProtoNumber(1) val memo: SourceMemo,
    /** The `entries.update_strategy` column's value: whether library updates refresh the entry or leave it. */
    @ProtoNumber(2) val updateStrategy: Long = 0,
)
