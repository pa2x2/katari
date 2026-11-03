package mihon.entry.interactions.book.download

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32

/** Crash-tolerant, length-framed mutations applied after the compact BOOK index snapshot. */
@OptIn(ExperimentalSerializationApi::class)
internal class BookDownloadIndexJournal(
    private val file: File,
) {
    fun append(mutation: BookDownloadIndexMutation) {
        file.parentFile?.mkdirs()
        val payload = ProtoBuf.encodeToByteArray(mutation)
        require(payload.size <= MAX_RECORD_BYTES) { "BOOK download index mutation is too large" }
        FileOutputStream(file, true).use { fileOutput ->
            DataOutputStream(fileOutput).use { output ->
                output.writeInt(payload.size)
                output.write(payload)
                output.writeInt(payload.crc32())
                output.flush()
                fileOutput.fd.sync()
            }
        }
    }

    fun replay(
        downloadsRootUri: String,
        packages: MutableMap<BookDownloadPackageKey, IndexedBookDownloadPackage>,
    ): BookDownloadIndexJournalReplay {
        if (!file.exists()) return BookDownloadIndexJournalReplay(discardedTail = false)
        var validLength = 0L
        var discardedTail = false
        RandomAccessFile(file, "rw").use { input ->
            while (input.filePointer < input.length()) {
                val recordStart = input.filePointer
                val mutation = runCatching {
                    val size = input.readInt()
                    require(size in 1..MAX_RECORD_BYTES) { "Invalid BOOK download index mutation size" }
                    require(input.length() - input.filePointer >= size + CHECKSUM_BYTES) {
                        "Incomplete BOOK download index mutation"
                    }
                    val payload = ByteArray(size).also(input::readFully)
                    require(input.readInt() == payload.crc32()) { "BOOK download index mutation checksum mismatch" }
                    ProtoBuf.decodeFromByteArray<BookDownloadIndexMutation>(payload).also {
                        require(it.version == CURRENT_VERSION) { "Unsupported BOOK download index mutation version" }
                    }
                }.getOrElse {
                    input.seek(recordStart)
                    discardedTail = true
                    return@use
                }
                if (mutation.downloadsRootUri == downloadsRootUri) {
                    mutation.removals.forEach { packages.remove(it.toPackageKey()) }
                    mutation.upserts.forEach { packages[it.manifest.packageKey] = it }
                }
                validLength = input.filePointer
            }
            if (discardedTail) input.setLength(validLength)
        }
        return BookDownloadIndexJournalReplay(discardedTail)
    }

    fun clear() {
        if (file.exists()) RandomAccessFile(file, "rw").use { it.setLength(0L) }
    }

    fun shouldCompact(): Boolean = file.length() >= COMPACTION_THRESHOLD_BYTES

    private fun ByteArray.crc32(): Int = CRC32().also { it.update(this) }.value.toInt()

    private companion object {
        const val CURRENT_VERSION = 1
        const val MAX_RECORD_BYTES = 4 * 1024 * 1024
        const val CHECKSUM_BYTES = Int.SIZE_BYTES
        const val COMPACTION_THRESHOLD_BYTES = 8L * 1024L * 1024L
    }
}

internal data class BookDownloadIndexJournalReplay(
    val discardedTail: Boolean,
)
