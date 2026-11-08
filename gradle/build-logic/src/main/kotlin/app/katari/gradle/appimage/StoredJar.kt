package app.katari.gradle.appimage

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Copies [jar] to [target] with every entry stored uncompressed, in the same order and with the same contents, so a
 * signed jar stays valid. The AppImage compresses the jars together, much better than each compressed on its own, and
 * the JVM reads their classes without inflating them a second time.
 */
internal fun copyJarStored(jar: File, target: File) {
    ZipFile(jar).use { zip ->
        ZipOutputStream(target.outputStream().buffered()).use { out ->
            out.setMethod(ZipOutputStream.STORED)
            zip.entries().asSequence().forEach { entry ->
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
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
