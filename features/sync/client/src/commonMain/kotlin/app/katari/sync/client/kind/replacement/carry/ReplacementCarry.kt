package app.katari.sync.client.kind.replacement.carry

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.kind.replacement.EntryReplacementPayload
import app.katari.sync.client.kind.replacement.PairedChapter
import app.katari.sync.client.kind.replacement.Replacement
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.domain.entry.migration.EntryMigrationOption

/** One replacement of the entry [from], and how its later edits carry over to the entry that replaced it. */
internal class ReplacementCarry(val writes: LocalWrites, payload: EntryReplacementPayload) {
    val from: String = payload.from
    private val replacement: Replacement = payload.replacement.value

    /** When the replacement was made; only edits after it carry over. */
    val at: HybridTimestamp = payload.replacement.at
    val to: String = replacement.to
    val isMove: Boolean = replacement.cause == Replacement.Cause.PROFILE_MOVE
    private val options: Set<EntryMigrationOption> =
        replacement.options.mapNotNullTo(HashSet()) { name ->
            EntryMigrationOption.entries.firstOrNull { it.name == name }
        }

    /** The order a migration paired chapters in by position; empty when it paired them by number. */
    val chapterOrder: List<PairedChapter> = payload.migratedChapters?.inOrder.orEmpty()

    /** Whether the replacement carried what [option] stands for: a move carries everything. */
    fun carries(option: EntryMigrationOption): Boolean = isMove || option in options

    fun <T> after(field: Stamped<T>): Stamped<T>? = field.takeIf { it.at > at }

    /** [source] where it was edited after the replacement, else [target], or [source] as never edited without one. */
    fun <T> pick(source: Stamped<T>, target: Stamped<T>?): Stamped<T> =
        after(source) ?: target ?: source.copy(at = Stamped.UNKNOWN_EDIT)

    /**
     * [value], which a migration rule derived from [source], where it changes [target] and [source] was edited after
     * the replacement; else [target].
     */
    fun <T> ruled(value: T, source: Stamped<*>, target: Stamped<T>): Stamped<T> =
        if (value == target.value) target else after(source)?.let { Stamped(value, it.at) } ?: target

    suspend fun <P : Any> stored(kind: SyncedKind<P>, key: String): P? =
        writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
            ?.takeUnless { it.deleted }
            ?.let { kind.decode(it.payload) }

    /** The local ID of the entry [entryKey], when its row is here. */
    suspend fun localEntryId(entryKey: String): Long? {
        val ref = writes.database.sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()?.local_ref ?: return null
        return LocalRef(ref).long(0).takeIf { writes.database.sync_applyQueries.entryExists(it).awaitAsOne() }
    }
}
