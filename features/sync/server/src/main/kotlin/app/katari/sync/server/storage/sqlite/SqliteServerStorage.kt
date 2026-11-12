package app.katari.sync.server.storage.sqlite

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.katari.sync.server.account.AccountStore
import app.katari.sync.server.device.DeviceStore
import app.katari.sync.server.device.LinkCodeStore
import app.katari.sync.server.file.StoredFileIndex
import app.katari.sync.server.invite.InviteStore
import app.katari.sync.server.message.MessageStore
import app.katari.sync.server.record.RecordStore
import app.katari.sync.server.storage.ServerStorage
import app.katari.sync.server.websession.WebSessionStore
import java.nio.file.Path
import java.util.UUID

/** Server storage in one SQLite file, created or upgraded to the current schema when it is opened. */
class SqliteServerStorage private constructor(private val driver: SqliteFileDriver) : ServerStorage {
    private val database = SyncServerDatabase(
        driver,
        record_filesAdapter = Record_files.Adapter(categoryAdapter = EnumColumnAdapter()),
        stored_filesAdapter = Stored_files.Adapter(categoryAdapter = EnumColumnAdapter()),
    )

    override val instanceId: String = database.transactionWithResult {
        database.server_metaQueries.insertIfAbsent(INSTANCE_ID, UUID.randomUUID().toString())
        database.server_metaQueries.valueOf(INSTANCE_ID).executeAsOne()
    }

    override val accounts: AccountStore = SqliteAccountStore(database)
    override val devices: DeviceStore = SqliteDeviceStore(database)
    override val linkCodes: LinkCodeStore = SqliteLinkCodeStore(database)
    override val records: RecordStore = SqliteRecordStore(database)
    override val files: StoredFileIndex = SqliteStoredFileIndex(database)
    override val invites: InviteStore = SqliteInviteStore(database)
    override val webSessions: WebSessionStore = SqliteWebSessionStore(database)
    override val messages: MessageStore = SqliteMessageStore(database)

    override fun <T> transaction(block: () -> T): T = database.transactionWithResult { block() }

    override fun close() = driver.close()

    companion object {
        private const val INSTANCE_ID = "instance_id"

        fun open(file: Path): SqliteServerStorage {
            val driver = SqliteFileDriver(file)
            try {
                driver.createOrUpgradeSchema()
            } catch (e: Throwable) {
                driver.close()
                throw e
            }
            return SqliteServerStorage(driver)
        }

        // The schema version is SQLite's user_version, as SQLDelight's own drivers keep it.
        private fun SqliteFileDriver.createOrUpgradeSchema() {
            val schema = SyncServerDatabase.Schema
            object : TransacterImpl(this) {}.transaction {
                val version = executeQuery(
                    identifier = null,
                    sql = "PRAGMA user_version",
                    mapper = { cursor ->
                        cursor.next()
                        QueryResult.Value(cursor.getLong(0) ?: 0L)
                    },
                    parameters = 0,
                ).value
                check(version <= schema.version) {
                    "The database is at schema version $version, newer than this server's ${schema.version}; " +
                        "run the server version that wrote it or a later one"
                }
                if (version == schema.version) return@transaction
                if (version == 0L) {
                    schema.create(this@createOrUpgradeSchema)
                } else {
                    schema.migrate(this@createOrUpgradeSchema, version, schema.version)
                }
                execute(identifier = null, sql = "PRAGMA user_version = ${schema.version}", parameters = 0)
            }
        }
    }
}
