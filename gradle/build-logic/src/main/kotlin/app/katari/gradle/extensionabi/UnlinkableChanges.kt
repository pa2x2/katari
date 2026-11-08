package app.katari.gradle.extensionabi

import japicmp.model.JApiClass
import japicmp.model.JApiCompatibility

/**
 * Marks binary compatible every change to what no extension can link to, which [runtimeSurfaceOf] leaves out for the
 * same reason: anonymous and synthetic classes, and the synthetic accessors (`access$…`) a class gets for the private
 * members of its file. The compiler names them after the code around them, so they come and go with any change to that
 * code, a newer compiler included, while what extensions link to stays. A class or member that was linkable is checked
 * as before, however it changed.
 */
internal fun acceptUnlinkableChanges(classes: List<JApiClass>) {
    classes.forEach { jApiClass ->
        val oldClass = jApiClass.oldClass.orElse(null) ?: return@forEach
        if (!oldClass.isLinkable()) {
            val members = jApiClass.fields + jApiClass.methods + jApiClass.constructors + jApiClass.interfaces
            (listOf(jApiClass, jApiClass.superclass) + members).forEach { it.accept() }
            return@forEach
        }
        jApiClass.methods
            .filter { method -> method.oldMethod.orElse(null)?.isLinkable() == false }
            .forEach { it.accept() }
    }
}

private fun JApiCompatibility.accept() {
    compatibilityChanges.forEach { it.setBinaryCompatible(true) }
}
