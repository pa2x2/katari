package mihon.core.common.storage

import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Replaces the file at [path] with [bytes] through a synced temporary file beside it and an atomic rename, so a crash
 * or power loss mid-write leaves the previous file whole. The file is readable by its owner only, as the temporary
 * file is created.
 */
@Throws(IOException::class)
fun replaceAtomically(path: Path, bytes: ByteArray) {
    val directory = path.toAbsolutePath().parent
    Files.createDirectories(directory)
    val temporary = Files.createTempFile(directory, path.fileName.toString(), ".tmp")
    try {
        FileOutputStream(temporary.toFile()).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}
