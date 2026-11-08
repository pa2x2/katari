package app.katari.gradle.appimage

import org.gradle.api.GradleException
import java.io.File
import java.util.Properties

/** The modules a runtime's `release` file lists. */
internal fun runtimeModules(release: File): Set<String> {
    val modules = releaseProperty(release, "MODULES").split(' ').filter { it.isNotEmpty() }
    if (modules.isEmpty()) throw GradleException("$release lists no MODULES")
    return modules.toSet()
}

/** The Java feature version a runtime's `release` file names, such as `25` for `25.0.4.1`. */
internal fun runtimeFeatureVersion(
    release: File,
): String = releaseProperty(release, "JAVA_VERSION").substringBefore('.')

private fun releaseProperty(release: File, key: String): String {
    val properties = release.inputStream().use { Properties().apply { load(it) } }
    return properties.getProperty(key)?.trim('"') ?: throw GradleException("$release has no $key")
}
