package tachiyomi.domain.entry.migration

/** A chapter of the entry migrated to, with the chapter of the migrated entry whose state it takes, if any. */
data class MigrationChapterMatch<C>(
    val source: C?,
    val target: C,
)

/**
 * Pairs each of [targets] with the chapter of [sources] it stands for, as migrating an entry carries chapter state
 * over: a target with a chapter number takes the first source with the same number (zero and minus zero alike); one
 * without takes the first source of the same name.
 */
fun <C> matchMigrationChapters(
    sources: List<C>,
    targets: List<C>,
    number: (C) -> Double,
    name: (C) -> String,
): List<MigrationChapterMatch<C>> {
    val sourceByNumber = buildMap {
        sources.forEach { source ->
            if (number(source) >= 0.0) putIfAbsent(number(source).normalized(), source)
        }
    }
    val sourceByName = buildMap {
        sources.forEach { source -> putIfAbsent(name(source), source) }
    }
    return targets.map { target ->
        val source = if (number(target) >= 0.0) {
            sourceByNumber[number(target).normalized()]
        } else {
            sourceByName[name(target)]
        }
        MigrationChapterMatch(source = source, target = target)
    }
}

private fun Double.normalized(): Double = if (this == 0.0) 0.0 else this
