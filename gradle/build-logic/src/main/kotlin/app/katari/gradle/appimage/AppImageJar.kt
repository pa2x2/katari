package app.katari.gradle.appimage

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Copies [jar] to [target] as the AppImage ships it: with every entry stored uncompressed, in the same order, and
 * with its classes' local variable tables left out ([withoutLocalVariableTables]). The AppImage compresses the jars
 * together, much better than each compressed on its own, and the JVM reads their classes without inflating them a
 * second time. A signed jar keeps its classes as they are, so its signatures stay valid.
 */
internal fun copyJarForAppImage(jar: File, target: File) {
    ZipFile(jar).use { zip ->
        val entries = zip.entries().asSequence().toList()
        val signed = entries.any { it.isSignature() }
        ZipOutputStream(target.outputStream().buffered()).use { out ->
            out.setMethod(ZipOutputStream.STORED)
            entries.forEach { entry ->
                val contents = zip.getInputStream(entry).use { it.readBytes() }
                val rewritten = !signed && entry.name.endsWith(".class")
                val bytes = if (rewritten) withoutLocalVariableTables(contents) else contents
                val stored = ZipEntry(entry.name).apply {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = bytes.size.toLong()
                    crc = CRC32().apply { update(bytes) }.value
                    time = entry.time
                    comment = entry.comment
                }
                out.putNextEntry(stored)
                out.write(bytes)
                out.closeEntry()
            }
        }
    }
}

private fun ZipEntry.isSignature() = name.startsWith("META-INF/") && name.endsWith(".SF")
