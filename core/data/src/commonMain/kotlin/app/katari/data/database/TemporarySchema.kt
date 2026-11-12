package app.katari.data.database

import kotlin.concurrent.Volatile

/**
 * Statements every connection to the database runs once, before its first statement after the schema is ready, such
 * as `CREATE TEMP TRIGGER` on main tables. SQLite keeps TEMP objects per connection and drops them with it, so they are
 * never part of the schema: schema migrations neither see nor fire them, and they exist from the database's first
 * write after those migrations, whichever part of the app makes it. Features contribute theirs as
 * [TemporarySchemaPart]s; each part's statements must not depend on another part's objects.
 *
 * Each connection the driver opens, the writer and every reader, runs them when it next starts a statement outside a
 * transaction. Every transaction starts with such a statement, so a transaction never runs without them, and a write
 * outside one, even through a reader running `INSERT … RETURNING`, never runs on a connection without them.
 */
class TemporarySchema(internal val statements: List<String>) {
    @Volatile
    internal var isSchemaReady = false
        private set

    /** Called by the driver once the schema is created or migrated; tables a statement names may not exist before. */
    internal fun schemaReady() {
        isSchemaReady = true
    }
}
