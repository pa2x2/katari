package app.katari.sync.server.retention

import app.katari.sync.server.file.FileContentStore
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Removes files no live record lists once [GRACE] has passed since they were last uploaded or claimed, so a file
 * uploaded or claimed for a record still being pushed survives until the push lands.
 */
class UnreferencedFileRetention(
    private val storage: ServerStorage,
    private val content: FileContentStore,
    private val clock: Clock,
) {
    fun sweep() {
        val before = clock.now().minus(GRACE).toEpochMilliseconds()
        storage.files.unreferencedClaimedBefore(before).forEach { file ->
            // Checked again under the write lock: a claim or a push may have come in since the list was read.
            storage.transaction {
                if (storage.files.removeIfUnreferencedClaimedBefore(file, before)) content.delete(file)
            }
        }
    }

    companion object {
        val GRACE = 1.days
    }
}
