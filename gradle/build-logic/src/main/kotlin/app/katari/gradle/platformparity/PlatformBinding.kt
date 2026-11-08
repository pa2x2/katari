package app.katari.gradle.platformparity

/**
 * Which files of platform-only source sets need their platform. A file there is bound to its platform when it imports
 * or writes out in full a name the other platform can't see, uses Android's generated `R` or `BuildConfig`, declares
 * an `actual` or a class implementing a `@PlatformFacility` (each platform's own by definition), or refers to a top-level declaration of a bound file of its own source set, by import or by a
 * same-package simple name. The last is taken transitively, so a file that only wraps a bound helper is bound too. A
 * file that isn't bound is neutral: nothing stops it from being shared.
 *
 * Names declared in the file's own source set don't count by themselves: the other platform can't see them only
 * because they sit beside the file, which says nothing about whether they need the platform.
 */
internal class PlatformBinding(private val codebase: PlatformCodebase) {

    private val boundFiles: Set<PlatformFile>

    init {
        val platformFiles = codebase.files.filter { it.platform != null }
        val dependents = mutableMapOf<PlatformFile, MutableSet<PlatformFile>>()
        val bound = mutableSetOf<PlatformFile>()
        platformFiles.forEach { file ->
            val (direct, sameSourceSet) = references(file)
            if (direct) bound += file
            sameSourceSet.forEach { dependents.getOrPut(it, ::mutableSetOf) += file }
        }
        val pending = ArrayDeque(bound)
        while (pending.isNotEmpty()) {
            dependents[pending.removeFirst()].orEmpty().forEach { if (bound.add(it)) pending += it }
        }
        boundFiles = bound
    }

    fun isBound(file: PlatformFile): Boolean = file in boundFiles

    private fun PlatformFile.implementsFacility(type: TypeDeclaration): Boolean =
        codebase.sharedSupertypes(this, type).any { (_, superType) -> superType.qualifiedName in codebase.facilities }

    /** Whether [file] is bound by itself, and the files of its own source set whose declarations it uses. */
    private fun references(file: PlatformFile): Pair<Boolean, Set<PlatformFile>> {
        val platform = file.platform!!
        val other = platform.other
        val module = file.sourceSet.module
        val sameSourceSet = mutableSetOf<PlatformFile>()
        fun beside(name: String) = codebase.declarations[name].orEmpty()
            .filter { it !== file && it.sourceSet.id == file.sourceSet.id }

        if (ACTUAL_DECLARATION.containsMatchIn(file.code)) return true to emptySet()
        if (file.types.any { file.implementsFacility(it) }) return true to emptySet()
        if (platform == Platform.ANDROID && codebase.usesAndroidGeneratedClasses(file)) return true to emptySet()
        file.imports.forEach { import ->
            val name = import.importedFqName!!
            if (import.isStar) {
                if (!codebase.isVisiblePackage(name, other, module)) return true to emptySet()
                return@forEach
            }
            val besideFiles = generateSequence(name) {
                it.substringBeforeLast('.', "").takeIf { owner -> '.' in owner }
            }
                .map(::beside)
                .firstOrNull { it.isNotEmpty() }
            when {
                besideFiles != null -> sameSourceSet += besideFiles
                codebase.declarations[name]?.any { it.sourceSet.id == file.sourceSet.id } == true -> Unit
                !codebase.isVisible(name, other, module) -> return true to emptySet()
            }
        }
        if (codebase.qualifiedUses(file).any { !codebase.isVisible(it, other, module) }) return true to emptySet()
        file.identifiers.forEach { identifier ->
            val name = if (file.packageName.isEmpty()) identifier else "${file.packageName}.$identifier"
            val declaringFiles = codebase.declarations[name] ?: return@forEach
            val besideFiles = beside(name)
            when {
                besideFiles.isNotEmpty() -> sameSourceSet += besideFiles
                declaringFiles.all { it === file } -> Unit
                declaringFiles.none { it.platform == null || it.platform == other } -> return true to emptySet()
            }
        }
        return false to sameSourceSet
    }
}

private val ACTUAL_DECLARATION =
    Regex("""\bactual\s+(?:[a-z]+\s+)*(?:fun|val|var|class|interface|object|typealias|constructor)\b""")
