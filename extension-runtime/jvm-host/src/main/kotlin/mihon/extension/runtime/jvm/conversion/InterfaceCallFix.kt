package mihon.extension.runtime.jvm.conversion

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Marks the `invokespecial` and `invokestatic` calls dex2jar writes to an interface's own methods, a default method's
 * `super` call or a static interface method, as calls to an interface member. Dex records no such distinction, so
 * dex2jar writes them as calls to a class's method, which the JVM refuses on an interface with an
 * `IncompatibleClassChangeError` ("must be InterfaceMethodref constant"). Kotlin's interface default methods, such as
 * every kotlinx.serialization serializer's `typeParametersSerializers`, are called this way.
 *
 * Only Java 8 class files are fixed: dex2jar writes older ones for dex code from before interfaces had code, and the
 * JVM allows those calls in Java 8 class files only.
 */
internal class InterfaceCallFix(next: ClassVisitor, private val hierarchy: ClassHierarchy) :
    ClassVisitor(Opcodes.ASM9, next) {

    private var fixes = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?,
    ) {
        fixes = version and 0xffff >= Opcodes.V1_8
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?,
    ): MethodVisitor? {
        val next = super.visitMethod(access, name, descriptor, signature, exceptions) ?: return null
        if (!fixes) return next
        return object : MethodVisitor(Opcodes.ASM9, next) {
            override fun visitMethodInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean,
            ) {
                val namesOwnMethod = opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKESTATIC
                val ownerIsInterface = isInterface || (namesOwnMethod && hierarchy.isInterface(owner))
                super.visitMethodInsn(opcode, owner, name, descriptor, ownerIsInterface)
            }
        }
    }
}
