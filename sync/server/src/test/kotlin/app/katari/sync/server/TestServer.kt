package app.katari.sync.server

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileReference
import app.katari.sync.protocol.pull.PullRequest
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.protocol.push.PushResponse
import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.device.AuthenticationOutcome
import app.katari.sync.server.device.Device
import app.katari.sync.server.device.RegistrationOutcome
import app.katari.sync.server.file.UploadOutcome
import app.katari.sync.server.record.PullOutcome
import app.katari.sync.server.storage.sqlite.SqliteServerStorage
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The server's services over a real SQLite database in [dataDirectory], with a clock the test moves. */
class TestServer(dataDirectory: Path) : AutoCloseable {
    val clock = SettableClock()
    private val config = ServerConfig.fromEnvironment(emptyMap()).copy(dataDirectory = dataDirectory)
    val services = SyncServices(config, SqliteServerStorage.open(config.databaseFile), clock)
        .also { it.content.prepare() }

    /** With no [password], the account can't sign in with one. */
    fun account(username: String = "reader", password: String? = null, isAdmin: Boolean = false): Long {
        val hash = password?.let(services.passwords::hash).orEmpty()
        return services.storage.accounts.create(username, hash, isAdmin, clock.millis).id
    }

    fun upload(accountId: Long, content: String): FileReference {
        val bytes = content.encodeToByteArray()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
        services.files.upload(accountId, FileCategory.COVER, sha256, bytes.inputStream()) shouldBe UploadOutcome.Stored
        return FileReference(FileCategory.COVER, sha256, bytes.size.toLong())
    }

    /** Registers a device into [accountId] the way a new device joins with a link code. */
    fun device(accountId: Long): LinkedTestDevice {
        val code = services.devices.issueLinkCode(accountId).code
        val outcome = services.registration.register(
            RegisterDeviceRequest(
                DeviceDescription(name = "Device", platform = "Linux", appVersion = "1"),
                linkCode = code,
            ),
            remoteAddress = "192.0.2.1",
        )
        return LinkedTestDevice((outcome as RegistrationOutcome.Registered).response.token)
    }

    inner class LinkedTestDevice(val token: String) {
        /** Makes a call the way every authenticated request does, which also marks the device as seen. */
        val device: Device
            get() = (services.authentication.authenticate(token) as AuthenticationOutcome.Authenticated).device

        fun push(vararg records: RecordEnvelope): PushResponse = services.push.push(
            device,
            PushRequest(records.toList()),
        )

        fun pull(after: Long, liveOnly: Boolean = false, afterChain: String = "", limit: Int = 0): PullOutcome =
            services.pull.pull(device, PullRequest(after, limit, liveOnly, afterChain))
    }

    override fun close() = services.storage.close()
}

fun record(
    key: String,
    basedOn: Long,
    payload: String = key,
    deleted: Boolean = false,
    changeId: String = UUID.randomUUID().toString(),
) = RecordEnvelope(
    key = key,
    kind = "entry",
    kindVersion = 1,
    basedOn = basedOn,
    changeId = changeId,
    device = DeviceId(0),
    deleted = deleted,
    payload = payload.encodeToByteArray(),
)

class SettableClock(private var instant: Instant = Instant.fromEpochMilliseconds(1_800_000_000_000)) : Clock {
    val millis: Long
        get() = instant.toEpochMilliseconds()

    override fun now(): Instant = instant

    fun advance(by: Duration) {
        instant += by
    }
}
