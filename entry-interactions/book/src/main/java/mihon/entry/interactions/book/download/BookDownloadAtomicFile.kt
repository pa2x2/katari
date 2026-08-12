package mihon.entry.interactions.book.download

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/** Small host-testable atomic file primitive that retains the last complete value after write failure. */
internal class BookDownloadAtomicFile(
    private val base: File,
) {
    private val pending = File(base.path + PENDING_SUFFIX)
    private val backup = File(base.path + BACKUP_SUFFIX)

    fun exists(): Boolean = base.exists() || backup.exists()

    fun openRead(): InputStream {
        recoverBackup()
        return FileInputStream(base)
    }

    fun write(bytes: ByteArray) {
        base.parentFile?.mkdirs()
        FileOutputStream(pending).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }

        if (backup.exists() && !backup.delete()) {
            pending.delete()
            error("Unable to replace stale BOOK metadata backup")
        }
        if (base.exists() && !base.renameTo(backup)) {
            pending.delete()
            error("Unable to preserve previous BOOK metadata")
        }
        if (!pending.renameTo(base)) {
            backup.renameTo(base)
            pending.delete()
            error("Unable to publish BOOK metadata")
        }
        backup.delete()
    }

    fun delete() {
        pending.delete()
        base.delete()
        backup.delete()
    }

    private fun recoverBackup() {
        pending.delete()
        if (!backup.exists()) return
        if (base.exists()) {
            backup.delete()
        } else {
            check(backup.renameTo(base)) { "Unable to restore BOOK metadata backup" }
        }
    }

    companion object {
        const val BACKUP_SUFFIX = ".bak"
        private const val PENDING_SUFFIX = ".new"
    }
}
