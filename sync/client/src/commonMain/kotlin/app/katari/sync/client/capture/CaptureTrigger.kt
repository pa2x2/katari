package app.katari.sync.client.capture

import app.katari.sync.client.record.FieldStamps

/**
 * A TEMP trigger that records edits of [table] into `sync_outbox` for one kind. [ref] are SQL expressions over the
 * row (`NEW.` or `OLD.` columns) that locate it, in the order the kind reads them. An update is recorded only when a
 * column of [fields] changed, with the payload fields it touched; inserts and deletes touch every field.
 */
internal class CaptureTrigger(
    val table: String,
    val event: Event,
    val ref: List<String>,
    val fields: List<Field> = emptyList(),
    /** An SQL condition the row must also meet. */
    val condition: String? = null,
) {
    init {
        require((event == Event.UPDATE) == fields.isNotEmpty()) { "Only updates name the fields they capture" }
    }

    enum class Event { INSERT, UPDATE, DELETE }

    /** Payload field [number] changes when any of [columns] does; [EVERY] marks the whole record changed. */
    class Field(val number: Int, vararg val columns: String) {
        companion object {
            const val EVERY = -1
        }
    }

    fun name(kind: String, index: Int): String =
        "sync_capture_${kind.replace(Regex("[^a-z]"), "_")}_${event.name.lowercase()}_$index"

    fun createStatement(kind: String, index: Int): String {
        val localRef = "json_array(${ref.joinToString()})"
        val changed = when (event) {
            Event.UPDATE -> fields.joinToString(" | ") { field ->
                val bit = if (field.number == Field.EVERY) FieldStamps.EVERY_FIELD else 1L shl field.number
                val differs = field.columns.joinToString(" OR ") { "OLD.$it IS NOT NEW.$it" }
                "(CASE WHEN $differs THEN $bit ELSE 0 END)"
            }
            Event.INSERT, Event.DELETE -> FieldStamps.EVERY_FIELD.toString()
        }
        val columns = when (event) {
            Event.UPDATE -> " OF " + fields.flatMap { it.columns.toList() }.distinct().joinToString()
            Event.INSERT, Event.DELETE -> ""
        }
        val conditions = listOfNotNull(
            "($changed) != 0".takeIf { event == Event.UPDATE },
            condition?.let { "($it)" },
        )
        val recordKey = when (event) {
            Event.DELETE -> "(SELECT record_key FROM sync_records WHERE kind = '$kind' AND local_ref = $localRef)"
            Event.INSERT, Event.UPDATE -> "NULL"
        }
        // A deleted row's record is detached from it, so a row that later takes the same local id starts its own.
        val detach = when (event) {
            Event.DELETE -> "UPDATE sync_records SET local_ref = NULL WHERE kind = '$kind' AND local_ref = $localRef;"
            Event.INSERT, Event.UPDATE -> ""
        }
        return """
            CREATE TEMP TRIGGER ${name(kind, index)}
            AFTER ${event.name}$columns ON main.$table
            FOR EACH ROW${if (conditions.isEmpty()) "" else " WHEN " + conditions.joinToString(" AND ")}
            BEGIN
                INSERT INTO sync_outbox(kind, local_ref, fields, record_key, changed_at, seen_physical, seen_counter)
                SELECT '$kind', $localRef, $changed, $recordKey, $NOW_MILLIS, seen_physical, seen_counter
                FROM sync_clock WHERE id = 0;
                $detach
            END
        """.trimIndent()
    }

    private companion object {
        // Wall-clock milliseconds; julianday('now') has millisecond precision on every SQLite version.
        const val NOW_MILLIS = "CAST(round((julianday('now') - 2440587.5) * 86400000) AS INTEGER)"
    }
}
