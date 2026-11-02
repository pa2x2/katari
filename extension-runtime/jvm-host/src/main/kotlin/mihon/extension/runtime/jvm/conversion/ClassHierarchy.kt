package mihon.extension.runtime.jvm.conversion

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * The superclasses and interfaces of what an apk's converted classes name, by internal name: the apk's own classes, as
 * read from its converted ones with their superclasses as [remapper] rewrites them, and else the host's, which the
 * extension links against, as the class loader the conversion runs in finds them. Their headers are read, the classes
 * aren't loaded.
 *
 * A class neither has is taken as a direct subclass of `Object`: the extension fails on the missing class wherever it
 * names it, whatever its frames say.
 */
internal class ClassHierarchy(apkClasses: Iterable<ClassReader>, remapper: Remapper) {

    private val apk: Map<String, ClassHeader> = apkClasses.associate { reader ->
        reader.className to ClassHeader(
            superName = reader.superName?.let(remapper::mapType),
            isInterface = reader.access and Opcodes.ACC_INTERFACE != 0,
        )
    }

    fun isInterface(name: String): Boolean = header(name).isInterface

    /**
     * The nearest class both [type1] and [type2] are, as the JVM's verifier merges two types where code paths meet:
     * `Object` when either is an interface, since the verifier lets any reference stand for an interface type.
     */
    fun commonSuperClass(type1: String, type2: String): String {
        if (isInterface(type1) || isInterface(type2)) return OBJECT
        val ancestors = selfAndSuperClasses(type1)
        return selfAndSuperClasses(type2).firstOrNull { it in ancestors } ?: OBJECT
    }

    /** [name] and the classes it extends, nearest first; a cycle, which no class loader would define, ends it. */
    private fun selfAndSuperClasses(name: String): Set<String> {
        val classes = LinkedHashSet<String>()
        var current: String? = name
        while (current != null && classes.add(current)) {
            current = header(current).superName
        }
        return classes
    }

    private fun header(name: String): ClassHeader = apk[name] ?: hostHeaders.computeIfAbsent(name, ::readHostHeader)

    private data class ClassHeader(val superName: String?, val isInterface: Boolean)

    private companion object {
        const val OBJECT = "java/lang/Object"

        val UNKNOWN = ClassHeader(superName = OBJECT, isInterface = false)

        val hostHeaders = ConcurrentHashMap<String, ClassHeader>()

        fun readHostHeader(name: String): ClassHeader {
            val bytes = try {
                ClassHierarchy::class.java.classLoader.getResourceAsStream("$name.class")?.use { it.readBytes() }
            } catch (_: IOException) {
                null
            } ?: return UNKNOWN
            val reader = ClassReader(bytes)
            return ClassHeader(reader.superName, reader.access and Opcodes.ACC_INTERFACE != 0)
        }
    }
}
