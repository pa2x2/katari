package app.katari.sync.server

import app.katari.sync.server.account.FirstAdminSetup
import app.katari.sync.server.account.PasswordChange
import app.katari.sync.server.account.PasswordCheck
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.account.SyncedDataWipe
import app.katari.sync.server.administration.AccountAdministration
import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.device.AccountDevices
import app.katari.sync.server.device.DeviceAuthentication
import app.katari.sync.server.device.DeviceRegistration
import app.katari.sync.server.file.FileContentStore
import app.katari.sync.server.file.FileTransfers
import app.katari.sync.server.invite.Invites
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.record.GroupPull
import app.katari.sync.server.record.RecordPull
import app.katari.sync.server.record.RecordPush
import app.katari.sync.server.record.SnapshotSummary
import app.katari.sync.server.retention.DeletionMarkerRetention
import app.katari.sync.server.retention.RetentionSchedule
import app.katari.sync.server.retention.UnreferencedFileRetention
import app.katari.sync.server.storage.ServerStorage
import app.katari.sync.server.websession.WebSessions
import java.security.SecureRandom
import kotlin.time.Clock

/** The composition of every service over one storage and data directory. */
class SyncServices(
    val config: ServerConfig,
    val storage: ServerStorage,
    val clock: Clock = Clock.System,
    val random: SecureRandom = SecureRandom(),
) {
    val passwords = PasswordHasher(random = random)
    val changes = AccountChanges()
    val content = FileContentStore(config.dataDirectory)
    val attempts = AttemptLimiter(clock)

    val passwordCheck = PasswordCheck(storage, passwords, attempts)

    val firstAdminSetup = FirstAdminSetup(storage, passwords, clock, random, attempts)
    val registration = DeviceRegistration(storage, changes, passwordCheck, clock, random, attempts)
    val invites = Invites(storage, passwords, registration, clock, random, attempts)
    val administration = AccountAdministration(storage, passwords, content, changes, clock, random)
    val passwordChange = PasswordChange(storage, passwords, passwordCheck)
    val dataWipe = SyncedDataWipe(storage, passwordCheck, content, changes)
    val webSessions = WebSessions(storage, clock, random)
    val authentication = DeviceAuthentication(storage, clock)
    val devices = AccountDevices(storage, changes, clock, random)
    val push = RecordPush(storage, changes)
    val pull = RecordPull(storage, config.pullPageSize)
    val groups = GroupPull(storage)
    val snapshot = SnapshotSummary(storage)
    val files = FileTransfers(storage, content, clock, config.maxFileBytes)
    val retention = RetentionSchedule(
        markers = DeletionMarkerRetention(storage, clock),
        files = UnreferencedFileRetention(storage, content, clock),
        devices = devices,
        invites = invites,
        webSessions = webSessions,
    )
}
