package app.katari.sync.client.kind.tracking

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * An entry linked to a tracker, with the values last mirrored from it, so another device shows them before it logs
 * in to the tracker. The tracker's next refresh overrides them. Logins never sync.
 */
@Serializable
data class TrackerBindingPayload(
    @ProtoNumber(1) val entryKey: String,
    /** The tracker's id, which is the same in every app install. */
    @ProtoNumber(2) val trackerId: Long,
    @ProtoNumber(3) val remoteId: Stamped<Long>,
    @ProtoNumber(4) val libraryId: Stamped<Long?>,
    @ProtoNumber(5) val remoteUrl: Stamped<String>,
    @ProtoNumber(6) val title: Stamped<String>,
    @ProtoNumber(7) val lastChapterRead: Stamped<Double>,
    @ProtoNumber(8) val totalChapters: Stamped<Long>,
    @ProtoNumber(9) val status: Stamped<Long>,
    @ProtoNumber(10) val score: Stamped<Double>,
    /** Epoch milliseconds, 0 when unset. */
    @ProtoNumber(11) val startDate: Stamped<Long>,
    @ProtoNumber(12) val finishDate: Stamped<Long>,
    @ProtoNumber(13) val private: Stamped<Boolean>,
)
