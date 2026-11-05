package app.katari.sync.server.storage.sqlite

import app.katari.sync.server.device.LinkCodeStore

internal class SqliteLinkCodeStore(private val database: SyncServerDatabase) : LinkCodeStore {
    private val queries = database.link_codesQueries

    override fun insert(codeHash: ByteArray, accountId: Long, expiresAtMillis: Long) {
        queries.insert(codeHash = codeHash, accountId = accountId, expiresAt = expiresAtMillis)
    }

    override fun take(codeHash: ByteArray, nowMillis: Long): Long? = database.transactionWithResult {
        queries.unexpiredAccount(codeHash, nowMillis).executeAsOneOrNull()
            ?.also { queries.delete(codeHash) }
    }

    override fun deleteExpired(nowMillis: Long) {
        queries.deleteExpired(nowMillis)
    }
}
