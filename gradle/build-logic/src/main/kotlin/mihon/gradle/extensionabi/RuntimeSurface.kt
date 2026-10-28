package mihon.gradle.extensionabi

import japicmp.model.JApiClass
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtField
import javassist.CtMember
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.AccessFlag
import javassist.bytecode.InnerClassesAttribute

/**
 * What an extension compiled against these classes can link to, as R8's `seeds.txt` lines: a class line, then
 * `Class: type field`, `Class: returnType method(parameters)` and `Class: SimpleName(parameters)` for constructors.
 * Only an element a keep rule matches is left exactly as compiled, so each of these must appear in the seeds.
 *
 * Anonymous and synthetic classes are left out: Kotlin copies the ones inline functions use into the caller, and no
 * extension can name the rest.
 */
internal fun runtimeSurfaceOf(jApiClass: JApiClass): List<String> {
    val ctClass = jApiClass.newClass.orElse(null) ?: return emptyList()
    return surfaceLines(
        ctClass,
        fields = jApiClass.fields.mapNotNull { it.newFieldOptional.orElse(null) },
        methods = jApiClass.methods.mapNotNull { it.newMethod.orElse(null) },
        constructors = jApiClass.constructors.mapNotNull { it.newConstructor.orElse(null) },
    )
}

/** The same lines for every member [ctClass] declares, which is what a class file read on its own offers to link. */
internal fun declaredSurfaceOf(ctClass: CtClass): List<String> = surfaceLines(
    ctClass,
    fields = ctClass.declaredFields.toList(),
    methods = ctClass.declaredMethods.toList(),
    constructors = ctClass.declaredConstructors.toList(),
)

private fun surfaceLines(
    ctClass: CtClass,
    fields: List<CtField>,
    methods: List<CtMethod>,
    constructors: List<CtConstructor>,
): List<String> {
    if (!ctClass.isLinkable()) return emptyList()
    val className = ctClass.name
    val fieldLines = fields.filter { it.isLinkable() }
        .map { "$className: ${javaTypeName(it.fieldInfo2.descriptor)} ${it.name}" }
    val methodLines = methods.filter { it.isLinkable() }
        .map { "$className: ${javaReturnType(it.signature)} ${it.name}${javaParameters(it.signature)}" }
    val constructorLines = constructors.filter { it.isLinkable() }
        .map { "$className: ${className.substringAfterLast('.')}${javaParameters(it.signature)}" }
    return listOf(className) + fieldLines + methodLines + constructorLines
}

private fun CtClass.isLinkable(): Boolean {
    val flags = classFile2.accessFlags
    if (flags and AccessFlag.SYNTHETIC != 0) return false
    if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) return false
    val innerClasses = classFile2.getAttribute(InnerClassesAttribute.tag) as InnerClassesAttribute?
        ?: return true
    return (0..<innerClasses.tableLength()).none { entry ->
        innerClasses.innerClass(entry) == name && innerClasses.innerName(entry) == null
    }
}

private fun CtMember.isLinkable() = Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)

private fun javaParameters(descriptor: String): String {
    val types = mutableListOf<String>()
    var index = 1
    while (descriptor[index] != ')') {
        val end = typeEnd(descriptor, index)
        types += javaTypeName(descriptor.substring(index, end))
        index = end
    }
    return types.joinToString(",", "(", ")")
}

private fun javaReturnType(descriptor: String) = javaTypeName(descriptor.substringAfter(')'))

private fun typeEnd(descriptor: String, start: Int): Int {
    var index = start
    while (descriptor[index] == '[') index++
    return if (descriptor[index] == 'L') descriptor.indexOf(';', index) + 1 else index + 1
}

private fun javaTypeName(descriptor: String): String {
    val dimensions = descriptor.takeWhile { it == '[' }.length
    val element = when (val type = descriptor.substring(dimensions)) {
        "Z" -> "boolean"
        "B" -> "byte"
        "C" -> "char"
        "S" -> "short"
        "I" -> "int"
        "J" -> "long"
        "F" -> "float"
        "D" -> "double"
        "V" -> "void"
        else -> type.removePrefix("L").removeSuffix(";").replace('/', '.')
    }
    return element + "[]".repeat(dimensions)
}
