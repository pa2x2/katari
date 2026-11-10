package app.katari.gradle.appimage

import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.RecordComponentVisitor

/**
 * [classFile] without its methods' local variable tables: the names and types of local variables, which only a
 * debugger reads. Stack traces take their lines from the line number tables, which stay.
 *
 * The class is written anew, with a constant pool of what it still uses. A class that carries an attribute from
 * outside the class file format is returned as it is, as such an attribute may point into the constant pool it had.
 */
internal fun withoutLocalVariableTables(classFile: ByteArray): ByteArray {
    val writer = ClassWriter(0)
    val remover = LocalVariableTableRemover(writer)
    ClassReader(classFile).accept(remover, 0)
    return if (remover.hasForeignAttribute) classFile else writer.toByteArray()
}

private class LocalVariableTableRemover(writer: ClassWriter) : ClassVisitor(Opcodes.ASM9, writer) {
    var hasForeignAttribute = false
        private set

    override fun visitAttribute(attribute: Attribute) {
        hasForeignAttribute = true
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?,
    ): FieldVisitor = object : FieldVisitor(api, super.visitField(access, name, descriptor, signature, value)) {
        override fun visitAttribute(attribute: Attribute) {
            hasForeignAttribute = true
        }
    }

    override fun visitRecordComponent(name: String, descriptor: String, signature: String?): RecordComponentVisitor =
        object : RecordComponentVisitor(api, super.visitRecordComponent(name, descriptor, signature)) {
            override fun visitAttribute(attribute: Attribute) {
                hasForeignAttribute = true
            }
        }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor = object : MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
        override fun visitAttribute(attribute: Attribute) {
            hasForeignAttribute = true
        }

        override fun visitLocalVariable(
            name: String,
            descriptor: String,
            signature: String?,
            start: Label,
            end: Label,
            index: Int,
        ) {
            // Left out of the class written
        }
    }
}
