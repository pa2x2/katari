package app.katari.sync.client.kind.chapter

import app.katari.sync.client.payload.SourceMemo
import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A chapter or episode the person marked, or one a refresh found as new. [metadata], [details] and [sourceOrder] let
 * another device create the row in its place, as its own refresh would list it, and show it as read, or in Updates,
 * before it refreshes the entry; its next refresh overrides them, but not [dateFetch].
 *
 * [dateFetch], [sourceOrder], [markedReadAt] and [details] came after the first release with sync, so they are
 * top-level fields, which an older device keeps as they are when it rewrites the record
 * ([app.katari.sync.client.payload.UnknownFields]).
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
    /** Null when the device that wrote the record carried none. */
    @ProtoNumber(9) val details: ChapterDetails? = null,
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

/**
 * The rest of the source's description of the chapter, which came after [ChapterMetadata] was released: a device on
 * that release rewrites the metadata message without fields it doesn't know, so these travel as one of their own.
 */
@Serializable
data class ChapterDetails(
    /** What the source's extension stored with the chapter and needs back to open it. */
    @ProtoNumber(1) val memo: SourceMemo,
    @ProtoNumber(2) val summary: String? = null,
    @ProtoNumber(3) val previewUrl: String? = null,
    /** `eu.kanade.tachiyomi.source.entry.ChapterKind.code`, which reads as a regular chapter where it is unknown. */
    @ProtoNumber(4) val kind: Long = 0,
    /** The group the source lists the chapter in; null when it lists it in none. */
    @ProtoNumber(5) val group: ChapterGroup? = null,
    /** The release time as the source shows it, when it gave no date. */
    @ProtoNumber(6) val dateText: String? = null,
)

/** One of an entry's groups of chapters, such as a season or volume, as the source lists them. */
@Serializable
data class ChapterGroup(
    @ProtoNumber(1) val key: String,
    @ProtoNumber(2) val title: String,
    /** Its place among the entry's groups. */
    @ProtoNumber(3) val position: Long,
)
