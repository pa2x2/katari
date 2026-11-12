package app.katari.sync.client.kind.replacement.carry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.apply.Resolved
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.history.HistoryKind
import app.katari.sync.client.kind.history.HistoryPayload
import app.katari.sync.client.kind.progress.ProgressKind
import app.katari.sync.client.kind.progress.ProgressPayload
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.record.LocalReads
import app.katari.sync.data.ChaptersOfEntry
import app.katari.sync.protocol.clock.HybridTimestamp
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.migration.EntryMigrationOption
import tachiyomi.domain.entry.migration.MigrationChapterMarks
import tachiyomi.domain.entry.migration.MigrationChapterMatch
import tachiyomi.domain.entry.migration.MigrationChapterPairs
import tachiyomi.domain.entry.migration.MigrationChapterPosition
import tachiyomi.domain.entry.migration.migratedChapterMarks
import tachiyomi.domain.entry.migration.pairMigrationChaptersByNumber
import tachiyomi.domain.entry.migration.pairMigrationChaptersInOrder

/**
 * What hangs off an entry's chapters, carried over a replacement to the chapters that stand for them ([pairs]): read
 * and bookmark marks and positions when a migration kept chapter state, and for a move also history, whose time read
 * adds to the time read on the new chapter.
 */
internal class CarriedChapterState(private val carry: ReplacementCarry) {

    /**
     * The new entry's chapter marks after an old chapter's changed. A migration decides them with
     * [migratedChapterMarks] over all the old entry's chapters, so marking a later chapter read also reads the earlier
     * ones, as migrating then would have; a mark it changes is carried with the edit that caused it, when that came
     * after the migration. A move keeps each chapter's own marks.
     */
    suspend fun chapterMarks() {
        if (!carry.carries(EntryMigrationOption.CHILD_STATE)) return
        val sources = chapters(carry.from)
        val pairs = pairs(sources)
        if (carry.isMove) {
            pairs.matches.forEach { match ->
                val source = match.source ?: return@forEach
                write(
                    match.target,
                    carry.pick(source.read, match.target.read),
                    carry.pick(source.bookmark, match.target.bookmark),
                )
            }
            return
        }
        val typeName = carry.stored(EntryKind, carry.to)?.type?.uppercase() ?: return
        val type = EntryType.entries.firstOrNull { it.name == typeName } ?: return
        val rule = carry.writes.migrationRules.chapterMarks(type)
        migratedChapterMarks(
            sources = sources,
            pairs = pairs,
            transfer = rule,
            marks = { MigrationChapterMarks(it.read.value, it.bookmark.value) },
        ).forEach { (target, marks) ->
            val match = pairs.matches.first { it.target === target }
            val read = if (marks.read == target.read.value) {
                target.read
            } else {
                readCause(match, marks.read, sources, pairs, rule.read)?.let { Stamped(marks.read, it) } ?: target.read
            }
            val bookmark = match.source?.bookmark
                ?.takeIf { rule.bookmarks && marks.bookmark != target.bookmark.value && it.value == marks.bookmark }
                ?.let(carry::after)
                ?: target.bookmark
            write(target, read, bookmark)
        }
    }

    /**
     * The newest edit after the migration that set [read] on [match]'s target: its own chapter's mark, or for a read
     * mark the later chapters read on the old entry, which read every chapter before them.
     */
    private fun readCause(
        match: MigrationChapterMatch<Chapter>,
        read: Boolean,
        sources: List<Chapter>,
        pairs: MigrationChapterPairs<Chapter>,
        carryRead: Boolean,
    ): HybridTimestamp? {
        val own = match.source?.read?.takeIf { carryRead && it.value == read }?.let(carry::after)?.at
        val place = pairs.number(match.target)
        val covering = if (read && carryRead && place >= 0.0) {
            sources.filter {
                it.read.value && pairs.number(it) >= place
            }.mapNotNull { carry.after(it.read)?.at }.maxOrNull()
        } else {
            null
        }
        return listOfNotNull(own, covering).maxOrNull()
    }

    private suspend fun write(target: Chapter, read: Stamped<Boolean>, bookmark: Stamped<Boolean>) {
        if (read == target.read && bookmark == target.bookmark) return
        // A chapter nobody marked has no record yet; its row holds what the source listed, which the record carries
        val base = carry.stored(ChapterKind, target.key) ?: ChapterKind.payload(
            row = carry.writes.database.sync_local_rowsQueries.chapterRow(target.id).awaitAsOne(),
            entryKey = carry.to,
            read = target.read,
            bookmark = target.bookmark,
            previousDetails = null,
            renamedFrom = null,
        )
        carry.writes.change(ChapterKind, target.key, base.copy(read = read, bookmark = bookmark))
    }

    /**
     * A position in a chapter moves to the matched chapter, whose URL is its resource, when chapter state carries; a
     * position in the entry as a whole always moves, as migration copies it.
     */
    suspend fun progress(from: ProgressPayload) {
        val locator = carry.after(from.locator)
        val completed = carry.after(from.completed)
        if (locator == null && completed == null) return
        val targets = if (from.chapterKey != null) {
            if (!carry.carries(EntryMigrationOption.CHILD_STATE)) return
            val sources = chapters(carry.from)
            val source = sources.firstOrNull { it.key == from.chapterKey } ?: return
            pairedTargets(sources, source).map { it.key to it.url }
        } else {
            listOf(null to from.resourceKey)
        }
        targets.forEach { (chapterKey, resourceKey) ->
            val key = RecordKeys.derive(ProgressKind.name, carry.to, from.contentKey, resourceKey)
            val base = carry.stored(ProgressKind, key)
            carry.writes.change(
                ProgressKind,
                key,
                ProgressPayload(
                    entryKey = carry.to,
                    contentKey = from.contentKey,
                    resourceKey = resourceKey,
                    chapterKey = chapterKey,
                    locator = locator ?: base?.locator ?: from.locator.copy(at = Stamped.UNKNOWN_EDIT),
                    completed = completed ?: base?.completed ?: from.completed.copy(at = Stamped.UNKNOWN_EDIT),
                ),
            )
        }
    }

    /**
     * Later history follows a move only. Every share of time read on the old chapter is carried as read on it
     * ([HistoryKind]), so it adds to the time read on the new chapter, a later share from the same device replaces the
     * one carried before, and carrying it again changes nothing. A migration moves history on the device that migrates
     * as its own time read, which already holds every other device's share as last merged, so carrying them again would
     * count them twice.
     */
    suspend fun history(from: HistoryPayload) {
        if (!carry.isMove) return
        val lastRead = carry.after(from.lastRead)
        if (lastRead == null && from.timeRead.none { it.at > carry.at }) return
        val shares = from.timeRead.map { it.copy(from = it.from ?: from.chapterKey) }
        val sources = chapters(carry.from)
        val source = sources.firstOrNull { it.key == from.chapterKey } ?: return
        pairedTargets(sources, source).forEach { target ->
            carry.writes.change(
                HistoryKind,
                RecordKeys.derive(HistoryKind.name, target.key),
                HistoryPayload(
                    entryKey = carry.to,
                    chapterKey = target.key,
                    lastRead = lastRead ?: Stamped(null, Stamped.UNKNOWN_EDIT),
                    timeRead = shares,
                ),
            )
        }
    }

    /** The new entry's chapters that stand for [source], one of [sources]. */
    private suspend fun pairedTargets(sources: List<Chapter>, source: Chapter): List<Chapter> =
        pairs(sources).matches.filter { it.source === source }.map { it.target }

    /**
     * The old entry's chapters, [sources], paired with the new entry's as the replacement paired them. A migration that
     * paired them by position recorded the pairs, which are taken as they are, whatever this device's chapter lists
     * hold now; any other pairs them as local migration does, by number or else by name. A move keeps its own chapter
     * rows, so each pairs with itself by URL, also where two chapters share a number.
     */
    private suspend fun pairs(sources: List<Chapter>): MigrationChapterPairs<Chapter> {
        val targets = chapters(carry.to)
        val sourceByUrl = sources.associateBy(Chapter::url)
        return when {
            carry.isMove -> pairMigrationChaptersInOrder(
                targets.map { MigrationChapterPosition(sourceByUrl[it.url], it) },
            )
            carry.chapterOrder.isNotEmpty() -> {
                val targetByUrl = targets.associateBy(Chapter::url)
                pairMigrationChaptersInOrder(
                    carry.chapterOrder.map { paired ->
                        MigrationChapterPosition(sourceByUrl[paired.sourceUrl], paired.targetUrl?.let(targetByUrl::get))
                    },
                )
            }
            else -> pairMigrationChaptersByNumber(sources, targets, Chapter::number, Chapter::name)
        }
    }

    /**
     * The chapters of [entryKey] on this device, with their marks as stored, unedited when nothing is. A move takes
     * the old entry's chapter rows along to the new entry ([EntryReplacementKind]), so there the old chapters are
     * those rows under their old keys.
     */
    private suspend fun chapters(entryKey: String): List<Chapter> {
        val entryId = when {
            entryKey == carry.to -> targetId()
            else -> carry.localEntryId(entryKey) ?: if (carry.isMove) targetId() else null
        } ?: return emptyList()
        return carry.writes.database.sync_applyQueries.chaptersOfEntry(entryId).awaitAsList().map { row ->
            val key = RecordKeys.derive(LocalReads.CHAPTER, entryKey, row.url)
            val stored = carry.stored(ChapterKind, key)
            Chapter(
                key = key,
                row = row,
                read = stored?.read ?: Stamped(false, Stamped.UNKNOWN_EDIT),
                bookmark = stored?.bookmark ?: Stamped(false, Stamped.UNKNOWN_EDIT),
            )
        }
    }

    private suspend fun targetId(): Long? = (carry.writes.entryId(carry.to) as? Resolved.Found)?.id

    private class Chapter(
        val key: String,
        row: ChaptersOfEntry,
        val read: Stamped<Boolean>,
        val bookmark: Stamped<Boolean>,
    ) {
        val id: Long = row._id
        val url: String = row.url
        val name: String = row.name
        val number: Double = row.chapter_number
    }
}
