package app.katari.sync.client.kind.entry

import app.katari.sync.client.payload.Stamped
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
)

/** A source as every device knows it, never by the local key a device stores it under. */
@Serializable
data class SourceReference(
    @ProtoNumber(1) val idScheme: String,
    @ProtoNumber(2) val externalId: Long,
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
