package mihon.gradle.platformparity

/*
 * A2. A binding in a platform-only source set (a `@Binds` or `@Provides` declaration, including elements of
 * multibindings such as the entry-type registries, or a class contributed with `@ContributesBinding`,
 * `@ContributesIntoSet` or `@ContributesIntoMap`) belongs to that platform only when it names something shared code
 * can't: a declaration of the platform's own source sets, a name only the platform's libraries have, or a facility, a
 * type implementing one, or an `expect` declaration, which each platform supplies itself. It belongs there too when
 * only that platform's bindings provide one of its inputs, as the other graph couldn't answer it, or only that
 * platform's bindings take what it binds, as the other graph would carry it unused; those bindings are held to this
 * rule in turn. Otherwise both graphs can take the binding from a shared container, and keeping a copy per platform
 * is how the two drifted apart. Whether a platform declaration it names needs the platform is A1's and B1's question;
 * a shared container can't name it either way.
 */

internal fun bindingPlacementFindings(codebase: PlatformCodebase): List<PlatformPlacementFinding> {
    val providers = bindingSourceSets(codebase)
    val consumers = consumingSourceSets(codebase)
    return codebase.files
        .filter { it.platform != null && it.sourceSet.module.path in codebase.appModules }
        .flatMap { file ->
            val platform = file.platform!!.label
            val bindings = file.bindings
                .filter { declaration -> declaration.annotations.any { it in BINDING_ANNOTATIONS } }
                .filterNot { declaration ->
                    (declaration.inputTypes + declaration.boundTypes + declaration.bodyNames)
                        .any { codebase.namesPlatformOnly(file, it) } ||
                        declaration.inputTypes.any { codebase.isOnlyOn(file, it, providers) } ||
                        declaration.boundTypes.any { codebase.isOnlyOn(file, it, consumers) }
                }
                .map { declaration ->
                    PlatformPlacementFinding(
                        relativePath = file.relativePath,
                        lineNumber = file.lineNumberAt(declaration.offset),
                        reason = "this binding names nothing only $platform has; provide it from a shared container",
                    )
                }
            val contributions = file.types
                .filter { type -> type.annotations.any { it in CONTRIBUTION_ANNOTATIONS } }
                .filterNot { type ->
                    type.qualifiedName?.let(codebase::isPlatformSupplied) == true ||
                        (type.constructorTypes + type.superTypes + type.bodyNames)
                            .any { codebase.namesPlatformOnly(file, it) } ||
                        type.constructorTypes.any { codebase.isOnlyOn(file, it, providers) } ||
                        type.superTypes.any { codebase.isOnlyOn(file, it, consumers) }
                }
                .map { type ->
                    PlatformPlacementFinding(
                        relativePath = file.relativePath,
                        lineNumber = file.lineNumberAt(type.offset),
                        reason = "${type.name} is contributed from $platform code but names nothing only " +
                            "$platform has; move it to commonMain",
                    )
                }
            bindings + contributions
        }
}

/**
 * Whether [name], as [file] writes it, is something shared code can't name, or something each platform supplies. A
 * name the file imports counts when the other platform can't see it, which takes in what the platform generates, such
 * as Android's `BuildConfig`; a name it doesn't import counts only when its own platform's libraries have it.
 */
private fun PlatformCodebase.namesPlatformOnly(file: PlatformFile, name: String): Boolean {
    val platform = file.platform ?: return false
    val module = file.sourceSet.module
    val imported = file.isImported(name)
    return file.candidates(name).any { candidate ->
        val declaringFiles = declarations[candidate] ?: types[candidate]?.map { it.first }
        when {
            isPlatformSupplied(candidate) -> true
            declaringFiles != null -> declaringFiles.none { it.platform == null }
            !isVisible(candidate, platform.other, module) -> imported || isVisible(candidate, platform, module)
            else -> false
        }
    }
}

/** Whether [name], as [file] writes it, is a type that [sourceSets] has only on [file]'s platform. */
private fun PlatformCodebase.isOnlyOn(
    file: PlatformFile,
    name: String,
    sourceSets: Map<String, Set<Platform?>>,
): Boolean = resolveTypes(file, name).any { (_, type) ->
    sourceSets[type.qualifiedName].orEmpty().let { it.isNotEmpty() && it.all { platform -> platform == file.platform } }
}

/**
 * The platforms whose source sets bind each type, null standing for shared code: by `@Binds` or `@Provides`, by
 * contributing a class, or by an injectable constructor.
 */
private fun bindingSourceSets(codebase: PlatformCodebase): Map<String, Set<Platform?>> {
    val found = mutableMapOf<String, MutableSet<Platform?>>()
    fun add(file: PlatformFile, names: List<String>) = names.flatMap { codebase.resolveTypes(file, it) }
        .forEach { (_, type) -> found.getOrPut(type.qualifiedName ?: return@forEach, ::mutableSetOf) += file.platform }
    codebase.files.forEach { file ->
        file.bindings.filter { binding -> binding.annotations.any { it in BINDING_ANNOTATIONS } }
            .forEach { add(file, it.boundTypes) }
        file.types.forEach { type ->
            val name = type.qualifiedName ?: return@forEach
            if (type.annotations.any { it in CONTRIBUTION_ANNOTATIONS }) add(file, type.superTypes)
            if (type.annotations.any { it in CONTRIBUTION_ANNOTATIONS || it == "Inject" }) {
                found.getOrPut(name, ::mutableSetOf) += file.platform
            }
        }
    }
    return found
}

/**
 * The platforms whose source sets take each type, null standing for shared code: as an input of a binding, or in the
 * constructor of a class a graph builds.
 */
private fun consumingSourceSets(codebase: PlatformCodebase): Map<String, Set<Platform?>> {
    val found = mutableMapOf<String, MutableSet<Platform?>>()
    fun add(file: PlatformFile, names: List<String>) = names.flatMap { codebase.resolveTypes(file, it) }
        .forEach { (_, type) -> found.getOrPut(type.qualifiedName ?: return@forEach, ::mutableSetOf) += file.platform }
    codebase.files.forEach { file ->
        file.bindings.filter { binding -> binding.annotations.any { it in BINDING_ANNOTATIONS } }
            .forEach { add(file, it.inputTypes) }
        file.types.filter { type -> type.annotations.any { it in CONTRIBUTION_ANNOTATIONS || it == "Inject" } }
            .forEach { add(file, it.constructorTypes) }
    }
    return found
}

private val BINDING_ANNOTATIONS = setOf("Binds", "Provides")

private val CONTRIBUTION_ANNOTATIONS = setOf("ContributesBinding", "ContributesIntoSet", "ContributesIntoMap")
