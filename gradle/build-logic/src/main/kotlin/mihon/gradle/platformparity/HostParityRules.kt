package mihon.gradle.platformparity

import mihon.gradle.tasks.withoutCommentsAndStrings

/*
 * Android and desktop each build the app from their own dependency graph. Shared code reaches both through Metro
 * contributions, except the binding containers a graph lists in `bindingContainers` itself, such as the database and
 * the Entry hosts, which validation graphs replace. A shared container one graph lists and the other doesn't leaves that
 * platform to answer its contracts with a copy of its own, or not at all, so every container declared in a shared
 * source set and listed by one production graph has to be listed by the other.
 */

internal data class HostParitySource(val relativePath: String, val content: String)

internal data class HostParityFinding(val relativePath: String, val lineNumber: Int, val reason: String)

internal fun checkHostParity(sources: List<HostParitySource>): List<HostParityFinding> {
    val productionSources = sources.filterNot { it.relativePath.isTestSource() }
    val sharedDeclarations = productionSources
        .filter { SHARED_SOURCE_SET in it.relativePath }
        .flatMap { source -> source.content.withoutCommentsAndStrings().topLevelDeclarations() }
        .toSet()
    val listingsByGraph = GRAPH_MODULES.mapValues { (module, _) ->
        productionSources
            .filter { it.relativePath.startsWith("$module/src/main/") }
            .flatMap(::bindingContainerListings)
            .filter { it.qualifiedName in sharedDeclarations }
            .associateBy { it.qualifiedName }
    }
    return listingsByGraph.flatMap { (module, listings) ->
        val others = listingsByGraph.filterKeys { it != module }
        listings.values.flatMap { listing ->
            others.filterValues { listing.qualifiedName !in it }.keys.map { otherModule ->
                HostParityFinding(
                    relativePath = listing.relativePath,
                    lineNumber = listing.lineNumber,
                    reason = "${listing.qualifiedName} is declared in shared code and listed in the " +
                        "${GRAPH_MODULES.getValue(module)} graph's bindingContainers but not in the " +
                        "${GRAPH_MODULES.getValue(otherModule)} graph's; list it in both, or contribute it",
                )
            }
        }
    }.sortedWith(compareBy({ it.relativePath }, { it.lineNumber }))
}

/** The production graphs, by the module that declares each, with the platform it builds the app for. */
private val GRAPH_MODULES = mapOf("app" to "Android", "desktop" to "desktop")

private const val SHARED_SOURCE_SET = "/src/commonMain/"

private fun String.isTestSource(): Boolean =
    substringAfter("/src/", missingDelimiterValue = "").substringBefore("/").contains("test", ignoreCase = true)

private class BindingContainerListing(val qualifiedName: String, val relativePath: String, val lineNumber: Int)

/** The containers each `@DependencyGraph` in [source] lists in `bindingContainers`, by their qualified names. */
private fun bindingContainerListings(source: HostParitySource): List<BindingContainerListing> {
    val code = source.content.withoutCommentsAndStrings()
    val packageName = PACKAGE.find(code)?.groupValues?.get(1).orEmpty()
    val imports = IMPORT.findAll(code).associate { match ->
        val qualifiedName = match.groupValues[1]
        (match.groupValues[2].ifEmpty { qualifiedName.substringAfterLast('.') }) to qualifiedName
    }
    return DEPENDENCY_GRAPH.findAll(code).flatMap { graph ->
        val arguments = code.bracketed(graph.range.last, '(', ')')
        val list = BINDING_CONTAINERS.find(code.substring(arguments))
            ?.let { code.bracketed(arguments.first + it.range.last, '[', ']') }
            ?: return@flatMap emptySequence()
        CLASS_LITERAL.findAll(code.substring(list)).map { literal ->
            val name = literal.groupValues[1]
            val qualifiedName = when {
                '.' in name -> name
                name in imports -> imports.getValue(name)
                packageName.isEmpty() -> name
                else -> "$packageName.$name"
            }
            val offset = list.first + literal.range.first
            BindingContainerListing(qualifiedName, source.relativePath, code.lineNumberAt(offset))
        }
    }.toList()
}

/** The qualified names of the classes, interfaces and objects declared at the top level of [this] blanked code. */
private fun String.topLevelDeclarations(): List<String> {
    val packageName = PACKAGE.find(this)?.groupValues?.get(1)
    return TOP_LEVEL_DECLARATION.findAll(this)
        .map { match -> match.groupValues[1] }
        .map { name -> if (packageName == null) name else "$packageName.$name" }
        .toList()
}

/** The range strictly inside the [open] bracket at or after [from] and its matching [close]. */
private fun String.bracketed(from: Int, open: Char, close: Char): IntRange {
    val start = indexOf(open, from)
    if (start < 0) return IntRange.EMPTY
    var depth = 0
    for (index in start until length) {
        when (this[index]) {
            open -> depth++
            close -> if (--depth == 0) return start + 1 until index
        }
    }
    return start + 1 until length
}

private fun String.lineNumberAt(offset: Int): Int = 1 + (0 until offset).count { this[it] == '\n' }

private val PACKAGE = Regex("""(?m)^package\s+([\w.]+)""")

private val IMPORT = Regex("""(?m)^import\s+([\w.]+)(?:\s+as\s+(\w+))?""")

private val DEPENDENCY_GRAPH = Regex("""@(?:dev\.zacsweers\.metro\.)?DependencyGraph\s*(?=\()""")

private val BINDING_CONTAINERS = Regex("""\bbindingContainers\s*=\s*(?=\[)""")

private val CLASS_LITERAL = Regex("""([\w.]+)\s*::\s*class""")

private val TOP_LEVEL_DECLARATION = Regex(
    """(?m)^(?:(?:public|internal|private|abstract|open|sealed|data|fun|enum|annotation|value|expect|actual)\s+)*""" +
        """(?:class|interface|object)\s+(\w+)""",
)
