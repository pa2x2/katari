package mihon.entry.interactions.download

/**
 * The names a source's download folder is written and found under.
 *
 * A source's [current] folder name includes its language, so the same-named sources of a multi-language extension
 * each get a folder of their own. Folders written before languages were added have the [legacy] name, which is still
 * read after [current], and [legacySourceDirRenames] picks the legacy folders that can become current ones.
 *
 * @property legacy `null` when it is the same as [current], for a source without a language.
 */
data class SourceDirNames(val current: String, val legacy: String?) {

    /** [current] first. */
    val all: List<String> get() = listOfNotNull(current, legacy)
}

/**
 * Returns the legacy folders among [folderNames] to rename, each to its current name.
 *
 * A legacy folder is renamed only when it is a name of just one of [sources], so it holds the downloads of that source
 * alone, and that source has no current folder yet, which renaming would collide with. Every other legacy folder
 * stays, read by each source it is a name of. Names match ignoring case, as storage access framework folders do.
 */
fun legacySourceDirRenames(
    folderNames: Collection<String>,
    sources: Collection<SourceDirNames>,
): Map<String, String> {
    val existing = folderNames.mapTo(HashSet()) { it.lowercase() }
    val claims = sources.distinct()
        .flatMap { names -> names.all.map { it.lowercase() } }
        .groupingBy { it }
        .eachCount()
    return sources.distinct()
        .mapNotNull { names ->
            val legacy = names.legacy ?: return@mapNotNull null
            val folder = folderNames.firstOrNull { it.equals(legacy, ignoreCase = true) } ?: return@mapNotNull null
            if (names.current.lowercase() in existing || claims[legacy.lowercase()] != 1) return@mapNotNull null
            folder to names.current
        }
        .toMap()
}
