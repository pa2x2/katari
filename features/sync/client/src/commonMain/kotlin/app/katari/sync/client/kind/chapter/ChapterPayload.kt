package app.katari.sync.client.kind.chapter

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A chapter or episode the person marked, or one a refresh found as new. [metadata] lets another device create the row
 * and show it as read, or in Updates, before it refreshes the entry; its next refresh overrides all but when it was
 * found.
 */
@Serializable
data class ChapterPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val read: Stamped<Boolean>,
    @ProtoNumber(4) val bookmark: Stamped<Boolean>,
    @ProtoNumber(5) val metadata: ChapterMetadata,
)

@Serializable
data class ChapterMetadata(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val number: Double,
    @ProtoNumber(3) val scanlator: String? = null,
    /** Epoch milliseconds. */
    @ProtoNumber(4) val dateUpload: Long = 0,
    /**
     * When a device first listed the chapter, in epoch milliseconds; 0 when unknown. Later than its entry joined the
     * Library, it is a new release, which Updates shows.
     */
    @ProtoNumber(5) val dateFetch: Long = 0,
    /** Its place in the source's list on the device that wrote it, newest first; null when unknown. */
    @ProtoNumber(6) val sourceOrder: Long? = null,
)
