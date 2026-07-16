import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal const val STALE_XMLUTIL_SERVICE =
    "META-INF/services/nl.adaptivity.xmlutil.util.SerializationProvider"

/** Removes invalid dependency metadata from merged Java resources before R8 analyzes them. */
class PluginArtifactSanitizer : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        tasks.matching { task ->
            task.name.startsWith("merge") && task.name.endsWith("JavaResource")
        }.configureEach {
            doLast {
                outputs.files.asFileTree
                    .filter { output -> output.isFile && output.extension == "jar" }
                    .forEach(::sanitizeMergedJavaResources)
            }
        }
    }
}

internal fun sanitizeMergedJavaResources(archive: File): Boolean {
    val containsStaleXmlUtilService = ZipFile(archive).use { zip ->
        zip.getEntry(STALE_XMLUTIL_SERVICE) != null
    }
    if (!containsStaleXmlUtilService) return false

    val sanitized = File.createTempFile("${archive.nameWithoutExtension}-", ".jar", archive.parentFile)
    try {
        removeZipEntry(archive.inputStream(), sanitized.outputStream(), STALE_XMLUTIL_SERVICE)
        Files.move(
            sanitized.toPath(),
            archive.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    } finally {
        sanitized.delete()
    }
    return true
}

internal fun removeZipEntry(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    excludedEntry: String,
) {
    ZipInputStream(input.buffered()).use { source ->
        ZipOutputStream(output.buffered()).use { target ->
            generateSequence(source::getNextEntry).forEach { entry ->
                if (entry.name != excludedEntry) {
                    target.putNextEntry(
                        ZipEntry(entry.name).apply {
                            comment = entry.comment
                            extra = entry.extra
                            method = entry.method
                            if (entry.method == ZipEntry.STORED) {
                                size = entry.size
                                compressedSize = entry.compressedSize
                                crc = entry.crc
                            }
                        },
                    )
                    source.copyTo(target)
                    target.closeEntry()
                }
                source.closeEntry()
            }
        }
    }
}
