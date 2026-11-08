package app.katari.data.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL

/** A connection that runs [schema]'s newest statements before it starts a statement outside a transaction. */
internal class TemporarySchemaConnection(
    private val connection: SQLiteConnection,
    private val schema: TemporarySchema,
) : SQLiteConnection by connection {
    // Only read and written by whoever holds the connection, which the driver's pool hands to one caller at a time.
    private var appliedRevision = 0

    override fun prepare(sql: String): SQLiteStatement = CatchingUpStatement(connection.prepare(sql))

    private fun catchUp() {
        val published = schema.published
        if (published.revision == appliedRevision || connection.inTransaction()) return
        connection.execSQL("BEGIN")
        try {
            published.statements.forEach(connection::execSQL)
            connection.execSQL("COMMIT")
        } catch (error: Throwable) {
            runCatching { connection.execSQL("ROLLBACK") }.exceptionOrNull()?.let(error::addSuppressed)
            throw error
        }
        appliedRevision = published.revision
    }

    private inner class CatchingUpStatement(private val statement: SQLiteStatement) : SQLiteStatement by statement {
        // Catching up changes the schema, which must not happen while this statement is part way through its rows.
        private var isRunning = false

        override fun step(): Boolean {
            if (!isRunning) catchUp()
            return statement.step().also { isRunning = it }
        }

        override fun reset() {
            isRunning = false
            statement.reset()
        }
    }
}
