package eu.kanade.tachiyomi.data.backup.stream

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.feature.profiles.core.ProfileScopedBackup
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

/**
 * Writes an encoded [Backup] without holding all of its entries at once, which decodes to the same [Backup] as
 * encoding it whole would give.
 *
 * A profile is a message of its own, which has to start with its length, so its entries are written to a file in
 * [scratchDir] until they are all known and then copied after the profile's other fields.
 */
internal class BackupWriter(
    private val sink: BufferedSink,
    private val parser: ProtoBuf,
    private val scratchDir: File,
) {
    var isEmpty = true
        private set

    /** Writes an entry of a backup made without profiles. */
    fun writeEntry(entry: BackupEntry) {
        sink.writeEntry(BackupLayout.TOP_LEVEL_ENTRY_TAG, entry)
        isEmpty = false
    }

    /**
     * Writes [profile], whose own entries are left out, with the entries [writeEntries] hands to the function it
     * is given.
     */
    suspend fun writeProfile(
        profile: ProfileScopedBackup,
        writeEntries: suspend (writeEntry: (BackupEntry) -> Unit) -> Unit,
    ) {
        val scratch = File.createTempFile("backup-profile", null, scratchDir)
        try {
            scratch.sink().buffer().use { entries ->
                writeEntries { entry -> entries.writeEntry(BackupLayout.PROFILE_ENTRY_TAG, entry) }
            }
            val fields = parser.encodeToByteArray(ProfileScopedBackup.serializer(), profile.copy(entries = emptyList()))
            sink.writeVarint(BackupLayout.PROFILE_TAG)
            sink.writeVarint(fields.size + scratch.length())
            sink.write(fields)
            scratch.source().use(sink::writeAll)
            isEmpty = false
        } finally {
            scratch.delete()
        }
    }

    /** Writes everything but the entries and profiles, which have to be written already. */
    fun writeMetadata(metadata: Backup) {
        val bytes = parser.encodeToByteArray(
            Backup.serializer(),
            metadata.copy(backupEntries = emptyList(), backupProfiles = emptyList()),
        )
        sink.write(bytes)
        if (bytes.isNotEmpty()) isEmpty = false
    }

    private fun BufferedSink.writeEntry(tag: Long, entry: BackupEntry) {
        val bytes = parser.encodeToByteArray(BackupEntry.serializer(), entry)
        writeVarint(tag)
        writeVarint(bytes.size.toLong())
        write(bytes)
    }
}
