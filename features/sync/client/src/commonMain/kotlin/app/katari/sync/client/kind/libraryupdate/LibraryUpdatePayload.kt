package app.katari.sync.client.kind.libraryupdate

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType
import tachiyomi.domain.library.update.model.LibraryUpdateRun
import tachiyomi.domain.library.update.model.LibraryUpdateRunSummary
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger

/** The newest update of a profile's whole library that any device made. */
@Serializable
data class LibraryUpdatePayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val latest: Stamped<SyncedLibraryUpdate>,
)

/** One library update in numbers, as [LibraryUpdateRunSummary] counts them on the device that ran it. */
@Serializable
data class SyncedLibraryUpdate(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    /** Epoch milliseconds. */
    @ProtoNumber(2) val startedAt: Long,
    /** Epoch milliseconds; null while the update runs, or when it was stopped before finishing. */
    @ProtoNumber(3) val finishedAt: Long? = null,
    /** A [LibraryUpdateTrigger] name in lower case, as the device's database stores it. */
    @ProtoNumber(4) val trigger: String,
    @ProtoNumber(5) val librarySize: Int,
    @ProtoNumber(6) val checked: Int,
    @ProtoNumber(7) val newChapters: Int,
    @ProtoNumber(8) val failed: Int,
    @ProtoNumber(9) val skipped: Int,
    @ProtoNumber(10) val neverChecked: Int,
    @ProtoNumber(11) val switchedOff: Int,
) {
    /** Null for a trigger a newer app version added, which this one can't name. */
    fun summary(): LibraryUpdateRunSummary? {
        val trigger = LibraryUpdateTrigger.entries.find { it.name.lowercase() == trigger } ?: return null
        return LibraryUpdateRunSummary(
            run = LibraryUpdateRun(
                startedAt = startedAt,
                finishedAt = finishedAt,
                trigger = trigger,
                librarySize = librarySize,
            ),
            checked = checked,
            newChapters = newChapters,
            failed = failed,
            skipped = skipped,
            neverChecked = neverChecked,
            switchedOff = switchedOff,
        )
    }

    companion object {
        fun of(device: DeviceId, summary: LibraryUpdateRunSummary) = SyncedLibraryUpdate(
            device = device,
            startedAt = summary.run.startedAt,
            finishedAt = summary.run.finishedAt,
            trigger = summary.run.trigger.name.lowercase(),
            librarySize = summary.run.librarySize,
            checked = summary.checked,
            newChapters = summary.newChapters,
            failed = summary.failed,
            skipped = summary.skipped,
            neverChecked = summary.neverChecked,
            switchedOff = summary.switchedOff,
        )
    }
}
