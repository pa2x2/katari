package app.katari.sync.server.file

import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileClaimRequest
import app.katari.sync.protocol.file.FileClaimResponse
import app.katari.sync.server.storage.ServerStorage
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.time.Clock

/**
 * Uploads, claims and downloads of an account's files. The index row is written before a file is moved into place,
 * so a crash between the two leaves a row without a file, which a claim reports as missing and an upload repairs,
 * rather than a file nothing would ever remove.
 */
class FileTransfers(
    private val storage: ServerStorage,
    private val content: FileContentStore,
    private val clock: Clock,
    private val maxFileBytes: Long,
) {
    fun claim(accountId: Long, request: FileClaimRequest): FileClaimResponse {
        val now = clock.now().toEpochMilliseconds()
        val missing = storage.transaction {
            request.files.filterNot { reference ->
                val file = StoredFile(accountId, reference.category, reference.sha256)
                storage.files.claim(file, now) && content.exists(file)
            }
        }
        return FileClaimResponse(missing = missing)
    }

    fun upload(accountId: Long, category: FileCategory, sha256: String, bytes: InputStream): UploadOutcome {
        val upload = content.newUpload()
        try {
            val received = receive(bytes, upload) ?: return UploadOutcome.TooLarge
            if (received.sha256 != sha256) return UploadOutcome.HashMismatch
            val file = StoredFile(accountId, category, sha256)
            storage.transaction { storage.files.record(file, received.size, clock.now().toEpochMilliseconds()) }
            content.moveIntoPlace(upload, file)
            return UploadOutcome.Stored
        } finally {
            upload.deleteIfExists()
        }
    }

    /** Null when the account has no such file. */
    fun download(accountId: Long, category: FileCategory, sha256: String): Path? =
        content.pathOf(StoredFile(accountId, category, sha256)).takeIf { it.toFile().isFile }

    private fun receive(bytes: InputStream, upload: Path): Received? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_BYTES)
        var size = 0L
        upload.outputStream().use { out ->
            while (true) {
                val read = bytes.read(buffer)
                if (read < 0) break
                size += read
                if (size > maxFileBytes) return null
                digest.update(buffer, 0, read)
                out.write(buffer, 0, read)
            }
        }
        return Received(sha256 = digest.digest().toHexString(), size = size)
    }

    private class Received(val sha256: String, val size: Long)

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}

enum class UploadOutcome {
    Stored,

    /** The bytes do not hash to the name they were uploaded under; nothing was kept. */
    HashMismatch,
    TooLarge,
}
