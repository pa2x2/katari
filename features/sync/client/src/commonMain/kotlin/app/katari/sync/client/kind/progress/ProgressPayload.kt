package app.katari.sync.client.kind.progress

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Where the person is inside one resource of an entry (a chapter, an episode, a book section), and whether done. */
@Serializable
data class ProgressPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val contentKey: String,
    @ProtoNumber(3) val resourceKey: String,
    /** The chapter the resource belongs to, when it belongs to one. */
    @ProtoNumber(4) val chapterKey: String? = null,
    @ProtoNumber(5) val locator: Stamped<ProgressLocator>,
    @ProtoNumber(6) val completed: Stamped<Boolean>,
    /**
     * The key of the record the position was kept under before its resource moved here, as a refresh found its chapter
     * at a new URL; null for one that never moved. A device holding edits made there tells by it the position moved
     * back from those it must move along.
     */
    @ProtoNumber(7) val movedFrom: String? = null,
)

@Serializable
data class ProgressLocator(
    @ProtoNumber(1) val kind: String,
    @ProtoNumber(2) val position: Long? = null,
    @ProtoNumber(3) val extent: Long? = null,
    @ProtoNumber(4) val progression: Double? = null,
    @ProtoNumber(5) val totalProgression: Double? = null,
    /** A JSON object of reader-specific detail. */
    @ProtoNumber(6) val extensions: String = "{}",
    @ProtoNumber(7) val resourceRevision: String? = null,
)
