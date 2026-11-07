package app.katari.sync.server.storage.sqlite

import app.katari.sync.server.websession.WebSessionStore

internal class SqliteWebSessionStore(database: SyncServerDatabase) : WebSessionStore {
    private val queries = database.web_sessionsQueries

    override fun insert(tokenHash: ByteArray, accountId: Long, expiresAtMillis: Long) {
        queries.insert(tokenHash = tokenHash, accountId = accountId, expiresAt = expiresAtMillis)
    }

    override fun accountOf(tokenHash: ByteArray, nowMillis: Long): Long? =
        queries.unexpiredAccount(tokenHash, nowMillis).executeAsOneOrNull()

    override fun delete(tokenHash: ByteArray) {
        queries.delete(tokenHash)
    }

    override fun deleteForAccount(accountId: Long) {
        queries.deleteForAccount(accountId)
    }

    override fun deleteExpired(nowMillis: Long) {
        queries.deleteExpired(nowMillis)
    }
}
