package mihon.gradle.platformparity

/**
 * The repository's modules as their build files describe them: which platforms a module's source sets compile for,
 * which modules it depends on outside its tests, and whether its shared code compiles against the Android API (D063).
 * Read from the build files' text, as the checks read sources, so they run without configuring the build.
 */
internal data class PlatformBuildFile(val modulePath: String, val content: String)

internal enum class Platform(val label: String) {
    ANDROID("Android"),
    DESKTOP("desktop"),
    ;

    val other: Platform get() = if (this == ANDROID) DESKTOP else ANDROID
}

internal enum class ModuleKind { MULTIPLATFORM, ANDROID, DESKTOP }

internal data class PlatformModule(
    val path: String,
    val kind: ModuleKind,
    val dependencies: Set<String>,
    val sharedCodeSeesAndroidApi: Boolean,
)

/** A production source set; [platform] is null for `commonMain`, which every platform compiles. */
internal data class PlatformSourceSet(val module: PlatformModule, val name: String, val platform: Platform?) {
    val id: String = "${module.path}/src/$name"
}

internal class PlatformModules(buildFiles: List<PlatformBuildFile>) {

    val modules: Map<String, PlatformModule>

    init {
        val accessors = buildFiles.associate { it.modulePath.toProjectAccessor() to it.modulePath }
        modules = buildFiles.mapNotNull { buildFile ->
            val kind = buildFile.content.moduleKind() ?: return@mapNotNull null
            PlatformModule(
                path = buildFile.modulePath,
                kind = kind,
                dependencies = buildFile.content.productionProjectDependencies(accessors),
                sharedCodeSeesAndroidApi = SHARED_CODE_AGAINST_ANDROID_API.containsMatchIn(buildFile.content),
            )
        }.associateBy { it.path }
    }

    /** [root] and every module it depends on outside tests, directly or not. */
    fun closure(root: String): Set<String> {
        val reached = mutableSetOf<String>()
        val pending = ArrayDeque(listOfNotNull(modules[root]?.path))
        while (pending.isNotEmpty()) {
            val module = pending.removeFirst()
            if (reached.add(module)) pending += modules[module]?.dependencies.orEmpty()
        }
        return reached
    }

    /** The production source set [relativePath] belongs to, or null for tests, build scripts and unknown modules. */
    fun sourceSetOf(relativePath: String): PlatformSourceSet? {
        val module = modules.values
            .filter { relativePath.startsWith("${it.path}/src/") }
            .maxByOrNull { it.path.length }
            ?: return null
        val name = relativePath.removePrefix("${module.path}/src/").substringBefore('/')
        if (name.contains("test", ignoreCase = true)) return null
        val platform = when (name) {
            "commonMain" -> null
            "androidMain" -> Platform.ANDROID
            "desktopMain" -> Platform.DESKTOP
            else -> when (module.kind) {
                ModuleKind.ANDROID -> Platform.ANDROID
                ModuleKind.DESKTOP -> Platform.DESKTOP
                ModuleKind.MULTIPLATFORM -> return null
            }
        }
        return PlatformSourceSet(module, name, platform)
    }
}

private fun String.moduleKind(): ModuleKind? = when {
    contains("mihonx.plugins.kotlin.multiplatform") -> ModuleKind.MULTIPLATFORM
    contains("mihonx.plugins.android.") -> ModuleKind.ANDROID
    contains("mihonx.plugins.desktop.application") || contains("mihonx.plugins.kotlin.jvm") -> ModuleKind.DESKTOP
    else -> null
}

/**
 * The modules this build file depends on through type-safe project accessors, leaving out test configurations and
 * test source sets' blocks, and the baseline profile producer.
 */
private fun String.productionProjectDependencies(accessors: Map<String, String>): Set<String> {
    val blocks = ArrayDeque<String>()
    val dependencies = mutableSetOf<String>()
    lineSequence().forEach { rawLine ->
        val line = rawLine.substringBefore("//")
        val isTestLine = TEST_CONFIGURATION.containsMatchIn(line) ||
            (blocks + line.substringBefore('{', missingDelimiterValue = "")).any { it.contains("test", true) }
        if (!isTestLine) {
            PROJECT_ACCESSOR.findAll(line).forEach { match ->
                val segments = match.groupValues[1].split('.')
                (segments.size downTo 1)
                    .firstNotNullOfOrNull { size -> accessors[segments.take(size).joinToString(".")] }
                    ?.let(dependencies::add)
            }
        }
        val opened = line.count { it == '{' }
        val closed = line.count { it == '}' }
        repeat(closed - opened) { blocks.removeLastOrNull() }
        repeat(opened - closed) { blocks.addLast(line.substringBefore('{').trim()) }
    }
    return dependencies
}

/**
 * The accessor Gradle generates for the module at [this] path: `features/entry-interactions/api` →
 * `features.entryInteractions.api`.
 */
private fun String.toProjectAccessor(): String = split('/').joinToString(".") { segment ->
    segment.split('-').mapIndexed { index, part ->
        if (index == 0) part else part.replaceFirstChar(Char::uppercase)
    }.joinToString("")
}

private val PROJECT_ACCESSOR = Regex("""\bprojects\.([\w.]+)""")

private val TEST_CONFIGURATION = Regex("""(?i)^\s*"?\w*(?:test|baselineProfile)\w*"?\s*\(""")

private val SHARED_CODE_AGAINST_ANDROID_API = Regex("""(?m)^\s*compileSharedCodeAgainstAndroidApi\(""")
