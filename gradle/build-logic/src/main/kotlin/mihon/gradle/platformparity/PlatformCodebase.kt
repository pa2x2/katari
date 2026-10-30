package mihon.gradle.platformparity

import mihon.gradle.kotlinsource.KotlinSourceFile
import mihon.gradle.tasks.withoutCommentsAndStrings

internal data class PlatformSource(val relativePath: String, val content: String)

/** One production Kotlin file, with the declarations the platform checks read. */
internal class PlatformFile(val relativePath: String, val sourceSet: PlatformSourceSet, content: String) {
    val code: String = content.withoutCommentsAndStrings()
    private val source = KotlinSourceFile.from(relativePath, content)
    val packageName: String = source.packageName
    val imports = source.imports.filter { it.importedFqName != null }
    val types: List<TypeDeclaration> = code.typeDeclarations(packageName)
    val bindings: List<BindingDeclaration> by lazy { code.bindingDeclarations() }
    val topLevelDeclarations: List<TopLevelDeclaration> = code.topLevelDeclarations(packageName)

    /** The qualified names of what this file declares at its top level, and of the classes nested in its classes. */
    val declaredNames: Set<String> = buildSet {
        topLevelDeclarations.mapTo(this) { it.qualifiedName }
        types.mapNotNullTo(this) { it.qualifiedName }
    }

    val identifiers: Set<String> = source.references.mapTo(mutableSetOf()) { it.name }

    val platform: Platform? get() = sourceSet.platform

    fun lineNumberAt(offset: Int): Int = 1 + (0 until offset).count { code[it] == '\n' }

    /**
     * The qualified names [name] (simple, or qualified as written) can stand for here: what an import names, else the
     * same package's or a star-imported package's. Kotlin's and Java's default imports are left out; no repository or
     * platform rule concerns them.
     */
    fun candidates(name: String): List<String> {
        val first = name.substringBefore('.')
        importOf(first)?.let { return listOf(it.importedFqName + name.removePrefix(first)) }
        if (first.first().isLowerCase() && '.' in name) return listOf(name)
        return listOf(qualify(name)) + imports.filter { it.isStar }.map { "${it.importedFqName}.$name" }
    }

    /** Whether [name]'s first segment is a name this file imports by itself, rather than through a star import. */
    fun isImported(name: String): Boolean = importOf(name.substringBefore('.')) != null

    private fun importOf(simpleName: String) =
        imports.firstOrNull { !it.isStar && (it.alias ?: it.importedFqName!!.substringAfterLast('.')) == simpleName }

    private fun qualify(name: String) = if (packageName.isEmpty()) name else "$packageName.$name"
}

/**
 * The repository's production code across its modules, and what each platform can see of it. A name is visible on a
 * platform when that platform's compilations can see it: a declaration of `commonMain` or of that platform's source
 * sets, a name of that platform's libraries (and on desktop, for modules whose shared code compiles against the
 * Android API, the Android API's stand-ins), or generated shared code: a name nothing here declares in a package that
 * shared sources declare, such as resource accessors and database queries. Android's generated `R` and `BuildConfig`
 * are never shared.
 */
internal class PlatformCodebase(
    sources: List<PlatformSource>,
    buildFiles: List<PlatformBuildFile>,
    val apis: PlatformApis,
) {

    val modules = PlatformModules(buildFiles)

    val files: List<PlatformFile> = sources.mapNotNull { source ->
        if (!source.relativePath.endsWith(".kt")) return@mapNotNull null
        val sourceSet = modules.sourceSetOf(source.relativePath) ?: return@mapNotNull null
        PlatformFile(source.relativePath, sourceSet, source.content)
    }

    /** The files that declare each qualified name. */
    val declarations: Map<String, List<PlatformFile>> = buildMap<String, MutableList<PlatformFile>> {
        files.forEach { file -> file.declaredNames.forEach { getOrPut(it, ::mutableListOf) += file } }
    }

    /** Every type declaration, by its qualified name, with the file declaring it. */
    val types: Map<String, List<Pair<PlatformFile, TypeDeclaration>>> = files
        .flatMap { file -> file.types.mapNotNull { type -> type.qualifiedName?.let { it to (file to type) } } }
        .groupBy({ it.first }, { it.second })

    private val sharedPackages: Set<String> = files.filter {
        it.platform == null
    }.mapTo(mutableSetOf()) { it.packageName }

    private val packageRoots: Set<String> =
        (apis.desktop.packages + apis.android.packages + files.map { it.packageName })
            .mapTo(mutableSetOf()) { it.substringBefore('.') }

    /** The modules either app ships: the Android app's and the desktop app's, with what they depend on. */
    val appModules: Set<String> = modules.closure(ANDROID_APP) + modules.closure(DESKTOP_APP)

    val androidAppModules: Set<String> = modules.closure(ANDROID_APP)

    /** The shared types marked `@PlatformFacility`: the contracts each platform answers on its own. */
    val facilities: Set<String> = types.filterValues { declarations ->
        declarations.any { (file, type) -> file.platform == null && FACILITY_ANNOTATION in type.annotations }
    }.keys

    /** Resolves [name] as [file] writes it to the repository types it can stand for. */
    fun resolveTypes(file: PlatformFile, name: String): List<Pair<PlatformFile, TypeDeclaration>> =
        file.candidates(name).flatMap { types[it].orEmpty() }

    /**
     * The shared types [type] extends or implements, directly or through types of platform-only source sets, which
     * is how a platform class reaches a shared contract through a platform interface of its own.
     */
    fun sharedSupertypes(file: PlatformFile, type: TypeDeclaration): List<Pair<PlatformFile, TypeDeclaration>> {
        val found = mutableListOf<Pair<PlatformFile, TypeDeclaration>>()
        val seen = mutableSetOf<String>()
        fun visit(declaringFile: PlatformFile, declaration: TypeDeclaration) {
            declaration.superTypes.flatMap { resolveTypes(declaringFile, it) }.forEach { resolved ->
                val (resolvedFile, resolvedType) = resolved
                if (!seen.add(resolvedType.qualifiedName ?: return@forEach)) return@forEach
                if (resolvedFile.platform == null) found += resolved else visit(resolvedFile, resolvedType)
            }
        }
        visit(file, type)
        return found
    }

    /** Whether [platform]'s compilations can see [qualifiedName], from code in [viewer]. */
    fun isVisible(qualifiedName: String, platform: Platform, viewer: PlatformModule): Boolean {
        val libraries = when (platform) {
            Platform.DESKTOP -> apis.desktop
            Platform.ANDROID -> apis.android
        }
        val standIns = apis.androidStandIns.takeIf { platform == Platform.DESKTOP && viewer.sharedCodeSeesAndroidApi }
        declarations[qualifiedName]?.let { files ->
            return files.any { it.platform == null || it.platform == platform }
        }
        if (qualifiedName in libraries.classes || qualifiedName in libraries.callables) return true
        if (standIns != null && (qualifiedName in standIns.classes || qualifiedName in standIns.callables)) return true
        // A member of a class: `Foo.Companion.bar`, an enum entry, a nested class the libraries don't list. Only a
        // class says it has the member; a type alias of the same name, as Coil's `PlatformContext` is on Android,
        // may stand for a class without it.
        var owner = qualifiedName.substringBeforeLast('.', missingDelimiterValue = "")
        while ('.' in owner) {
            types[owner]?.let { owners ->
                return owners.any { (file, _) ->
                    file.platform == null ||
                        file.platform == platform
                }
            }
            if (owner in libraries.classes || (standIns != null && owner in standIns.classes)) return true
            owner = owner.substringBeforeLast('.')
        }
        return isGeneratedSharedCode(qualifiedName)
    }

    /**
     * Whether each platform supplies [qualifiedName] itself whatever the code inside needs: a facility, a type that
     * implements one, or an `expect` declaration.
     */
    fun isPlatformSupplied(qualifiedName: String): Boolean {
        if (qualifiedName in facilities) return true
        if (qualifiedName in expectedNames) return true
        return types[qualifiedName].orEmpty().any { (file, type) ->
            sharedSupertypes(file, type).any { (_, superType) -> superType.qualifiedName in facilities }
        }
    }

    private val expectedNames: Set<String> = files.filter { it.platform == null }.flatMap { file ->
        file.topLevelDeclarations.filter { "expect" in it.modifiers }.map { it.qualifiedName } +
            file.types.filter { "expect" in it.modifiers }.mapNotNull { it.qualifiedName }
    }.toSet()

    /** Whether [platform]'s compilations can see the package [packageName], from code in [viewer]. */
    fun isVisiblePackage(packageName: String, platform: Platform, viewer: PlatformModule): Boolean {
        if (files.any { it.packageName == packageName && (it.platform == null || it.platform == platform) }) return true
        if (platform == Platform.DESKTOP && viewer.sharedCodeSeesAndroidApi &&
            packageName in apis.androidStandIns.packages
        ) {
            return true
        }
        return packageName in (if (platform == Platform.DESKTOP) apis.desktop else apis.android).packages
    }

    /** Whether [file] uses `R` or `BuildConfig`, the classes Android generates for its modules. */
    fun usesAndroidGeneratedClasses(file: PlatformFile): Boolean =
        ANDROID_GENERATED_USE.containsMatchIn(file.code) ||
            file.imports.any { it.importedFqName!!.substringAfterLast('.') in ANDROID_GENERATED_CLASSES }

    /** The qualified class names [file]'s code writes out in full, such as `android.os.Build.VERSION`. */
    fun qualifiedUses(file: PlatformFile): List<String> = file.code.lineSequence()
        .filterNot { it.startsWith("import ") || it.startsWith("package ") }
        .flatMap { line -> QUALIFIED_NAME.findAll(line).map { it.value } }
        .filter { it.substringBefore('.') in packageRoots }
        .toList()

    private fun isGeneratedSharedCode(qualifiedName: String): Boolean {
        val segments = qualifiedName.split('.')
        val classIndex = segments.indexOfFirst { it.firstOrNull()?.isUpperCase() == true }
        if (classIndex <= 0) return false
        if (segments[classIndex] in ANDROID_GENERATED_CLASSES) return false
        val packageName = segments.take(classIndex).joinToString(".")
        val className = segments.take(classIndex + 1).joinToString(".")
        return packageName in sharedPackages && className !in declarations
    }

    companion object {
        const val ANDROID_APP = "app"
        const val DESKTOP_APP = "desktop"
        const val FACILITY_ANNOTATION = "PlatformFacility"
    }
}

private val ANDROID_GENERATED_CLASSES = setOf("R", "BuildConfig")

private val ANDROID_GENERATED_USE = Regex("""(?<![\w.])(?:R\.[a-z]|BuildConfig\.)""")

private val QUALIFIED_NAME = Regex("""(?<![\w.])(?:[a-z_]\w*\.)+[A-Z]\w*""")
