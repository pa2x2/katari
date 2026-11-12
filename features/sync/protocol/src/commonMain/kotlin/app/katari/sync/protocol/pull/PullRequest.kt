package app.katari.sync.protocol.pull

import app.katari.sync.protocol.device.DeviceDescription
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
     * Leave out the deletions made before the pull from 0. A device taking the whole account pulls from 0 this way,
     * which is what a snapshot download is: it pages and resumes like any pull, and a record deleted before it began
     * is nothing the device takes. A deletion made after it began still comes, as it may delete a record an earlier
     * page brought, and the server keeps it until the device has it.
     */
    @ProtoNumber(3) val liveOnly: Boolean = false,
    /**
     * The chain value the device was handed with [after] ([PullPage.nextChain]), so the server can tell its history up
     * to there is the one the device saw. Empty skips the check, which only a device at 0 has reason to do.
     */
    @ProtoNumber(4) val afterChain: String = "",
    /**
     * How the device presents itself now, for the account's device list to follow an app update or a renamed device
     * instead of keeping what it registered with. A device that leaves it out keeps its description.
     */
    @ProtoNumber(5) val device: DeviceDescription? = null,
    /**
     * The device takes the whole account again, deletions included, because its server was lost or restored from an
     * older copy. The server drops a deletion marker only once every device still syncing has pulled past it, so the
     * device applied the deletions behind the dropped markers before: such a pull isn't refused for them.
     */
    @ProtoNumber(6) val resync: Boolean = false,
)
