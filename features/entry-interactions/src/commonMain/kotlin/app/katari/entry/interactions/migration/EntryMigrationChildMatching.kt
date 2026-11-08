package app.katari.entry.interactions.migration

import app.katari.entry.interactions.migration.host.EntryMigrationHostChildUpdate
import tachiyomi.domain.entry.migration.MigrationChapterMarks
import tachiyomi.domain.entry.migration.MigrationChapterMatch
import tachiyomi.domain.entry.migration.MigrationChapterPairs
import tachiyomi.domain.entry.migration.MigrationChapterPosition
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.migration.migratedChapterMarks
import tachiyomi.domain.entry.migration.pairMigrationChaptersByNumber
import tachiyomi.domain.entry.migration.pairMigrationChaptersInOrder
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedInSourceOrder

internal typealias EntryMigrationChildMatch = MigrationChapterMatch<EntryChapter>

/** How a migration pairs the migrated entry's chapters with the target's. */
internal sealed interface EntryMigrationChildPairing {
    fun pair(
        sourceChildren: List<EntryChapter>,
        targetChildren: List<EntryChapter>,
    ): MigrationChapterPairs<EntryChapter>

    /** A target chapter takes the source chapter of its number, or of its name when it has none. */
    data object ByNumber : EntryMigrationChildPairing {
        override fun pair(sourceChildren: List<EntryChapter>, targetChildren: List<EntryChapter>) =
            pairMigrationChaptersByNumber(
                sourceChildren,
                targetChildren,
                EntryChapter::chapterNumber,
                EntryChapter::name,
            )
    }

    /**
     * The source's chapters take the target's from [offset] on, one for one, both in the order their sources list them:
     * a merged entry's seasons, which number their episodes from one each, landing on an entry that lists them all. How
     * the person sorts the list to view it doesn't change which episode stands for which.
     */
    data class ByPosition(val offset: Int) : EntryMigrationChildPairing {
        override fun pair(
            sourceChildren: List<EntryChapter>,
            targetChildren: List<EntryChapter>,
        ): MigrationChapterPairs<EntryChapter> {
            val targets = targetChildren.sortedInSourceOrder().drop(offset)
            return pairMigrationChaptersInOrder(
                sourceChildren.sortedInSourceOrder().mapIndexed { index, source ->
                    MigrationChapterPosition(source, targets.getOrNull(index))
                },
            )
        }
    }
}

internal fun prepareMigrationChildUpdates(
    sourceChildren: List<EntryChapter>,
    childPairs: MigrationChapterPairs<EntryChapter>,
    transfer: MigrationChildStateTransfer,
): List<EntryMigrationHostChildUpdate> {
    val marks = migratedChapterMarks(
        sources = sourceChildren,
        pairs = childPairs,
        transfer = transfer,
        marks = { MigrationChapterMarks(it.read, it.bookmark) },
    ).toMap()
    return childPairs.matches.mapNotNull { match ->
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
