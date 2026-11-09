package app.katari.sync.client.kind.chapter

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A chapter or episode the person marked, or one a refresh found as new. [metadata] and [sourceOrder] let another
 * device create the row in its place and show it as read, or in Updates, before it refreshes the entry; its next
 * refresh overrides them, but not [dateFetch].
 *
 * [dateFetch] and [sourceOrder] came after the first release with sync, so they are top-level fields, which a device
 * on that release keeps as they are when it rewrites the record ([app.katari.sync.client.payload.UnknownFields]).
 */
@Serializable
data class ChapterPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val read: Stamped<Boolean>,
    @ProtoNumber(4) val bookmark: Stamped<Boolean>,
    @ProtoNumber(5) val metadata: ChapterMetadata,
    /**
     * When a device first listed the chapter, in epoch milliseconds; 0 when unknown. Later than its entry joined the
     * Library, it is a new release, which Updates shows.
     */
    @ProtoNumber(6) val dateFetch: Long = 0,
    /** Its place in the source's list on the device that last wrote it, newest first; null when unknown. */
    @ProtoNumber(7) val sourceOrder: Long? = null,
)

@Serializable
data class ChapterMetadata(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val number: Double,
    @ProtoNumber(3) val scanlator: String? = null,
    /** Epoch milliseconds. */
    @ProtoNumber(4) val dateUpload: Long = 0,
)
