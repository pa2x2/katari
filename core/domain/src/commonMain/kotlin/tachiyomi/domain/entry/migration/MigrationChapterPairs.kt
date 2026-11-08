package tachiyomi.domain.entry.migration

/**
 * How a migration paired the migrated entry's chapters with the target's: the target chapters it changes, each with the
 * source chapter whose state it takes, and where [number] places a chapter for the rule that every target chapter up to
 * the last source chapter read counts as read ([migratedChapterMarks]). A chapter [number] can't place is at -1.
 *
 * Every device carrying later edits of a migrated entry over pairs its chapters the same way, so it is built only here.
 */
class MigrationChapterPairs<C>(
    val matches: List<MigrationChapterMatch<C>>,
    val number: (C) -> Double,
)

/** One place in the order a migration paired chapters in: a source chapter and the target chapter beside it. */
data class MigrationChapterPosition<C>(
    /** Null where this device doesn't hold the source chapter. */
    val source: C?,
    /** Null where the target had no chapter left for the source chapter, or this device doesn't hold it. */
    val target: C?,
)

/**
 * Each of [targets] takes the source chapter of its number, or of its name when it has none ([matchMigrationChapters]).
 */
fun <C> pairMigrationChaptersByNumber(
    sources: List<C>,
    targets: List<C>,
    number: (C) -> Double,
    name: (C) -> String,
): MigrationChapterPairs<C> = MigrationChapterPairs(matchMigrationChapters(sources, targets, number, name), number)

/**
 * Each target of [order] takes the source beside it, and both stand at their index in [order], so a source chapter read
 * reads every target chapter before its own place, as it does in a list numbered one through.
 */
fun <C> pairMigrationChaptersInOrder(order: List<MigrationChapterPosition<C>>): MigrationChapterPairs<C> {
    val places = HashMap<C, Double>()
    order.forEachIndexed { index, (source, target) ->
        source?.let { places[it] = index.toDouble() }
        target?.let { places[it] = index.toDouble() }
    }
    return MigrationChapterPairs(
        matches = order.mapNotNull { (source, target) -> target?.let { MigrationChapterMatch(source, it) } },
        number = { places[it] ?: -1.0 },
    )
}
