package app.katari.sync.server.storage.sqlite

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.server.device.IssuedLinkCode
import app.katari.sync.server.device.LinkCodeStore

internal class SqliteLinkCodeStore(private val database: SyncServerDatabase) : LinkCodeStore {
    private val queries = database.link_codesQueries

    override fun insert(codeHash: ByteArray, accountId: Long, issuedBy: DeviceId?, expiresAtMillis: Long) {
        queries.insert(
            codeHash = codeHash,
            accountId = accountId,
            expiresAt = expiresAtMillis,
            deviceId = issuedBy?.value,
        )
    }

    override fun unexpired(codeHash: ByteArray, nowMillis: Long): IssuedLinkCode? =
        queries.unexpired(codeHash, nowMillis).executeAsOneOrNull()?.let {
            IssuedLinkCode(accountId = it.account_id, issuedBy = it.device_id?.let(::DeviceId))
        }

    override fun take(codeHash: ByteArray, nowMillis: Long): Long? = database.transactionWithResult {
        queries.unexpired(codeHash, nowMillis).executeAsOneOrNull()
            ?.also { queries.delete(codeHash) }
            ?.account_id
    }

    override fun deleteExpired(nowMillis: Long) {
        queries.deleteExpired(nowMillis)
    }

    override fun deleteForAccount(accountId: Long) {
        queries.deleteForAccount(accountId)
    }
}
