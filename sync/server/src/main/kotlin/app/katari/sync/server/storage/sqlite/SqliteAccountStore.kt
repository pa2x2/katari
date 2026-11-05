package app.katari.sync.server.storage.sqlite

import app.katari.sync.server.account.Account
import app.katari.sync.server.account.AccountStore

internal class SqliteAccountStore(private val database: SyncServerDatabase) : AccountStore {
    private val queries = database.accountsQueries

    override fun count(): Long = queries.count().executeAsOne()

    override fun create(username: String, passwordHash: String, isAdmin: Boolean, createdAtMillis: Long): Account =
        database.transactionWithResult {
            queries.insert(username, passwordHash, isAdmin, createdAtMillis)
            Account(
                id = queries.lastInsertedId().executeAsOne(),
                username = username,
                passwordHash = passwordHash,
                isAdmin = isAdmin,
                isDisabled = false,
            )
        }

    override fun byUsername(username: String): Account? = queries.byUsername(username, ::Account).executeAsOneOrNull()

    override fun byId(id: Long): Account? = queries.byId(id, ::Account).executeAsOneOrNull()

    override fun ids(): List<Long> = queries.ids().executeAsList()

    override fun setPasswordHash(id: Long, passwordHash: String) {
        queries.setPasswordHash(passwordHash = passwordHash, id = id)
    }

    override fun setDisabled(id: Long, disabled: Boolean) {
        queries.setDisabled(isDisabled = disabled, id = id)
    }
}
