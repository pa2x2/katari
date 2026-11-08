package mihon.desktop.application.installation

import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Checking a downloaded AppImage and putting it in the running one's place. */
internal object AppImageFiles {

    /** Whether [file] is an ELF executable carrying the AppImage type 2 mark, `AI` and 2, at its ninth byte. */
    fun isAppImage(file: File): Boolean {
        val header = ByteArray(HEADER_SIZE)
        val read = try {
            file.inputStream().use { it.readNBytes(header, 0, HEADER_SIZE) }
        } catch (_: IOException) {
            return false
        }
        return read == HEADER_SIZE &&
            header.copyOfRange(0, ELF_MAGIC.size).contentEquals(ELF_MAGIC) &&
            header.copyOfRange(APPIMAGE_MAGIC_OFFSET, HEADER_SIZE).contentEquals(APPIMAGE_TYPE_2)
    }

    /** Throws unless [replace] can write in [target]'s folder. */
    fun checkReplaceable(target: File) {
        if (target.absoluteFile.parentFile?.canWrite() != true) {
            throw AppUpdateException(AppUpdateError.NotReplaceable(target.absolutePath))
        }
    }

    /**
     * Puts [update] in [target]'s place, with [target]'s permissions. It is copied beside [target] first, so a failed
     * copy leaves the running AppImage as it was, then renamed over it: the running AppImage keeps its mounted files,
     * since the file it opened stays until it exits.
     */
    fun replace(target: File, update: File) {
        val next = File(target.absoluteFile.parentFile, ".${target.name}.update")
        try {
            update.copyTo(next, overwrite = true)
            Files.setPosixFilePermissions(next.toPath(), Files.getPosixFilePermissions(target.toPath()))
            Files.move(next.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: IOException) {
            next.delete()
            throw AppUpdateException(AppUpdateError.NotReplaceable(target.absolutePath))
        }
    }

    private val ELF_MAGIC = byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
    private val APPIMAGE_TYPE_2 = byteArrayOf('A'.code.toByte(), 'I'.code.toByte(), 2)
    private const val APPIMAGE_MAGIC_OFFSET = 8
    private const val HEADER_SIZE = 11
}
