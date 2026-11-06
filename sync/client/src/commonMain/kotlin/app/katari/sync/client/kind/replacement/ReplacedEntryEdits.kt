package app.katari.sync.client.kind.replacement

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.Resolved
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.category.CategoryMembershipKind
import app.katari.sync.client.kind.category.CategoryMembershipPayload
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.chapter.ChapterMetadata
import app.katari.sync.client.kind.chapter.ChapterPayload
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entry.EntryPayload
import app.katari.sync.client.kind.history.HistoryKind
import app.katari.sync.client.kind.history.HistoryPayload
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.kind.progress.ProgressPayload
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.data.ChaptersOfEntry
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.domain.entry.migration.matchMigrationChapters

/**
 * Carries edits made to an entry after it was replaced over to the entry that replaced it (D018): a device that
 * hadn't seen the migration yet edits the old entry, and the edit lands on the new one on every device.
 *
 * Only edits stamped after the replacement carry over; what came before, the device that replaced the entry already
 * carried over itself, choosing what to take. Chapters are matched as local migration matches them, by number or
 * else by name, and their read and bookmark marks, history and positions follow; entry fields and category links
 * follow as they are. Each device carries the same edits to the same records, so the changes it pushes merge into
 * one. Time read on the old chapter after the replacement is taken as the device's share on the new one unless that
 * share was changed later.
 */
internal class ReplacedEntryEdits(private val writes: LocalWrites) {

    /** Carries the record [key] of [kind], just applied, over when its entry was replaced. */
    suspend fun <P : Any> carry(kind: SyncedKind<P>, key: String, payload: P) {
        val from = if (kind === EntryKind) key else kind.entryKeyOf(payload) ?: return
        val replacement = replacement(from) ?: return
        carry(kind, payload, replacement)
    }

    /** Carries over every record of the entry [from] this device holds, once its replacement arrived. */
    suspend fun carryAll(from: String) {
        val replacement = replacement(from) ?: return
        val fromId = localEntryId(from) ?: return
        val stored = buildList {
            add(from)
            writes.database.sync_applyQueries.chaptersOfEntry(fromId).awaitAsList().forEach { chapter ->
                val chapterKey = RecordKeys.derive(LocalReads.CHAPTER, from, chapter.url)
                add(chapterKey)
                add(RecordKeys.derive(HistoryKind.name, chapterKey))
            }
        }
        stored.forEach { key -> carryStored(key, replacement) }
        writes.database.sync_recordsQueries.byKind(ProgressKind.name).awaitAsList()
            .plus(writes.database.sync_recordsQueries.byKind(CategoryMembershipKind.name).awaitAsList())
            .filterNot { it.deleted }
            .forEach { record -> carryStored(record.record_key, replacement, onlyOf = from) }
    }

    private suspend fun carryStored(key: String, replacement: Replacement, onlyOf: String? = null) {
        val record = writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return
        if (record.deleted) return
        val kind = when (record.kind) {
            EntryKind.name -> EntryKind
            ChapterKind.name -> ChapterKind
            HistoryKind.name -> HistoryKind
            ProgressKind.name -> ProgressKind
            CategoryMembershipKind.name -> CategoryMembershipKind
            else -> return
        }
        carryDecoded(kind, record.payload, replacement, onlyOf)
    }

    private suspend fun <P : Any> carryDecoded(
        kind: SyncedKind<P>,
        bytes: ByteArray,
        replacement: Replacement,
        onlyOf: String?,
    ) {
        val payload = kind.decode(bytes)
        if (onlyOf != null && kind.entryKeyOf(payload) != onlyOf) return
        carry(kind, payload, replacement)
    }

    private suspend fun <P : Any> carry(kind: SyncedKind<P>, payload: P, replacement: Replacement) {
        when (payload) {
            is EntryPayload -> entry(payload, replacement)
            is ChapterPayload -> matchedChapters(payload.metadata, replacement).forEach { (key, chapter) ->
                chapter(key, chapter, payload, replacement)
            }
            is HistoryPayload -> history(payload, replacement)
            is ProgressPayload -> progress(payload, replacement)
            is CategoryMembershipPayload -> if (payload.removed.at > replacement.at) {
                writes.change(
                    CategoryMembershipKind,
                    RecordKeys.derive(CategoryMembershipKind.name, replacement.to, payload.categoryKey),
                    payload.copy(entryKey = replacement.to),
                )
            }
            else -> Unit
        }
    }

    private suspend fun entry(from: EntryPayload, replacement: Replacement) {
        val stored = writes.database.sync_recordsQueries.getByKey(replacement.to).awaitAsOneOrNull() ?: return
        val to = EntryKind.decode(stored.payload)
        val after = replacement.at
        val carried = to.copy(
            favorite = from.favorite.after(after) ?: to.favorite,
            dateAdded = from.dateAdded.after(after) ?: to.dateAdded,
            displayName = from.displayName.after(after) ?: to.displayName,
            notes = from.notes.after(after) ?: to.notes,
            libraryPinned = from.libraryPinned.after(after) ?: to.libraryPinned,
            viewerFlags = from.viewerFlags.after(after) ?: to.viewerFlags,
            chapterFlags = from.chapterFlags.after(after) ?: to.chapterFlags,
        )
        if (carried != to) writes.change(EntryKind, replacement.to, carried)
    }

    private suspend fun chapter(key: String, row: ChaptersOfEntry, from: ChapterPayload, replacement: Replacement) {
        val read = from.read.after(replacement.at)
        val bookmark = from.bookmark.after(replacement.at)
        if (read == null && bookmark == null) return
        val stored = writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted }
        val base = stored?.let { ChapterKind.decode(it.payload) } ?: ChapterPayload(
            entryKey = replacement.to,
            url = row.url,
            read = Stamped(false, Stamped.UNKNOWN_EDIT),
            bookmark = Stamped(false, Stamped.UNKNOWN_EDIT),
            metadata = ChapterMetadata(name = row.name, number = row.chapter_number),
        )
        writes.change(ChapterKind, key, base.copy(read = read ?: base.read, bookmark = bookmark ?: base.bookmark))
    }

    private suspend fun history(from: HistoryPayload, replacement: Replacement) {
        val lastRead = from.lastRead.after(replacement.at)
        val shares = from.timeRead.filter { it.at > replacement.at }
        if (lastRead == null && shares.isEmpty()) return
        val metadata = chapterMetadata(from.chapterKey) ?: return
        matchedChapters(metadata, replacement).forEach { (chapterKey, _) ->
            writes.change(
                HistoryKind,
                RecordKeys.derive(HistoryKind.name, chapterKey),
                HistoryPayload(
                    entryKey = replacement.to,
                    chapterKey = chapterKey,
                    lastRead = lastRead ?: Stamped(null, Stamped.UNKNOWN_EDIT),
                    timeRead = shares,
                ),
            )
        }
    }

    /** A position in a chapter moves to the matched chapter, whose URL is its resource; any other stays as it is. */
    private suspend fun progress(from: ProgressPayload, replacement: Replacement) {
        val locator = from.locator.after(replacement.at)
        val completed = from.completed.after(replacement.at)
        if (locator == null && completed == null) return
        val targets = from.chapterKey?.let { chapterKey ->
            val metadata = chapterMetadata(chapterKey) ?: return
            matchedChapters(metadata, replacement).map { (key, row) -> key to row.url }
        } ?: listOf(null to from.resourceKey)
        targets.forEach { (chapterKey, resourceKey) ->
            val key = RecordKeys.derive(ProgressKind.name, replacement.to, from.contentKey, resourceKey)
            val stored = writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted }
            val base = stored?.let { ProgressKind.decode(it.payload) }
            writes.change(
                ProgressKind,
                key,
                ProgressPayload(
                    entryKey = replacement.to,
                    contentKey = from.contentKey,
                    resourceKey = resourceKey,
                    chapterKey = chapterKey,
                    locator = locator ?: base?.locator ?: from.locator.copy(at = Stamped.UNKNOWN_EDIT),
                    completed = completed ?: base?.completed ?: from.completed.copy(at = Stamped.UNKNOWN_EDIT),
                ),
            )
        }
    }

    /** The replacement's chapters that stand for a chapter of the replaced entry described by [metadata]. */
    private suspend fun matchedChapters(
        metadata: ChapterMetadata,
        replacement: Replacement,
    ): List<Pair<String, ChaptersOfEntry>> {
        val toId = (writes.entryId(replacement.to) as? Resolved.Found)?.id ?: return emptyList()
        val source = ChaptersOfEntry(_id = -1, url = "", name = metadata.name, chapter_number = metadata.number)
        val targets = writes.database.sync_applyQueries.chaptersOfEntry(toId).awaitAsList()
        return matchMigrationChapters(listOf(source), targets, { it.chapter_number }, { it.name })
            .filter { it.source != null }
            .map { RecordKeys.derive(LocalReads.CHAPTER, replacement.to, it.target.url) to it.target }
    }

    private suspend fun chapterMetadata(chapterKey: String): ChapterMetadata? =
        writes.database.sync_recordsQueries.getByKey(chapterKey).awaitAsOneOrNull()
            ?.takeUnless { it.deleted }
            ?.let { ChapterKind.decode(it.payload).metadata }

    private suspend fun localEntryId(entryKey: String): Long? {
        val ref = writes.database.sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()?.local_ref ?: return null
        return LocalRef(ref).long(0).takeIf { writes.database.sync_applyQueries.entryExists(it).awaitAsOne() }
    }

    private suspend fun replacement(from: String): Replacement? {
        val stored = writes.database.sync_recordsQueries.getByKey(EntryReplacementKind.key(from)).awaitAsOneOrNull()
        if (stored == null || stored.deleted) return null
        val replacement = EntryReplacementKind.decode(stored.payload).replacement
        return Replacement(replacement.value.to, replacement.at)
    }

    private class Replacement(val to: String, val at: HybridTimestamp)
}

private fun <T> Stamped<T>.after(at: HybridTimestamp): Stamped<T>? = takeIf { it.at > at }
