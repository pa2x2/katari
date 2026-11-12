package app.katari.sync.server.storage.sqlite

import app.cash.sqldelight.ColumnAdapter

/** A device's message kinds as one text, a line each; requests are checked to hold no line break in a kind. */
internal object MessageKindsAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> =
        if (databaseValue.isEmpty()) emptyList() else databaseValue.split('\n')

    override fun encode(value: List<String>): String = value.joinToString("\n")
}
