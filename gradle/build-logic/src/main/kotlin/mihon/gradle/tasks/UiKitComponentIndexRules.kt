package mihon.gradle.tasks

/*
 * Agents find kit components through the index in the ui-kit skill, by the job they do: a component missing from it
 * gets built again under another name somewhere else. So the index must name every public top-level composable of
 * app.katari.presentation.kit, and its Component column may name nothing else, which keeps renamed and deleted components
 * out of it. Scope members, such as PageSpreadScope.Pages, are found through the component that provides the scope.
 */

internal const val UI_KIT_COMPONENT_INDEX = ".agents/skills/ui-kit/components.md"

internal fun checkUiKitComponentIndex(
    kotlinSources: List<UiKitBoundarySource>,
    index: UiKitBoundarySource?,
): List<UiKitBoundaryFinding> {
    val kitComposables = kotlinSources
        .filter { source -> source.relativePath.isKitSource() }
        .flatMap { source -> source.publicTopLevelComposables() }
    if (index == null) {
        return listOf(
            UiKitBoundaryFinding(
                UI_KIT_COMPONENT_INDEX,
                null,
                "The kit's components index is missing; it lists every public kit composable by the job it does",
            ),
        )
    }
    val indexed = index.indexedComponents()
    val kitNames = kitComposables.map { it.name }.toSet()
    val indexedNames = indexed.map { it.name }.toSet()
    val unindexed = kitComposables
        .filter { it.name !in indexedNames }
        .distinctBy { it.name }
        .map { composable ->
            UiKitBoundaryFinding(
                composable.relativePath,
                composable.lineNumber,
                "${composable.name} is missing from the kit's components index; add a row for its job to " +
                    UI_KIT_COMPONENT_INDEX,
            )
        }
    val stale = indexed
        .filter { it.name !in kitNames }
        .map { entry ->
            UiKitBoundaryFinding(
                index.relativePath,
                entry.lineNumber,
                "${entry.name} is not a public kit composable; the index's Component column names only those",
            )
        }
    return unindexed + stale
}

private class IndexedName(val name: String, val relativePath: String, val lineNumber: Int)

private fun UiKitBoundarySource.publicTopLevelComposables(): List<IndexedName> {
    val lines = content.withoutCommentsAndStrings().lines()
    return lines.mapIndexedNotNull { index, line ->
        val name = TOP_LEVEL_FUNCTION.find(line)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
        if (!lines.annotationsAbove(index).contains(COMPOSABLE)) return@mapIndexedNotNull null
        IndexedName(name, relativePath, index + 1)
    }
}

// The annotations directly above a declaration, including the continuation lines of multi-line ones
private fun List<String>.annotationsAbove(declaration: Int): String {
    var start = declaration
    while (start > 0) {
        val previous = this[start - 1]
        if (previous.isBlank()) break
        val continuesAnnotations = previous.startsWith("@") || previous.startsWith(")") || previous[0].isWhitespace()
        if (!continuesAnnotations) break
        start--
    }
    return subList(start, declaration).joinToString("\n")
}

private fun UiKitBoundarySource.indexedComponents(): List<IndexedName> {
    return content.lines().flatMapIndexed { index, line ->
        val cells = line.split("|")
        // A table row is "| Job | Component | Notes |"; its header row names the columns
        if (!line.startsWith("|") || cells.size < 4 || cells[2].trim() == "Component") return@flatMapIndexed emptyList()
        INDEXED_NAME.findAll(cells[2]).map { IndexedName(it.groupValues[1], relativePath, index + 1) }.toList()
    }
}

private fun String.isKitSource(): Boolean {
    val sourceSet = removePrefix("core/presentation-core/src/").substringBefore("/")
    return startsWith("core/presentation-core/src/") &&
        !sourceSet.contains("test", ignoreCase = true) &&
        contains("/kotlin/app/katari/presentation/kit/")
}

private val TOP_LEVEL_FUNCTION =
    Regex("""^(?:(?:public|actual|expect|inline)\s+)*fun\s+(?:<[^>]*>\s+)?(?:[\w.]+\.)?(\w+)\s*\(""")
private val COMPOSABLE = Regex("""@Composable\b""")
private val INDEXED_NAME = Regex("""`(?:[\w.]+\.)?(\w+)`""")
