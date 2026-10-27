package tachiyomi.domain.entry.service

import java.io.InputStream

/**
 * Saves a user-chosen cover for an Entry. Modules outside the app write custom covers through this so the app's
 * cover cache, which indexes which entries have one, sees every write.
 */
fun interface EntryCustomCoverWriter {
    fun write(entryId: Long, inputStream: InputStream)
}
