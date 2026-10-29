package mihon.extension.runtime.jvm.conversion

import com.googlecode.d2j.Method
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.dex.DexExceptionHandler
import com.googlecode.d2j.node.DexFileNode
import com.googlecode.d2j.node.DexMethodNode
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.MultiDexFileReader
import com.googlecode.d2j.visitors.DexFileVisitor
import logcat.LogPriority
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Turns an apk into a jar the JVM runs: its dex code converted by dex2jar, as Suwayomi converts it, with the fixes the
 * JVM needs ([RemovedConstructorFix] on the dex code, [InterfaceCallFix] and [AndroidDateFormatRedirect] on the
 * classes), and the apk's other files beside the classes, since on Android an apk's class loader finds its Java
 * resources in the apk itself.
 *
 * A method dex2jar can't translate is converted to one that throws [UNCONVERTED_METHOD_MESSAGE] naming it, so the rest of
 * the extension still runs.
 */
internal object DexConversion {

    /** Names what a jar was converted with, so a new converter or fix converts every apk again. */
    val converterVersion: String =
        "${Dex2jar::class.java.`package`.implementationVersion ?: "unversioned"}-$FIXES_REVISION"

    const val UNCONVERTED_METHOD_MESSAGE = "Katari couldn't convert this method of the extension to run on desktop: "

    /** Writes the jar of [apk] to [output], which mustn't exist yet. Throws [DexConversionException] when it can't. */
    fun convert(apk: File, output: Path) {
        val translated = output.resolveSibling("${output.fileName}.dex2jar")
        try {
            val unconverted = mutableListOf<String>()
            try {
                Dex2jar.from(FixedDex(MultiDexFileReader.open(apk.readBytes())))
                    .withExceptionHandler(UnconvertedMethods(unconverted))
                    .reUseReg(false)
                    .topoLogicalSort()
                    .skipDebug(true)
                    .optimizeSynchronized(false)
                    .noCode(false)
                    .skipExceptions(false)
                    .dontSanitizeNames(true)
                    .computeFrames(true)
                    .to(translated)
            } catch (e: Exception) {
                throw DexConversionException(e)
            }
            if (unconverted.isNotEmpty()) {
                logcat(LogPriority.WARN) { "${apk.name}: dex2jar couldn't convert ${unconverted.joinToString()}" }
            }
            writeFixedJar(apk, translated, output)
        } finally {
            Files.deleteIfExists(translated)
        }
    }

    private fun writeFixedJar(apk: File, translated: Path, output: Path) {
        ZipOutputStream(Files.newOutputStream(output)).use { jar ->
            ZipFile(translated.toFile()).use { classes ->
                val readers = classes.entries().asSequence()
                    .filter { it.name.endsWith(CLASS_SUFFIX) }
                    .map { it.name to ClassReader(classes.getInputStream(it).readBytes()) }
                    .toList()
                val interfaces = InterfaceTypes(
                    apkInterfaces = readers.filter { (_, reader) -> reader.access and Opcodes.ACC_INTERFACE != 0 }
                        .mapTo(HashSet()) { (_, reader) -> reader.className },
                    apkClasses = readers.mapTo(HashSet()) { (_, reader) -> reader.className },
                )
                readers.forEach { (name, reader) ->
                    val writer = ClassWriter(0)
                    reader.accept(
                        InterfaceCallFix(ClassRemapper(writer, AndroidDateFormatRedirect.remapper), interfaces),
                        0,
                    )
                    jar.putNextEntry(ZipEntry(name))
                    jar.write(writer.toByteArray())
                    jar.closeEntry()
                }
            }
            ZipFile(apk).use { source ->
                source.entries().asSequence()
                    .filter { !it.isDirectory && it.isResource() }
                    .forEach { entry ->
                        jar.putNextEntry(ZipEntry(entry.name))
                        source.getInputStream(entry).use { it.copyTo(jar) }
                        jar.closeEntry()
                    }
            }
        }
    }

    /**
     * Everything but the dex code and the jar signature files: the signatures are the apk's, which the converted
     * classes no longer match, and the JVM would refuse a jar whose signatures fail.
     */
    private fun ZipEntry.isResource(): Boolean {
        if (DEX_FILE.matches(name)) return false
        if (!name.startsWith("META-INF/")) return true
        val file = name.removePrefix("META-INF/")
        return file != "MANIFEST.MF" && !SIGNATURE_FILE.matches(file)
    }

    /** [reader]'s dex code with [RemovedConstructorFix] applied, read whole first since the fix spans classes. */
    private class FixedDex(private val reader: BaseDexFileReader) : BaseDexFileReader by reader {
        override fun accept(dv: DexFileVisitor) = accept(dv, 0)

        override fun accept(dv: DexFileVisitor, config: Int) {
            val dex = DexFileNode()
            reader.accept(dex, config)
            RemovedConstructorFix.apply(dex)
            // DexFileNode.accept passes on the classes only. Dex2jar writes Java 8 class files, which may have
            // interface methods with code, only for dex files from version 037 on, so without the version every
            // class would be written as Java 6 and interfaces with default or static methods would fail to load.
            dv.visitDexFileVersion(dex.dexVersion)
            dex.accept(dv)
        }

        override fun accept(dv: DexFileVisitor, classIdx: Int, config: Int) =
            throw UnsupportedOperationException("Dex2jar reads every class at once")
    }

    private class UnconvertedMethods(private val members: MutableList<String>) : DexExceptionHandler {
        override fun handleFileException(e: Exception) {
            throw e
        }

        override fun handleMethodTranslateException(
            method: Method,
            node: DexMethodNode,
            visitor: MethodVisitor,
            e: Exception,
        ) {
            val member = "${method.owner.removePrefix("L").removeSuffix(";").replace('/', '.')}.${method.name}"
            members += member
            visitor.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException")
            visitor.visitInsn(Opcodes.DUP)
            visitor.visitLdcInsn(UNCONVERTED_METHOD_MESSAGE + member)
            visitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/RuntimeException",
                "<init>",
                "(Ljava/lang/String;)V",
                false,
            )
            visitor.visitInsn(Opcodes.ATHROW)
        }
    }

    /** Bumped whenever a fix changes what a conversion writes. */
    private const val FIXES_REVISION = 3

    private const val CLASS_SUFFIX = ".class"
    private val DEX_FILE = Regex("classes\\d*\\.dex")
    private val SIGNATURE_FILE = Regex("[^/]+\\.(SF|RSA|DSA|EC)", RegexOption.IGNORE_CASE)
}

/**
 * The method, as `package.Class.method`, that [error] or one of its causes says dex2jar couldn't convert, so it throws
 * in its place; `null` for any other error.
 */
fun unconvertedMethodOf(error: Throwable): String? =
    generateSequence(error) { it.cause.takeIf { cause -> cause !== it } }
        .take(MAX_CAUSES)
        .firstNotNullOfOrNull {
            it.message?.substringAfter(DexConversion.UNCONVERTED_METHOD_MESSAGE, "")?.ifEmpty { null }
        }

private const val MAX_CAUSES = 16

/**
 * An apk whose code dex2jar couldn't read or convert. It's the root of what's reported, so that the install and load
 * errors, which show the root's message, say what failed in words; dex2jar's own error is kept as suppressed.
 */
class DexConversionException(failure: Exception) :
    Exception("Katari couldn't convert this extension's code to run on desktop: ${failure.message}") {
    init {
        addSuppressed(failure)
    }
}
