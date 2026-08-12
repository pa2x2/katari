package mihon.entry.interactions.book.download

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32

/** Durable BOOK queue snapshot with incremental completion/cancellation removals. */
@OptIn(ExperimentalSerializationApi::class)
internal class BookDownloadQueueFileBackend(
    private val snapshotFile: File,
    private val legacyPreferences: SharedPreferences,
) : BookDownloadStoreBackend {
    private val journalFile = File(snapshotFile.path + JOURNAL_SUFFIX)
    private var currentGeneration: Long? = null

    @Synchronized
    override fun values(): Map<String, *> {
        val snapshot = loadSnapshot()
        val values = snapshot.values.toMutableMap()
        replayRemovals(snapshot.generation, values)
        return values
    }

    @Synchronized
    override fun putAll(values: Map<String, String>) {
        val current = this.values().mapValues { (_, value) -> value as String }.toMutableMap()
        current.putAll(values)
        replace(current)
    }

    @Synchronized
    override fun replace(values: Map<String, String>) {
        val nextGeneration = Math.incrementExact(generation())
        writeSnapshot(BookDownloadQueueSnapshot(generation = nextGeneration, values = values))
        currentGeneration = nextGeneration
        clearJournal()
    }

    @Synchronized
    override fun remove(keys: Set<String>) {
        if (keys.isEmpty()) return
        journalFile.parentFile?.mkdirs()
        val payload = ProtoBuf.encodeToByteArray(
            BookDownloadQueueRemoval(
                snapshotGeneration = generation(),
                keys = keys.toList(),
            ),
        )
        FileOutputStream(journalFile, true).use { fileOutput ->
            DataOutputStream(fileOutput).use { output ->
                output.writeInt(payload.size)
                output.write(payload)
                output.writeInt(payload.crc32())
                output.flush()
                fileOutput.fd.sync()
            }
        }
    }

    @Synchronized
    override fun clear() {
        BookDownloadAtomicFile(snapshotFile).delete()
        journalFile.delete()
        legacyPreferences.edit { clear() }
        currentGeneration = null
    }

    private fun loadSnapshot(): BookDownloadQueueSnapshot {
        val snapshot = readSnapshot() ?: migrateLegacyValues()
        currentGeneration = snapshot.generation
        return snapshot
    }

    private fun generation(): Long = currentGeneration ?: loadSnapshot().generation

    private fun migrateLegacyValues(): BookDownloadQueueSnapshot {
        val migrated = legacyPreferences.all.mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        }.toMap(linkedMapOf())
        val snapshot = BookDownloadQueueSnapshot(values = migrated)
        if (migrated.isNotEmpty()) {
            writeSnapshot(snapshot)
            legacyPreferences.edit { clear() }
        }
        return snapshot
    }

    private fun readSnapshot(): BookDownloadQueueSnapshot? {
        val atomicFile = BookDownloadAtomicFile(snapshotFile)
        if (!atomicFile.exists()) return null
        return runCatching {
            atomicFile.openRead().use {
                ProtoBuf.decodeFromByteArray<BookDownloadQueueSnapshot>(it.readBytes())
            }.also { require(it.version == CURRENT_VERSION) }
        }.getOrNull()
    }

    private fun writeSnapshot(snapshot: BookDownloadQueueSnapshot) {
        snapshotFile.parentFile?.mkdirs()
        BookDownloadAtomicFile(snapshotFile).write(ProtoBuf.encodeToByteArray(snapshot))
    }

    private fun replayRemovals(snapshotGeneration: Long, values: MutableMap<String, String>) {
        if (!journalFile.exists()) return
        var validLength = 0L
        var discardedTail = false
        RandomAccessFile(journalFile, "rw").use { input ->
            while (input.filePointer < input.length()) {
                val recordStart = input.filePointer
                val removal = runCatching {
                    val size = input.readInt()
                    require(size in 1..MAX_RECORD_BYTES)
                    require(input.length() - input.filePointer >= size + CHECKSUM_BYTES)
                    val payload = ByteArray(size).also(input::readFully)
                    require(input.readInt() == payload.crc32())
                    ProtoBuf.decodeFromByteArray<BookDownloadQueueRemoval>(payload).also {
                        require(it.version == CURRENT_VERSION)
                    }
                }.getOrElse {
                    input.seek(recordStart)
                    discardedTail = true
                    return@use
                }
                if (removal.snapshotGeneration == snapshotGeneration) {
                    removal.keys.forEach(values::remove)
                }
                validLength = input.filePointer
            }
            if (discardedTail) input.setLength(validLength)
        }
    }

    private fun clearJournal() {
        if (journalFile.exists()) RandomAccessFile(journalFile, "rw").use { it.setLength(0L) }
    }

    private fun ByteArray.crc32(): Int = CRC32().also { it.update(this) }.value.toInt()

    private companion object {
        const val CURRENT_VERSION = 1
        const val JOURNAL_SUFFIX = ".journal"
        const val MAX_RECORD_BYTES = 4 * 1024 * 1024
        const val CHECKSUM_BYTES = Int.SIZE_BYTES
    }
}

@Serializable
private data class BookDownloadQueueSnapshot(
    val version: Int = 1,
    val generation: Long = 0,
    val values: Map<String, String>,
)

@Serializable
private data class BookDownloadQueueRemoval(
    val version: Int = 1,
    val snapshotGeneration: Long = 0,
    val keys: List<String>,
)
