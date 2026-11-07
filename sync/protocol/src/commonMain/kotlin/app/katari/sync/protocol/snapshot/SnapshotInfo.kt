package app.katari.sync.protocol.snapshot

import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * The size of the whole account, asked for before a device takes all of it: a new device, or one resyncing in full.
 * It shows progress and decides whether to wait for Wi-Fi from it. The records themselves come as live-only pulls from
 * 0 ([app.katari.sync.protocol.pull.PullRequest.liveOnly]), and the files they refer to by hash.
 */
@Serializable
data class SnapshotInfo(
    @ProtoNumber(1) val server: ServerIdentity,
    @ProtoNumber(2) val recordCount: Long = 0,
    /** Payload bytes of those records, before compression. */
    @ProtoNumber(3) val recordBytes: Long = 0,
    @ProtoNumber(4) val fileCount: Long = 0,
    @ProtoNumber(5) val fileBytes: Long = 0,
)
