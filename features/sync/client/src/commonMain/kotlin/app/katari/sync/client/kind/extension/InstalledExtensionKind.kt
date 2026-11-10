package app.katari.sync.client.kind.extension

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.capture.CaptureTrigger.Event
import app.katari.sync.client.capture.CaptureTrigger.Field
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind

/**
 * Which devices of the account have an extension installed, keyed by package name, so each can be offered what the
 * others installed and uninstalled. Local ref: `[pkg_name]`, this device's row of `extension_installs`, which
 * [app.katari.sync.client.extension.InstalledExtensionLog] keeps as extensions come and go.
 *
 * Each device writes only its own share of the record, from its own row; the other devices' shares stay in the
 * record and nothing of them is written here. A record whose extension this device never had still takes its local
 * ref, so the device's first install of it is read into the same record.
 */
object InstalledExtensionKind : SyncedKind<InstalledExtensionPayload>(
    "extension.installed",
    1,
    InstalledExtensionPayload.serializer(),
    Absence.KEEP,
) {
    override val dataGroup = DataGroup.INSTALLED_EXTENSIONS

    override val capture = listOf(
        CaptureTrigger("extension_installs", Event.INSERT, listOf("NEW.pkg_name")),
        CaptureTrigger(
            "extension_installs",
            Event.UPDATE,
            listOf("NEW.pkg_name"),
            fields = listOf(Field(Field.EVERY, "installed", "changed_at")),
        ),
    )

    fun ref(pkgName: String): LocalRef = LocalRef.of(pkgName)

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<InstalledExtensionPayload> {
        val row = reads.database.sync_local_rowsQueries.extensionInstallRow(ref.text(0)).awaitAsOneOrNull()
            ?: return LocalRead.Missing
        val own = DeviceInstall(reads.device, row.installed, row.changed_at)
        return LocalRead.Found(RecordKeys.derive(name, row.pkg_name)) { previous, _ ->
            // This device's row is its share as it is now, even when its clock has since gone back
            val others = previous?.devices.orEmpty().filter { it.device != own.device }
            InstalledExtensionPayload(row.pkg_name, (others + own).sortedBy { it.device.value })
        }
    }

    override fun merge(local: InstalledExtensionPayload, incoming: InstalledExtensionPayload) = local.copy(
        devices = (local.devices + incoming.devices)
            .groupBy { it.device }
            .map { (_, shares) -> shares.maxWith(DeviceInstall.ORDER) }
            .sortedBy { it.device.value },
    )

    // Nothing is written: this device's share is its own row, and the other devices' have no rows here to go to
    override suspend fun apply(writes: LocalWrites, key: String, payload: InstalledExtensionPayload): ApplyOutcome =
        ApplyOutcome.Applied(ref(payload.pkgName))
}
