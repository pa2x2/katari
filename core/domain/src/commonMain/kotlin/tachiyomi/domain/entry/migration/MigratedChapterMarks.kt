package tachiyomi.domain.entry.migration

/** A chapter's read and bookmark marks. */
data class MigrationChapterMarks(val read: Boolean, val bookmark: Boolean)

/** Which chapter marks a migration that keeps chapter state carries, which depends on the entry's type. */
data class MigrationChildStateTransfer(val read: Boolean, val bookmarks: Boolean)

/**
 * The marks each target chapter of [matches] takes when a migration carries chapter state: a matched chapter takes
 * its source's marks that [transfer] carries, and with read marks carried every target
 * chapter numbered up to the highest read source chapter counts as read. Unchanged chapters are left out.
 */
fun <C> migratedChapterMarks(
    sources: List<C>,
    matches: List<MigrationChapterMatch<C>>,
    transfer: MigrationChildStateTransfer,
    marks: (C) -> MigrationChapterMarks,
    number: (C) -> Double,
): List<Pair<C, MigrationChapterMarks>> {
    val maxReadNumber = sources
        .filter { marks(it).read }
        .mapNotNull { chapter -> number(chapter).takeIf { it >= 0.0 } }
        .maxOrNull()
    return matches.mapNotNull { match ->
        val current = marks(match.target)
        var read = current.read
        var bookmark = current.bookmark
        match.source?.let(marks)?.let { source ->
            if (transfer.read) read = source.read
            if (transfer.bookmarks) bookmark = source.bookmark
        }
        val targetNumber = number(match.target)
        if (transfer.read && maxReadNumber != null && targetNumber >= 0.0 && targetNumber <= maxReadNumber) read = true
        MigrationChapterMarks(read, bookmark).takeIf { it != current }?.let { match.target to it }
    }
}
