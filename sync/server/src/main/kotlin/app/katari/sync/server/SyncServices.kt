package app.katari.sync.server

import app.katari.sync.server.account.FirstAdminSetup
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.device.AccountDevices
import app.katari.sync.server.device.DeviceAuthentication
import app.katari.sync.server.device.DeviceRegistration
import app.katari.sync.server.file.FileContentStore
import app.katari.sync.server.file.FileTransfers
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.record.RecordPull
import app.katari.sync.server.record.RecordPush
import app.katari.sync.server.record.SnapshotSummary
import app.katari.sync.server.retention.DeletionMarkerRetention
import app.katari.sync.server.retention.RetentionSchedule
import app.katari.sync.server.retention.UnreferencedFileRetention
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import kotlin.time.Clock

/** The composition of every service over one storage and data directory. */
class SyncServices(
    val config: ServerConfig,
    val storage: ServerStorage,
    clock: Clock = Clock.System,
    random: SecureRandom = SecureRandom(),
) {
    val passwords = PasswordHasher(random = random)
    val changes = AccountChanges()
    val content = FileContentStore(config.dataDirectory)

    val firstAdminSetup = FirstAdminSetup(storage, passwords, clock, random)
    val registration = DeviceRegistration(storage, passwords, clock, random)
    val authentication = DeviceAuthentication(storage, clock)
    val devices = AccountDevices(storage, changes, clock, random)
    val push = RecordPush(storage, changes)
    val pull = RecordPull(storage, config.pullPageSize)
    val snapshot = SnapshotSummary(storage)
    val files = FileTransfers(storage, content, clock, config.maxFileBytes)
    val retention = RetentionSchedule(
        markers = DeletionMarkerRetention(storage, clock),
        files = UnreferencedFileRetention(storage, content, clock),
        devices = devices,
    )
}
