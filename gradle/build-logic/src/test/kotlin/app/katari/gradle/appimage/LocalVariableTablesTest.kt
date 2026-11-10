package app.katari.gradle.appimage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class LocalVariableTablesTest {

    @Test
    fun `a class loses its local variables and keeps the line numbers stack traces show`() {
        val rewritten = withoutLocalVariableTables(classWithDebugInformation())

        val variables = mutableListOf<String>()
        val lines = mutableListOf<Int>()
        ClassReader(rewritten).accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ) = object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitLocalVariable(
                        name: String,
                        descriptor: String,
                        signature: String?,
                        start: Label,
                        end: Label,
                        index: Int,
                    ) {
                        variables += name
                    }

                    override fun visitLineNumber(line: Int, start: Label) {
                        lines += line
                    }
                }
            },
            0,
        )
        assertEquals(emptyList<String>(), variables)
        assertEquals(listOf(7), lines)
    }

    /** `static int same(int value) { return value; }`, compiled with its line and its variable's name. */
    private fun classWithDebugInformation(): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Sample", null, "java/lang/Object", null)
        writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "same", "(I)I", null, null).apply {
            val start = Label()
            val end = Label()
            visitCode()
            visitLabel(start)
            visitLineNumber(7, start)
            visitVarInsn(Opcodes.ILOAD, 0)
            visitInsn(Opcodes.IRETURN)
            visitLabel(end)
            visitLocalVariable("value", "I", null, start, end, 0)
            visitMaxs(1, 1)
            visitEnd()
        }
        writer.visitEnd()
        return writer.toByteArray()
    }
}
