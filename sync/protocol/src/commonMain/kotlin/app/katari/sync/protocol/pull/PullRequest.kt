package app.katari.sync.protocol.pull

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Asks for the records written after [after], oldest first. The server takes [after] as the position the device has
 * synced up to, which is what lets it drop deletion markers every device has passed.
 */
@Serializable
data class PullRequest(
    @ProtoNumber(1) val after: Long = 0,
    /** At most this many records; 0 lets the server choose. A page may hold fewer. */
    @ProtoNumber(2) val limit: Int = 0,
    /**
     * Leave out deletions. A device taking the whole account pulls from 0 this way, which is what a snapshot download
     * is: it pages and resumes like any pull, and a device with nothing of its own has no use for deletion markers.
     */
    @ProtoNumber(3) val liveOnly: Boolean = false,
)
