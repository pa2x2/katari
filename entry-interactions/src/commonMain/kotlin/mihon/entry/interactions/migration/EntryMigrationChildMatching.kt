package mihon.entry.interactions.migration

import mihon.entry.interactions.migration.host.EntryMigrationHostChildUpdate
import tachiyomi.domain.entry.migration.MigrationChapterMatch
import tachiyomi.domain.entry.migration.matchMigrationChapters
import tachiyomi.domain.entry.model.EntryChapter

internal fun prepareMigrationChildUpdates(
    sourceChildren: List<EntryChapter>,
    childMatches: List<EntryMigrationChildMatch>,
    transferConsumption: Boolean,
    transferBookmarks: Boolean,
): List<EntryMigrationHostChildUpdate> {
    val maxConsumedNumber = sourceChildren
        .filter(EntryChapter::read)
        .mapNotNull { child -> child.chapterNumber.takeIf { it >= 0.0 } }
        .maxOrNull()

    return childMatches.mapNotNull { match ->
        val source = match.source
        val target = match.target
        var read = target.read
        var bookmark = target.bookmark
        var dateFetch = target.dateFetch
        if (source != null) {
            if (transferConsumption) read = source.read
            if (transferBookmarks) bookmark = source.bookmark
            dateFetch = source.dateFetch
        }
        if (
            transferConsumption &&
            maxConsumedNumber != null &&
            target.chapterNumber >= 0.0 &&
            target.chapterNumber <= maxConsumedNumber
        ) {
            read = true
        }
        val updated = target.copy(read = read, bookmark = bookmark, dateFetch = dateFetch)
        updated.takeIf { it != target }?.let { EntryMigrationHostChildUpdate(target, it) }
    }
}

internal typealias EntryMigrationChildMatch = MigrationChapterMatch<EntryChapter>

internal fun matchMigrationChildren(
    sourceChildren: List<EntryChapter>,
    targetChildren: List<EntryChapter>,
): List<EntryMigrationChildMatch> =
    matchMigrationChapters(sourceChildren, targetChildren, EntryChapter::chapterNumber, EntryChapter::name)
