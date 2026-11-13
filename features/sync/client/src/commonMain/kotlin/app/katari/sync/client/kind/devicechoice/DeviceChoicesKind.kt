package app.katari.sync.client.kind.devicechoice

import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.merge.latest
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.device.DeviceId

/**
 * What each device of the account chose to sync, one record per device, keyed by its id and edited by that device
 * alone ([app.katari.sync.client.part.DeviceChoices]), so the others can say which devices sync a setting. Every
 * device takes them in whatever it syncs itself. Sync makes them, as it does questions: they are never captured and
 * live in their records alone.
 */
object DeviceChoicesKind : SyncedKind<DeviceChoicesPayload>(
    "device.sync-choices",
    1,
    DeviceChoicesPayload.serializer(),
    Absence.KEEP,
) {
    override val capture: List<CaptureTrigger> = emptyList()

    fun key(device: DeviceId): String = RecordKeys.derive(name, device.value.toString())

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<DeviceChoicesPayload> = LocalRead.NotSynced

    override fun merge(local: DeviceChoicesPayload, incoming: DeviceChoicesPayload) =
        local.copy(choices = latest(local.choices, incoming.choices))

    override suspend fun apply(writes: LocalWrites, key: String, payload: DeviceChoicesPayload): ApplyOutcome =
        ApplyOutcome.Applied(null)
}
