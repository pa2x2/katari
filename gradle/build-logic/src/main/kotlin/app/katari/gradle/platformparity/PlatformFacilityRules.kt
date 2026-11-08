package app.katari.gradle.platformparity

/*
 * A platform may answer a shared contract with a class of its own only where the contract is a platform facility:
 * an operating-system or platform service, a viewer or player until desktop has one, or a fact of the platform's own
 * history. Facilities carry `@PlatformFacility`, and a platform's stand-in for one reports itself not offered through
 * the contract. Everywhere else both platforms bind the shared implementation, which answers from the facilities.
 *
 * B1: a class a platform's graph hands out, contributed, bound or built by a binding in a platform-only source set,
 * implements only the shared interfaces that are facilities. The one other answer it may give is as an element of a
 * multibinding, such as an entry-type registry or a set of engines, when its file is bound to its platform
 * ([PlatformBinding]): a bound element adds what only that platform can do beside the other platform's elements, and
 * stands in for no shared answer. A neutral element is still reported, and A1 reports a neutral Android one, so an
 * element that stops needing its platform moves to shared code. A class a single binding also answers the contract
 * with is judged as that binding. Exempt are `actual` declarations and the interfaces a `@DependencyGraph` extends.
 *
 * B2: no shared class implements, and no shared binding binds, a facility. A shared default would let a graph go
 * without the platform's own implementation and still compile. Test fixtures' fakes ship in neither app, so only the
 * modules the apps ship count.
 */

internal fun checkPlatformFacilities(
    sources: List<PlatformSource>,
    buildFiles: List<PlatformBuildFile>,
    apis: PlatformApis,
): List<PlatformFacilityFinding> {
    val codebase = PlatformCodebase(sources, buildFiles, apis)
    return (platformStandInFindings(codebase) + sharedDefaultFindings(codebase))
        .sortedWith(compareBy({ it.relativePath }, { it.lineNumber }))
}

private fun platformStandInFindings(codebase: PlatformCodebase): List<PlatformFacilityFinding> {
    val exempt = graphSupertypes(codebase)
    val handedOut = graphAnsweredTypes(codebase)
    val binding = PlatformBinding(codebase)
    val answers = graphAnswers(codebase)
    return codebase.files
        .filter { it.platform != null && it.sourceSet.module.path in codebase.appModules }
        .flatMap { file ->
            val bindingBodies = file.bindings.map { it.body }
            file.types
                .filter { it.kind != TypeKind.INTERFACE && "actual" !in it.modifiers }
                .filter { type ->
                    type.qualifiedName?.let { it in handedOut } ?: bindingBodies.any { type.offset in it }
                }
                .flatMap { type ->
                    codebase.sharedSupertypes(file, type)
                        .filter { (_, declaration) -> declaration.kind == TypeKind.INTERFACE }
                        .mapNotNull { it.second.qualifiedName }
                        .distinct()
                        .filter { it !in codebase.facilities && it !in exempt }
                        .filterNot { contract ->
                            binding.isBound(file) && answers.isOnlyElementOf(type.qualifiedName, contract)
                        }
                        .map { contract ->
                            PlatformFacilityFinding(
                                relativePath = file.relativePath,
                                lineNumber = file.lineNumberAt(type.offset),
                                reason = "${type.name ?: "An object"} answers $contract for " +
                                    "${file.platform!!.label} only, but $contract isn't a @PlatformFacility; " +
                                    "bind its shared implementation instead",
                            )
                        }
                }
        }
}

/** The platform classes a graph hands out: contributed, or named by a platform binding's receiver, inputs or body. */
private fun graphAnsweredTypes(codebase: PlatformCodebase): Set<String> {
    return codebase.files.filter { it.platform != null }.flatMap { file ->
        val contributed = file.types
            .filter { type -> type.annotations.any { it in CONTRIBUTIONS } }
            .mapNotNull { it.qualifiedName }
        val bound = file.bindings
            .filter { binding -> binding.annotations.any { it in BINDINGS } }
            .flatMap { it.inputTypes + it.bodyNames }
            .flatMap { name -> codebase.resolveTypes(file, name).mapNotNull { it.second.qualifiedName } }
        contributed + bound
    }.toSet()
}

/**
 * How platform graphs answer with each class: [elements] are the classes contributed into, or named by a binding of,
 * a set or map; [singles] are the contracts a single binding naming a class binds, and every supertype of a class
 * contributed with `@ContributesBinding`.
 */
private class GraphAnswers(val elements: Set<String>, val singles: Map<String, Set<String>>) {
    fun isOnlyElementOf(type: String?, contract: String): Boolean =
        type != null && type in elements && contract !in singles[type].orEmpty()
}

private fun graphAnswers(codebase: PlatformCodebase): GraphAnswers {
    val elements = mutableSetOf<String>()
    val singles = mutableMapOf<String, MutableSet<String>>()
    fun PlatformFile.resolve(names: Iterable<String>) =
        names.flatMap { codebase.resolveTypes(this, it) }.mapNotNull { it.second.qualifiedName }
    codebase.files.filter { it.platform != null }.forEach { file ->
        file.types.forEach { type ->
            val name = type.qualifiedName ?: return@forEach
            if (type.annotations.any { it in MULTIBINDING_CONTRIBUTIONS }) elements += name
            if ("ContributesBinding" in type.annotations) {
                singles.getOrPut(name, ::mutableSetOf) += codebase.sharedSupertypes(file, type).mapNotNull {
                    it.second.qualifiedName
                }
            }
        }
        file.bindings.filter { binding -> binding.annotations.any { it in BINDINGS } }.forEach { binding ->
            val named = file.resolve(binding.inputTypes + binding.bodyNames)
            if (binding.annotations.any { it in MULTIBINDING_ANNOTATIONS }) {
                elements += named
            } else {
                val bound = file.resolve(binding.boundTypes)
                named.forEach { singles.getOrPut(it, ::mutableSetOf) += bound }
            }
        }
    }
    return GraphAnswers(elements, singles)
}

private fun sharedDefaultFindings(codebase: PlatformCodebase): List<PlatformFacilityFinding> {
    val sharedFiles = codebase.files.filter { it.platform == null && it.sourceSet.module.path in codebase.appModules }
    return sharedFiles.flatMap { file ->
        val implementations = file.types
            .filter { it.kind != TypeKind.INTERFACE && "expect" !in it.modifiers }
            .flatMap { type ->
                facilitiesNamed(codebase, file, type.superTypes).map { facility ->
                    PlatformFacilityFinding(
                        relativePath = file.relativePath,
                        lineNumber = file.lineNumberAt(type.offset),
                        reason = "${type.name ?: "An object"} implements the @PlatformFacility $facility in shared " +
                            "code; each platform implements a facility itself",
                    )
                }
            }
        val bindings = file.bindings
            .filter { binding -> binding.annotations.any { it == "Binds" || it == "Provides" } }
            .flatMap { binding ->
                facilitiesNamed(codebase, file, binding.boundTypes).map { facility ->
                    PlatformFacilityFinding(
                        relativePath = file.relativePath,
                        lineNumber = file.lineNumberAt(binding.offset),
                        reason = "a shared binding binds the @PlatformFacility $facility; bind it in each " +
                            "platform's graph",
                    )
                }
            }
        implementations + bindings
    }
}

private fun facilitiesNamed(codebase: PlatformCodebase, file: PlatformFile, names: List<String>): List<String> =
    names.flatMap { codebase.resolveTypes(file, it) }
        .mapNotNull { it.second.qualifiedName }
        .filter { it in codebase.facilities }
        .distinct()

/** The shared interfaces a `@DependencyGraph` extends, directly or through each other. */
private fun graphSupertypes(codebase: PlatformCodebase): Set<String> {
    val found = mutableSetOf<String>()
    val pending = ArrayDeque(
        codebase.files.flatMap { file ->
            file.types.filter { "DependencyGraph" in it.annotations }.map { file to it }
        },
    )
    while (pending.isNotEmpty()) {
        val (file, type) = pending.removeFirst()
        type.superTypes.flatMap { codebase.resolveTypes(file, it) }.forEach { resolved ->
            val name = resolved.second.qualifiedName ?: return@forEach
            if (found.add(name)) pending += resolved
        }
    }
    return found
}

private val MULTIBINDING_CONTRIBUTIONS = setOf("ContributesIntoSet", "ContributesIntoMap")

private val CONTRIBUTIONS = setOf("ContributesBinding") + MULTIBINDING_CONTRIBUTIONS

private val MULTIBINDING_ANNOTATIONS = setOf("IntoSet", "IntoMap", "ElementsIntoSet")

private val BINDINGS = setOf("Binds", "Provides")
