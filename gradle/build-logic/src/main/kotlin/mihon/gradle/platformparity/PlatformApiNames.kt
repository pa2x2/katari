package mihon.gradle.platformparity

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata

/**
 * The names one platform's compilations can see from outside the repository: every class, with nested classes joined
 * by `.` as Kotlin names them, and the top-level functions, properties and type aliases of Kotlin file facades. A
 * facade's members need its Kotlin metadata, because their package can be one the other platform also has:
 * `LocalContext` lives in `AndroidCompositionLocals_androidKt`, in `androidx.compose.ui.platform`, which desktop's
 * Compose also declares.
 */
internal data class PlatformApiNames(val classes: Set<String>, val callables: Set<String>) {

    val packages: Set<String> by lazy {
        (classes + callables).mapTo(mutableSetOf()) { it.substringBeforeLast('.', missingDelimiterValue = "") }
    }

    operator fun plus(other: PlatformApiNames) =
        PlatformApiNames(classes + other.classes, callables + other.callables)

    fun write(writer: Appendable) {
        classes.sorted().forEach { writer.appendLine("$CLASS_PREFIX$it") }
        callables.sorted().forEach { writer.appendLine("$CALLABLE_PREFIX$it") }
    }

    companion object {
        val EMPTY = PlatformApiNames(emptySet(), emptySet())

        fun read(lines: Iterable<String>): PlatformApiNames {
            val classes = mutableSetOf<String>()
            val callables = mutableSetOf<String>()
            lines.forEach { line ->
                when {
                    line.startsWith(CLASS_PREFIX) -> classes += line.removePrefix(CLASS_PREFIX)
                    line.startsWith(CALLABLE_PREFIX) -> callables += line.removePrefix(CALLABLE_PREFIX)
                }
            }
            return PlatformApiNames(classes, callables)
        }

        private const val CLASS_PREFIX = "c "
        private const val CALLABLE_PREFIX = "f "
    }
}

/** Reads the classes of [classpath]: jars and class directories. */
internal fun indexClasspath(classpath: Iterable<File>): PlatformApiNames {
    val names = PlatformApiNameCollector()
    classpath.forEach { entry ->
        when {
            entry.isDirectory -> entry.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { file ->
                    file.inputStream().use { names.add(file.relativeTo(entry).invariantSeparatorsPath, it) }
                }
            entry.isFile -> ZipFile(entry).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".class") }
                    .forEach { classEntry -> zip.getInputStream(classEntry).use { names.add(classEntry.name, it) } }
            }
        }
    }
    return names.toNames()
}

/** Reads the classes of the JDK running this build, the one desktop code compiles against. */
internal fun indexRunningJdk(): PlatformApiNames {
    val names = PlatformApiNameCollector()
    val modules = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("/modules")
    Files.list(modules).use { moduleDirectories ->
        moduleDirectories.forEach { module ->
            Files.walk(module).use { paths ->
                paths.filter { it.isRegularFile() && it.fileName.toString().endsWith(".class") }
                    .forEach { path: Path ->
                        names.addClassName(module.relativize(path).invariantSeparatorsPathString)
                    }
            }
        }
    }
    return names.toNames()
}

private class PlatformApiNameCollector {
    private val classes = mutableSetOf<String>()
    private val callables = mutableSetOf<String>()

    fun add(entryName: String, bytes: InputStream) {
        val className = addClassName(entryName) ?: return
        val metadata = FacadeMetadataReader().also { reader ->
            ClassReader(bytes.readBytes()).accept(
                reader,
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
            )
        }.metadata() ?: return
        val packageName = className.substringBeforeLast('.', missingDelimiterValue = "")
        val kmPackage = when (val read = KotlinClassMetadata.readLenient(metadata)) {
            is KotlinClassMetadata.FileFacade -> read.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> read.kmPackage
            else -> return
        }
        (
            kmPackage.functions.map { it.name } + kmPackage.properties.map { it.name } +
                kmPackage.typeAliases.map { it.name }
            )
            .forEach { callables += if (packageName.isEmpty()) it else "$packageName.$it" }
    }

    /** Records the class at [entryName] and returns its name, or null for module and package descriptors. */
    fun addClassName(entryName: String): String? {
        val path = entryName.replace(MULTI_RELEASE_PREFIX, "").removeSuffix(".class")
        if (path.endsWith("module-info") || path.endsWith("package-info")) return null
        val binaryName = path.replace('/', '.')
        // Anonymous and local classes ($1, $lambda$2) have no name a source file could use.
        if (binaryName.split('$').drop(1).any { it.isEmpty() || it.first().isDigit() }) return null
        classes += binaryName.replace('$', '.')
        return binaryName
    }

    fun toNames() = PlatformApiNames(classes.toSet(), callables.toSet())

    private companion object {
        val MULTI_RELEASE_PREFIX = Regex("""^META-INF/versions/\d+/""")
    }
}

/** Collects a class's `kotlin.Metadata` when the class is a file facade or a part of a multi-file facade. */
private class FacadeMetadataReader : ClassVisitor(Opcodes.ASM9) {
    private var kind: Int? = null
    private var version: IntArray? = null
    private var data1: Array<String>? = null
    private var data2: Array<String>? = null
    private var extraString: String? = null
    private var packageName: String? = null
    private var extraInt: Int? = null

    fun metadata(): Metadata? {
        if (kind != FILE_FACADE && kind != MULTI_FILE_CLASS_PART) return null
        return Metadata(kind, version, data1, data2, extraString, packageName, extraInt)
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor != "Lkotlin/Metadata;") return null
        return object : AnnotationVisitor(Opcodes.ASM9) {
            override fun visit(name: String, value: Any) {
                when (name) {
                    "k" -> kind = value as Int
                    "mv" -> version = value as IntArray
                    "xs" -> extraString = value as String
                    "pn" -> packageName = value as String
                    "xi" -> extraInt = value as Int
                }
            }

            override fun visitArray(name: String): AnnotationVisitor = object : AnnotationVisitor(Opcodes.ASM9) {
                private val values = mutableListOf<String>()

                override fun visit(name: String?, value: Any) {
                    values += value as String
                }

                override fun visitEnd() {
                    when (name) {
                        "d1" -> data1 = values.toTypedArray()
                        "d2" -> data2 = values.toTypedArray()
                    }
                }
            }
        }
    }

    private companion object {
        const val FILE_FACADE = 2
        const val MULTI_FILE_CLASS_PART = 5
    }
}
