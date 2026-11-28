package eu.kanade.tachiyomi.data.backup.conversion.newer

import kotlinx.serialization.SerializationException

/**
 * Reads another app's backup around what a newer version of [app] added: each format checks its messages or files
 * against what Katari knows, decodes the backup without its entries with [whole] and each entry with [entry], and hands
 * on [findings]. A part Katari doesn't know is converted around (D123); an entry that doesn't decode is left behind and
 * listed (D124); the rest of the backup not decoding fails the whole file as a newer app's (D129).
 */
internal class NewerAppReading(private val app: String) {
    private val unknown = linkedMapOf<Pair<List<String>, String>, UnknownPart>()
    private val changed = linkedMapOf<Pair<List<String>, String>, ChangedPart>()
    private val unreadable = mutableListOf<UnreadableEntry>()
    private var entries = 0

    var formatVersion: Int? = null

    var appVersion: String? = null

    fun unknown(where: List<String>, name: String, kind: String) {
        unknown.compute(where to name) { _, part ->
            part?.copy(times = part.times + 1) ?: UnknownPart(where, name, kind, times = 1)
        }
    }

    fun changed(where: List<String>, name: String, expected: String, found: String) {
        changed.compute(where to name) { _, part ->
            part?.copy(times = part.times + 1) ?: ChangedPart(where, name, expected, found, times = 1)
        }
    }

    /**
     * The backup without its entries. [check] records what Katari doesn't know in it, and throws when it is damaged;
     * [decode] failing then means a newer app laid it out.
     */
    fun <T> whole(check: () -> Unit, decode: () -> T): T {
        check()
        return try {
            decode()
        } catch (e: SerializationException) {
            throw NewerAppBackupException(app, e)
        }
    }

    /**
     * The backup's next entry, or null when it doesn't decode, which leaves it behind named by [title] where that can
     * be read. [check] throws when the entry is damaged, which fails the whole file as before.
     */
    fun <T> entry(title: () -> String?, check: () -> Unit, decode: () -> T): T? {
        entries++
        check()
        return try {
            decode()
        } catch (e: SerializationException) {
            // The decoder wraps what went wrong in the entry's own failure
            val reason = generateSequence<Throwable>(e) { it.cause }.last().message.orEmpty()
            unreadable += UnreadableEntry(entries, runCatching(title).getOrNull(), reason)
            null
        }
    }

    /**
     * Part of an entry, read before the entries are converted, or null when it doesn't decode. The entry itself is
     * then left behind by [entry], since the part's fields are among the entry's.
     */
    fun <T> part(decode: () -> T): T? = try {
        decode()
    } catch (_: SerializationException) {
        null
    }

    fun findings() = NewerAppFindings(
        unknownParts = unknown.values.toList(),
        changedParts = changed.values.toList(),
        unreadableEntries = unreadable.toList(),
        formatVersion = formatVersion,
        appVersion = appVersion,
    )
}
