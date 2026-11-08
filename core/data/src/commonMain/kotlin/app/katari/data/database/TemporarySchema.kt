package app.katari.data.database

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Statements every connection to the database runs before it next starts a statement, such as `CREATE TEMP TRIGGER`
 * on main tables. SQLite keeps TEMP objects per connection and drops them with it, so they are never part of the
 * schema: migrations neither see nor fire them. Nothing runs until its owner calls [publish], which it does only once
 * the schema and the app's own upgrade migrations are done.
 *
 * Each connection the driver opens, the writer and every reader, catches up when it next starts a statement outside
 * a transaction. Every transaction starts with such a statement, so what a transaction's first statement saw holds
 * for the whole transaction, and a write outside one, even through a reader running `INSERT … RETURNING`, never runs
 * on a connection that is behind.
 */
@OptIn(ExperimentalAtomicApi::class)
class TemporarySchema {
    internal class Published(val revision: Int, val statements: List<String>)

    private val current = AtomicReference(Published(revision = 0, statements = emptyList()))

    internal val published: Published get() = current.load()

    /**
     * Replaces the statements. A connection runs them once, in one transaction of its own, whatever it ran before, so
     * they must bring it from any earlier publication to this one: drop what earlier ones created, then create.
     */
    fun publish(statements: List<String>) {
        while (true) {
            val last = current.load()
            if (current.compareAndSet(last, Published(last.revision + 1, statements))) return
        }
    }
}
