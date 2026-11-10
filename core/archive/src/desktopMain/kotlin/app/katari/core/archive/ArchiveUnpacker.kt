package app.katari.core.archive

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * Unpacks the archive at [archive] into [directory], which it creates: its folders and its files, each file one its
 * owner may run where the archive says so. Throws an [IOException] for an archive that can't be read, one with an entry
 * of another kind (a link, a device) and one with a name that leads out of [directory], and leaves what it had unpacked
 * until then.
 */
fun unpackArchive(archive: Path, directory: Path) {
    Files.createDirectories(directory)
    LibarchiveInputStream(libarchive(), archive).use { input ->
        while (true) {
            val entry = input.getNextEntry() ?: break
            val target = unpackTarget(directory, entry.name)
            when (input.entryFiletype) {
                AE_IFDIR -> Files.createDirectories(target)
                AE_IFREG -> {
                    Files.createDirectories(target.parent)
                    Files.copy(input, target)
                    if (input.entryIsExecutable) {
                        Files.setPosixFilePermissions(target, Files.getPosixFilePermissions(target) + RUN)
                    }
                }
                else -> throw IOException("${entry.name} in ${archive.fileName} is neither a file nor a folder")
            }
        }
    }
}

/** Where the entry named [name] goes under [directory]; throws an [IOException] for a name that leads out of it. */
internal fun unpackTarget(directory: Path, name: String): Path {
    val target = directory.resolve(name).normalize()
    if (name.startsWith("/") || !target.startsWith(directory.normalize())) {
        throw IOException("$name leads out of the folder it is unpacked into")
    }
    return target
}

private val RUN = setOf(
    PosixFilePermission.OWNER_EXECUTE,
    PosixFilePermission.GROUP_EXECUTE,
    PosixFilePermission.OTHERS_EXECUTE,
)
