package app.katari.sync.server.storage.sqlite

import app.katari.sync.server.invite.InviteStore

internal class SqliteInviteStore(database: SyncServerDatabase) : InviteStore {
    private val queries = database.invitesQueries

    override fun insert(codeHash: ByteArray, expiresAtMillis: Long) {
        queries.insert(codeHash = codeHash, expiresAt = expiresAtMillis)
    }

    override fun isUnexpired(codeHash: ByteArray, nowMillis: Long): Boolean =
        queries.countUnexpired(codeHash, nowMillis).executeAsOne() > 0

    override fun take(codeHash: ByteArray, nowMillis: Long): Boolean = queries.delete(codeHash, nowMillis).value > 0

    override fun deleteExpired(nowMillis: Long) {
        queries.deleteExpired(nowMillis)
    }
}
