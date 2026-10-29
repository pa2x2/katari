package mihon.gradle.platformparity

import mihon.gradle.tasks.withoutCommentsAndStrings

/*
 * Android and desktop each build the app from their own dependency graph and start it from their own host. Shared code
 * reaches both graphs through Metro contributions, except the binding containers a graph lists in `bindingContainers`
 * itself, such as the database and the Entry hosts, which validation graphs replace. A shared container one graph lists
 * and the other doesn't leaves that platform to answer its contracts with a copy of its own, or not at all, so every
 * container declared in a shared source set and listed by one production graph has to be listed by the other. For the
 * same reason a production graph excludes no contribution: the shared features answer for what a platform lacks from
 * the facilities it binds, so excluding one only makes room for a stand-in copy.
 *
 * Start-up and launch work written once reaches a platform only when its host calls it. Every function marked
 * `@HostEntryPoint` in a shared source set has to be called from both hosts' production code, so a host can't leave
 * one out, as the desktop host once left out the extension update check and the start-up warmups.
 *
 * What every composition of the app's screens is given is provided once, by `AppUiRoot`, which both hosts call. A host
 * providing one of its locals itself, around it or inside it, is how the hosts drifted apart before it existed, so no
 * host provides a local `AppUiRoot` provides. Compose's own link and clipboard locals default to platform handlers
 * that bypass the app's link opener and clipboard, so nothing else provides them either.
 */

internal data class HostParitySource(val relativePath: String, val content: String)

internal data class HostParityFinding(val relativePath: String, val lineNumber: Int, val reason: String)

internal fun checkHostParity(sources: List<HostParitySource>): List<HostParityFinding> {
    val productionSources = sources.filterNot { it.relativePath.isTestSource() }
    return (
        bindingContainerFindings(productionSources) +
            excludeFindings(productionSources) +
            hostEntryPointFindings(productionSources) +
            compositionRootFindings(productionSources)
        )
        .sortedWith(compareBy({ it.relativePath }, { it.lineNumber }))
}

private fun bindingContainerFindings(productionSources: List<HostParitySource>): List<HostParityFinding> {
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
    }
}

private fun excludeFindings(productionSources: List<HostParitySource>): List<HostParityFinding> {
    return productionSources
        .filter { source -> GRAPH_MODULES.keys.any { source.relativePath.startsWith("$it/src/main/") } }
        .flatMap { source ->
            val code = source.content.withoutCommentsAndStrings()
            val platform = GRAPH_MODULES.getValue(source.relativePath.substringBefore('/'))
            DEPENDENCY_GRAPH.findAll(code).mapNotNull { graph ->
                val arguments = code.bracketed(graph.range.last, '(', ')')
                val excludes = EXCLUDES.find(code.substring(arguments)) ?: return@mapNotNull null
                val list = code.bracketed(arguments.first + excludes.range.last, '[', ']')
                val excluded = CLASS_LITERAL.findAll(code.substring(list)).map { it.groupValues[1] }.toList()
                if (excluded.isEmpty()) return@mapNotNull null
                HostParityFinding(
                    relativePath = source.relativePath,
                    lineNumber = code.lineNumberAt(arguments.first + excludes.range.first),
                    reason = "the $platform graph excludes ${excluded.joinToString()}; production graphs exclude " +
                        "no contribution, so bind the platform facilities the shared bindings need instead",
                )
            }
        }
}

private fun hostEntryPointFindings(productionSources: List<HostParitySource>): List<HostParityFinding> {
    val hostCode = GRAPH_MODULES.mapValues { (module, _) ->
        productionSources
            .filter { it.relativePath.startsWith("$module/src/main/") }
            .map { it.content.withoutCommentsAndStrings() }
    }
    return productionSources
        .filter { SHARED_SOURCE_SET in it.relativePath }
        .flatMap { source ->
            val code = source.content.withoutCommentsAndStrings()
            HOST_ENTRY_POINT.findAll(code).flatMap { match ->
                val name = match.groupValues[1]
                val reference = Regex("""\b${Regex.escape(name)}\b""")
                hostCode.filterValues { files -> files.none(reference::containsMatchIn) }.keys.map { module ->
                    HostParityFinding(
                        relativePath = source.relativePath,
                        lineNumber = code.lineNumberAt(match.range.first),
                        reason = "$name is a @HostEntryPoint, but the ${GRAPH_MODULES.getValue(module)} host " +
                            "never calls it; call it from $module/src/main",
                    )
                }
            }
        }
}

private fun compositionRootFindings(productionSources: List<HostParitySource>): List<HostParityFinding> {
    val roots = productionSources.filter { source ->
        SHARED_SOURCE_SET in source.relativePath &&
            COMPOSITION_ROOT.containsMatchIn(source.content.withoutCommentsAndStrings())
    }
    val rootLocals = roots
        .flatMap { root -> PROVIDED_LOCAL.findAll(root.content.withoutCommentsAndStrings()).map { it.groupValues[1] } }
        .toSet()
    val rootPaths = roots.map { it.relativePath }.toSet()
    return productionSources
        .filter { it.relativePath !in rootPaths }
        .flatMap { source ->
            val isHost = GRAPH_MODULES.keys.any { source.relativePath.startsWith("$it/src/main/") }
            val code = source.content.withoutCommentsAndStrings()
            PROVIDED_LOCAL.findAll(code).mapNotNull { match ->
                val local = match.groupValues[1]
                val reason = when {
                    isHost && local in rootLocals ->
                        "$local is provided by AppUiRoot; a host provides only what its platform alone has"
                    local in PLATFORM_HANDLER_LOCALS ->
                        "$local defaults to a platform handler that bypasses the app's own; only AppUiRoot may " +
                            "provide it"
                    else -> return@mapNotNull null
                }
                HostParityFinding(source.relativePath, code.lineNumberAt(match.range.first), reason)
            }
        }
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

private val EXCLUDES = Regex("""\bexcludes\s*=\s*(?=\[)""")

private val BINDING_CONTAINERS = Regex("""\bbindingContainers\s*=\s*(?=\[)""")

/** A function marked `@HostEntryPoint`, with its name; other annotations and modifiers may stand in between. */
private val HOST_ENTRY_POINT = Regex(
    """@(?:[\w.]+\.)?HostEntryPoint\b(?:\s+@[\w.]+(?:\([^)]*\))?|\s+[a-z]+)*?\s+fun\s+""" +
        """(?:<[^>]*>\s*)?(?:[\w.]+(?:<[^>]*>)?\.)?(\w+)\s*\(""",
)

/** The declaration of the composition root every host calls around the app's screens. */
private val COMPOSITION_ROOT = Regex("""\bfun\s+AppUiRoot\s*\(""")

/** A composition local given a value, as `CompositionLocalProvider` takes it: `LocalX provides value`. */
private val PROVIDED_LOCAL = Regex("""\b(\w+)\s+provides(?:Default|Computed)?\b""")

/** Compose's locals whose platform defaults open links and use the clipboard past `LinkOpener` and `TextClipboard`. */
private val PLATFORM_HANDLER_LOCALS = setOf("LocalUriHandler", "LocalClipboardManager", "LocalClipboard")

private val CLASS_LITERAL = Regex("""([\w.]+)\s*::\s*class""")

private val TOP_LEVEL_DECLARATION = Regex(
    """(?m)^(?:(?:public|internal|private|abstract|open|sealed|data|fun|enum|annotation|value|expect|actual)\s+)*""" +
        """(?:class|interface|object)\s+(\w+)""",
)
