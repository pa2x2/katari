package app.katari.sync.client.kind.replacement.carry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.apply.Resolved
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.chapter.ChapterKind
import app.katari.sync.client.kind.chapter.ChapterMetadata
import app.katari.sync.client.kind.chapter.ChapterPayload
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
import tachiyomi.domain.entry.migration.matchMigrationChapters
import tachiyomi.domain.entry.migration.migratedChapterMarks

/**
 * What hangs off an entry's chapters, carried over a replacement to the chapters that stand for them, matched as
 * local migration matches them, by number or else by name: read and bookmark marks and positions when a migration
 * kept chapter state, and for a move also history, whose time read adds to the time read on the new chapter.
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
        val targets = chapters(carry.to)
        val matches = matchMigrationChapters(sources, targets, Chapter::number, Chapter::name)
        if (carry.isMove) {
            matches.forEach { match ->
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
            matches = matches,
            transfer = rule,
            marks = { MigrationChapterMarks(it.read.value, it.bookmark.value) },
            number = Chapter::number,
        ).forEach { (target, marks) ->
            val match = matches.first { it.target === target }
            val read = if (marks.read == target.read.value) {
                target.read
            } else {
                readCause(match, marks.read, sources, rule.read)?.let { Stamped(marks.read, it) } ?: target.read
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
        carryRead: Boolean,
    ): HybridTimestamp? {
        val own = match.source?.read?.takeIf { carryRead && it.value == read }?.let(carry::after)?.at
        val covering = if (read && carryRead && match.target.number >= 0.0) {
            sources.filter {
                it.read.value && it.number >= match.target.number
            }.mapNotNull { carry.after(it.read)?.at }.maxOrNull()
        } else {
            null
        }
        return listOfNotNull(own, covering).maxOrNull()
    }

    private suspend fun write(target: Chapter, read: Stamped<Boolean>, bookmark: Stamped<Boolean>) {
        if (read == target.read && bookmark == target.bookmark) return
        val base = carry.stored(ChapterKind, target.key) ?: ChapterPayload(
            entryKey = carry.to,
            url = target.url,
            read = target.read,
            bookmark = target.bookmark,
            metadata = ChapterMetadata(name = target.name, number = target.number),
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
            val source = chapters(carry.from).firstOrNull { it.key == from.chapterKey } ?: return
            matchedChapters(source).map { it.key to it.url }
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
        val source = chapters(carry.from).firstOrNull { it.key == from.chapterKey } ?: return
        matchedChapters(source).forEach { target ->
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

    private suspend fun matchedChapters(source: Chapter): List<Chapter> =
        matchMigrationChapters(listOf(source), chapters(carry.to), Chapter::number, Chapter::name)
            .filter { it.source != null }
            .map { it.target }

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
        val url: String = row.url
        val name: String = row.name
        val number: Double = row.chapter_number
    }
}
