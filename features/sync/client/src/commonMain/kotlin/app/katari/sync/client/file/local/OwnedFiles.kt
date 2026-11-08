package app.katari.sync.client.file.local

import app.katari.sync.client.payload.SyncedFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * The files sync carries as they are in place on this device, read and changed under one lock. Capture compares what
 * it finds with what sync last saw in place, and placement checks a file is still the one it saw before replacing it,
 * so neither acts on a file the other is changing: a person's change made while sync puts another device's file in
 * place is never overwritten unseen, and sync's own writes are never taken for the person's.
 */
@Inject
@SingleIn(AppScope::class)
class OwnedFiles(private val store: LocalFileStore) {
    private val lock = Mutex()

    suspend fun <T> locked(block: suspend Locked.() -> T): T = lock.withLock {
        withContext(Dispatchers.IO) { Locked().block() }
    }

    inner class Locked internal constructor() {
        /** What the owner's file holds now, or null when it has none. */
        fun content(owner: FileOwner): SyncedFile? = store.file(owner)?.let(::contentOf)

        fun file(owner: FileOwner): File? = store.file(owner)

        fun put(owner: FileOwner, source: File) = store.put(owner, source)

        fun remove(owner: FileOwner) = store.remove(owner)

        fun fonts(profileId: Long): List<String> = store.fonts(profileId)
    }

    fun watch(onChange: (FileOwner) -> Unit): AutoCloseable = store.watch(onChange)
}

/** The content of [file], named by its SHA-256 as the protocol names files. */
internal fun contentOf(file: File): SyncedFile {
    val digest = MessageDigest.getInstance("SHA-256")
    var size = 0L
    file.inputStream().use { input ->
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            size += read
        }
    }
    return SyncedFile(sha256 = digest.digest().toHexString(), size = size)
}

private const val BUFFER_BYTES = 64 * 1024
