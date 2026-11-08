package mihon.entry.interactions.migration

import mihon.entry.interactions.migration.host.EntryMigrationHostChildUpdate
import tachiyomi.domain.entry.migration.MigrationChapterMarks
import tachiyomi.domain.entry.migration.MigrationChapterMatch
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.migration.matchMigrationChapters
import tachiyomi.domain.entry.migration.migratedChapterMarks
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedInSourceOrder

internal typealias EntryMigrationChildMatch = MigrationChapterMatch<EntryChapter>

/** How a migration pairs the migrated entry's chapters with the target's. */
internal sealed interface EntryMigrationChildPairing {
    fun pair(sourceChildren: List<EntryChapter>, targetChildren: List<EntryChapter>): EntryMigrationChildPairs

    /** A target chapter takes the source chapter of its number, or of its name when it has none. */
    data object ByNumber : EntryMigrationChildPairing {
        override fun pair(
            sourceChildren: List<EntryChapter>,
            targetChildren: List<EntryChapter>,
        ) = EntryMigrationChildPairs(
            matches = matchMigrationChapters(
                sourceChildren,
                targetChildren,
                EntryChapter::chapterNumber,
                EntryChapter::name,
            ),
            number = EntryChapter::chapterNumber,
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
        ): EntryMigrationChildPairs {
            val sources = sourceChildren.sortedInSourceOrder()
            val targets = targetChildren.sortedInSourceOrder().drop(offset).take(sources.size)
            val positions = (sources.withIndex() + targets.withIndex())
                .associate { (index, chapter) -> chapter.id to index.toDouble() }
            return EntryMigrationChildPairs(
                matches = sources.zip(targets) { source, target -> MigrationChapterMatch(source, target) },
                number = { positions[it.id] ?: -1.0 },
            )
        }
    }
}

/**
 * The target chapters a migration changes, each with the source chapter whose state it takes, and where [number]
 * places a chapter for the rule that every target chapter up to the last source chapter read counts as read.
 */
internal class EntryMigrationChildPairs(
    val matches: List<EntryMigrationChildMatch>,
    val number: (EntryChapter) -> Double,
)

internal fun prepareMigrationChildUpdates(
    sourceChildren: List<EntryChapter>,
    childPairs: EntryMigrationChildPairs,
    transfer: MigrationChildStateTransfer,
): List<EntryMigrationHostChildUpdate> {
    val marks = migratedChapterMarks(
        sources = sourceChildren,
        matches = childPairs.matches,
        transfer = transfer,
        marks = { MigrationChapterMarks(it.read, it.bookmark) },
        number = childPairs.number,
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
