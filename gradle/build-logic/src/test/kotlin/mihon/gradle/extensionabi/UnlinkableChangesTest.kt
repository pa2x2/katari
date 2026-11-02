package mihon.gradle.extensionabi

import japicmp.cmp.JApiCmpArchive
import japicmp.cmp.JarArchiveComparator
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.model.AccessModifier
import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class UnlinkableChangesTest {

    @Test
    fun `accessors and anonymous classes the compiler made may go while a public method may not`(@TempDir dir: File) {
        val baseline = jar(dir, "baseline", api(call = true, accessor = true), anonymousClass())

        val withoutGenerated = jar(dir, "without-generated", api(call = true, accessor = false))
        val withoutCall = jar(dir, "without-call", api(call = false, accessor = true), anonymousClass())

        assertEquals(emptyList<String>(), incompatibilities(baseline, withoutGenerated))
        assertEquals(listOf("call:METHOD_REMOVED"), incompatibilities(baseline, withoutCall))
    }

    private fun incompatibilities(baseline: File, current: File): List<String> {
        val options = Options.newDefault().apply {
            setAccessModifier(AccessModifier.PROTECTED)
            isIncludeSynthetic = true
        }
        val classes = JarArchiveComparator(JarArchiveComparatorOptions.of(options)).compare(
            listOf(JApiCmpArchive(baseline, "baseline")),
            listOf(JApiCmpArchive(current, "current")),
        )
        acceptUnlinkableChanges(classes)
        return classes.flatMap { it.elements() }.flatMap { (name, element) ->
            element.compatibilityChanges.filterNot { it.isBinaryCompatible }.map { "$name:${it.type}" }
        }
    }

    private fun JApiClass.elements(): List<Pair<String, JApiCompatibility>> =
        listOf(fullyQualifiedName to this, fullyQualifiedName to superclass) +
            methods.map { it.name to it } +
            constructors.map { it.name to it } +
            fields.map { it.name to it }

    private fun api(call: Boolean, accessor: Boolean) = classFile(API) {
        if (call) method("call", Opcodes.ACC_PUBLIC)
        if (accessor) method("access\$getState\$p", Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC)
    }

    private fun anonymousClass() = classFile("$API\$1") {
        visitOuterClass(API, null, null)
        visitInnerClass("$API\$1", null, null, Opcodes.ACC_FINAL)
        method("run", Opcodes.ACC_PUBLIC)
    }

    private fun classFile(name: String, body: ClassWriter.() -> Unit): Pair<String, ByteArray> {
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER, name, null, "java/lang/Object", null)
        writer.body()
        writer.visitEnd()
        return "$name.class" to writer.toByteArray()
    }

    private fun ClassWriter.method(name: String, access: Int) {
        visitMethod(access, name, "()V", null, null).apply {
            visitCode()
            visitInsn(Opcodes.RETURN)
            visitMaxs(0, 0)
            visitEnd()
        }
    }

    private fun jar(dir: File, name: String, vararg classes: Pair<String, ByteArray>): File {
        val file = dir.resolve("$name.jar")
        JarOutputStream(file.outputStream()).use { jar ->
            classes.forEach { (path, bytes) ->
                jar.putNextEntry(JarEntry(path))
                jar.write(bytes)
                jar.closeEntry()
            }
        }
        return file
    }

    private companion object {
        const val API = "sample/Api"
    }
}
