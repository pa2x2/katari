package mihon.gradle.extensioncompatibility

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File

/*
 * `app/extension-compatibility.json` lists, per extension family, the extension library versions the app loads. The
 * app's loader is generated from it ([GenerateExtensionFamiliesTask]), README.md's table is rewritten from it
 * ([ReadmeExtensionTable]) and the website reads it, so none of them can state a version the loader doesn't run.
 */

internal data class ExtensionFamily(
    /** Lower case; the app's enum constant is its upper case. */
    val id: String,
    val name: String,
    val library: String,
    /** `major.minor`, or `major` alone for libraries numbered that way. Patch versions are never listed. */
    val versions: List<String>,
)

internal fun readExtensionFamilies(file: File): List<ExtensionFamily> {
    val root = JsonSlurper().parse(file) as? Map<*, *> ?: throw GradleException("${file.name} isn't a JSON object")
    val families = root["families"] as? List<*> ?: throw GradleException("${file.name} has no families list")
    return families.map { entry ->
        val family = entry as? Map<*, *> ?: throw GradleException("${file.name} has a family that isn't an object")
        fun text(key: String): String =
            (family[key] as? String)?.takeIf { it.isNotBlank() }
                ?: throw GradleException("A family in ${file.name} has no $key")
        val id = text("id")
        if (!ID.matches(id)) throw GradleException("Family id '$id' in ${file.name} isn't lower-case letters")
        val versions = (family["versions"] as? List<*>)?.map { it as? String }
        if (versions.isNullOrEmpty() || versions.any { it == null || !VERSION.matches(it) }) {
            throw GradleException("Family '$id' in ${file.name} needs versions written as major or major.minor")
        }
        ExtensionFamily(id, text("name"), text("library"), versions.filterNotNull())
    }.also { list ->
        val duplicate = list.groupBy { it.id }.entries.firstOrNull { it.value.size > 1 }?.key
        if (duplicate != null) throw GradleException("Family id '$duplicate' appears twice in ${file.name}")
    }
}

private val ID = Regex("[a-z]+")
private val VERSION = Regex("\\d+(\\.\\d+)?")
