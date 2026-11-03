package mihon.extension.runtime.jvm.conversion

import com.android.dx.cf.direct.DirectClassFile
import com.android.dx.cf.direct.StdAttributeFactory
import com.android.dx.command.dexer.DxContext
import com.android.dx.dex.DexOptions
import com.android.dx.dex.cf.CfOptions
import com.android.dx.dex.cf.CfTranslator
import com.android.dx.dex.file.DexFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DexConversionTest {

    @TempDir
    lateinit var directory: Path

    // Keiyoushi's builds of 2026-11 pick a browser for their request headers into one variable from two sibling
    // classes on three branches, then call the classes' superclass on it. dex2jar's frames merged a class with its own
    // superclass to the superclass's superclass, so the variable was an Object there and the converted class failed
    // verification the first time a source built its client, taking every such extension down. The apk is dexed here
    // from classes of that shape; loading the converted class verifies it.
    @Test
    fun `a variable holding either of two sibling classes keeps their superclass's type`() {
        val apk = apkOf(
            browserClass(),
            browserSubclass("Chrome"),
            browserSubclass("Firefox"),
            pickerClass(branches = listOf("Chrome", "Firefox", "Chrome")),
        )
        val jar = directory.resolve("extension.jar")

        DexConversion.convert(apk.toFile(), jar)

        URLClassLoader(arrayOf(jar.toUri().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
            val pick = loader.loadClass(PICKER).getMethod("pick", Int::class.javaPrimitiveType)
            (0..2).map { pick.invoke(null, it) } shouldBe listOf("Chrome", "Firefox", "Chrome")
        }
    }

    private fun apkOf(vararg classes: Pair<String, ByteArray>): Path {
        val options = DexOptions().apply { minSdkVersion = MIN_SDK }
        val dex = DexFile(options)
        val context = DxContext()
        for ((name, bytes) in classes) {
            val classFile = DirectClassFile(bytes, "$name.class", true)
            classFile.setAttributeFactory(StdAttributeFactory.THE_ONE)
            dex.add(CfTranslator.translate(context, classFile, bytes, CfOptions(), options, dex))
        }
        val apk = directory.resolve("extension.apk")
        ZipOutputStream(Files.newOutputStream(apk)).use { out ->
            out.putNextEntry(ZipEntry("classes.dex"))
            out.write(dex.toDex(null, false))
            out.closeEntry()
        }
        return apk
    }

    /** `abstract class Browser { abstract fun name(): String }` */
    private fun browserClass(): Pair<String, ByteArray> = BROWSER to classBytes(BROWSER, Opcodes.ACC_ABSTRACT, OBJECT) {
        visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, "name", NAME_DESCRIPTOR, null, null).visitEnd()
    }

    /** `object <name> : Browser() { override fun name() = "<name>" }` */
    private fun browserSubclass(name: String): Pair<String, ByteArray> =
        name to classBytes(name, Opcodes.ACC_FINAL, BROWSER) {
            visitField(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, INSTANCE, "L$name;", null, null)
                .visitEnd()
            code(Opcodes.ACC_STATIC, "<clinit>", "()V") {
                visitTypeInsn(Opcodes.NEW, name)
                visitInsn(Opcodes.DUP)
                visitMethodInsn(Opcodes.INVOKESPECIAL, name, "<init>", "()V", false)
                visitFieldInsn(Opcodes.PUTSTATIC, name, INSTANCE, "L$name;")
                visitInsn(Opcodes.RETURN)
            }
            code(Opcodes.ACC_PUBLIC, "name", NAME_DESCRIPTOR) {
                visitLdcInsn(name)
                visitInsn(Opcodes.ARETURN)
            }
        }

    /** `fun pick(x: Int): String`: the name of the [branches] object at x as a Browser, the last one past the end. */
    private fun pickerClass(branches: List<String>): Pair<String, ByteArray> =
        PICKER to classBytes(PICKER, Opcodes.ACC_FINAL, OBJECT) {
            code(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "pick", "(I)Ljava/lang/String;") {
                val picked = Label()
                branches.forEachIndexed { index, branch ->
                    val next = Label()
                    val last = index == branches.lastIndex
                    if (!last) {
                        visitVarInsn(Opcodes.ILOAD, 0)
                        visitIntInsn(Opcodes.BIPUSH, index)
                        visitJumpInsn(Opcodes.IF_ICMPNE, next)
                    }
                    visitFieldInsn(Opcodes.GETSTATIC, branch, INSTANCE, "L$branch;")
                    visitVarInsn(Opcodes.ASTORE, 1)
                    if (!last) {
                        visitJumpInsn(Opcodes.GOTO, picked)
                        visitLabel(next)
                    }
                }
                visitLabel(picked)
                visitVarInsn(Opcodes.ALOAD, 1)
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, BROWSER, "name", NAME_DESCRIPTOR, false)
                visitInsn(Opcodes.ARETURN)
            }
        }

    private fun classBytes(name: String, access: Int, superName: String, body: ClassWriter.() -> Unit): ByteArray {
        // Without frames, which dex doesn't keep
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC or access, name, null, superName, null)
        writer.code(Opcodes.ACC_PUBLIC, "<init>", "()V") {
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false)
            visitInsn(Opcodes.RETURN)
        }
        writer.body()
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun ClassWriter.code(
        access: Int,
        name: String,
        descriptor: String,
        instructions: MethodVisitor.() -> Unit,
    ) {
        val method = visitMethod(access, name, descriptor, null, null)
        method.visitCode()
        method.instructions()
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private companion object {
        const val BROWSER = "Browser"
        const val PICKER = "Picker"
        const val OBJECT = "java/lang/Object"
        const val INSTANCE = "INSTANCE"
        const val NAME_DESCRIPTOR = "()Ljava/lang/String;"
        const val MIN_SDK = 26
    }
}
