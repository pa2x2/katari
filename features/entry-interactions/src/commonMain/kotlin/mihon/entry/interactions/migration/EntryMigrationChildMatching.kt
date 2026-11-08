package mihon.entry.interactions.migration

import mihon.entry.interactions.migration.host.EntryMigrationHostChildUpdate
import tachiyomi.domain.entry.migration.MigrationChapterMarks
import tachiyomi.domain.entry.migration.MigrationChapterMatch
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.migration.matchMigrationChapters
import tachiyomi.domain.entry.migration.migratedChapterMarks
import tachiyomi.domain.entry.model.EntryChapter

internal fun prepareMigrationChildUpdates(
    sourceChildren: List<EntryChapter>,
    childMatches: List<EntryMigrationChildMatch>,
    transfer: MigrationChildStateTransfer,
): List<EntryMigrationHostChildUpdate> {
    val marks = migratedChapterMarks(
        sources = sourceChildren,
        matches = childMatches,
        transfer = transfer,
        marks = { MigrationChapterMarks(it.read, it.bookmark) },
        number = EntryChapter::chapterNumber,
    ).toMap()
    return childMatches.mapNotNull { match ->
        val target = match.target
        val carried = marks[target]
        val updated = target.copy(
            read = carried?.read ?: target.read,
            bookmark = carried?.bookmark ?: target.bookmark,
            dateFetch = match.source?.dateFetch ?: target.dateFetch,
        )
        updated.takeIf { it != target }?.let { EntryMigrationHostChildUpdate(target, it) }
    }
}

internal typealias EntryMigrationChildMatch = MigrationChapterMatch<EntryChapter>

internal fun matchMigrationChildren(
    sourceChildren: List<EntryChapter>,
    targetChildren: List<EntryChapter>,
): List<EntryMigrationChildMatch> =
    matchMigrationChapters(sourceChildren, targetChildren, EntryChapter::chapterNumber, EntryChapter::name)
