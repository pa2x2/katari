package app.katari.sync.client.kind.chapter

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A chapter or episode the person marked, or one a refresh found as new. [metadata] and [sourceOrder] let another
 * device create the row in its place and show it as read, or in Updates, before it refreshes the entry; its next
 * refresh overrides them, but not [dateFetch].
 *
 * [dateFetch], [sourceOrder] and [markedReadAt] came after the first release with sync, so they are top-level fields,
 * which an older device keeps as they are when it rewrites the record ([app.katari.sync.client.payload.UnknownFields]).
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
    /**
     * When [read] holds the person marking the chapter read rather than reading it, its edit time. It counts only
     * while it is [read]'s: an older device that read the chapter again keeps this as it was, next to a newer [read].
     */
    @ProtoNumber(8) val markedReadAt: HybridTimestamp? = null,
) {
    val isMarkedRead: Boolean
        get() = read.value && markedReadAt == read.at
}

@Serializable
data class ChapterMetadata(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val number: Double,
    @ProtoNumber(3) val scanlator: String? = null,
    /** Epoch milliseconds. */
    @ProtoNumber(4) val dateUpload: Long = 0,
)
