package app.katari.sync.server.storage.sqlite

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.server.device.Device
import app.katari.sync.server.device.DeviceStore

internal class SqliteDeviceStore(database: SyncServerDatabase) : DeviceStore {
    private val queries = database.devicesQueries
    private val retired = database.retired_devicesQueries

    override fun exists(id: DeviceId): Boolean = queries.countWithId(id.value).executeAsOne() > 0

    override fun insert(
        id: DeviceId,
        accountId: Long,
        description: DeviceDescription,
        tokenHash: ByteArray,
        registeredAtMillis: Long,
    ) {
        queries.insert(
            id = id.value,
            accountId = accountId,
            name = description.name,
            platform = description.platform,
            appVersion = description.appVersion,
            tokenHash = tokenHash,
            registeredAt = registeredAtMillis,
        )
    }

    override fun byTokenHash(tokenHash: ByteArray): Device? =
        queries.byTokenHash(tokenHash).executeAsOneOrNull()?.toDevice()

    override fun byId(id: DeviceId): Device? = queries.byId(id.value).executeAsOneOrNull()?.toDevice()

    override fun activeIn(accountId: Long): List<Device> =
        queries.activeInAccount(accountId).executeAsList().map { it.toDevice() }

    override fun describe(id: DeviceId, description: DeviceDescription) {
        queries.describe(
            name = description.name,
            platform = description.platform,
            appVersion = description.appVersion,
            id = id.value,
        )
    }

    override fun touch(id: DeviceId, seenAtMillis: Long) {
        queries.touch(seenAt = seenAtMillis, id = id.value)
    }

    override fun setSyncedUpTo(id: DeviceId, position: Long) {
        queries.setSyncedUpTo(position = position, id = id.value)
    }

    override fun revoke(id: DeviceId) {
        queries.revoke(id.value)
    }

    override fun revokeAll(accountId: Long) {
        queries.revokeAllInAccount(accountId)
    }

    override fun retireAll(accountId: Long) {
        retired.retireAccount(accountId)
    }

    override fun isRetiredToken(tokenHash: ByteArray): Boolean =
        retired.countWithTokenHash(tokenHash).executeAsOne() > 0

    override fun oldestHeldPosition(accountId: Long, seenSinceMillis: Long): Long? =
        queries.oldestHeldPosition(accountId, seenSinceMillis).executeAsOne().position

    private fun Devices.toDevice() = Device(
        id = DeviceId(_id),
        accountId = account_id,
        description = DeviceDescription(name = name, platform = platform, appVersion = app_version),
        registeredAtMillis = registered_at,
        lastSeenMillis = last_seen_at,
        syncedUpTo = synced_up_to,
        isRevoked = is_revoked,
    )
}
