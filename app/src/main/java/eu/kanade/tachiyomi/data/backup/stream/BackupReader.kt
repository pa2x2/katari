package eu.kanade.tachiyomi.data.backup.stream

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupAnime
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupManga
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import okio.BufferedSource

/**
 * An entry as the backup encodes it, at its top level when [profileIndex] is null or in the profile at that index of
 * [Backup.backupProfiles] otherwise.
 */
internal class EncodedBackupEntry(
    val profileIndex: Int?,
    val format: BackupEntryFormat,
    private val bytes: ByteArray,
    private val parser: ProtoBuf,
) {
    fun decode(): BackupEntry = when (format) {
        BackupEntryFormat.ENTRY -> decode(BackupEntry.serializer())
        BackupEntryFormat.LEGACY_MANGA -> decode(LegacyBackupManga.serializer()).toBackupEntry()
        BackupEntryFormat.LEGACY_ANIME -> decode(LegacyBackupAnime.serializer()).toBackupEntry()
    }

    /** Decodes the entry as [deserializer], which can declare only the fields it needs so the rest are skipped. */
    fun <T> decode(deserializer: DeserializationStrategy<T>): T = parser.decodeBackup(deserializer, bytes)
}

/**
 * Reads an encoded [Backup] without holding all of its entries at once: they are handed over one at a time, wherever
 * they appear, and everything else is set aside and decoded at the end.
 *
 * Malformed or truncated input throws a [MalformedBackupException].
 */
internal class BackupReader(
    source: BufferedSource,
    private val parser: ProtoBuf,
) {
    private val input = ProtoInput(source)

    /**
     * Reads the whole backup, handing each entry [include] accepts by its profile index to [onEntry], and returns the
     * rest: the backup without entries, its profiles included without theirs.
     */
    suspend fun read(
        include: (profileIndex: Int?) -> Boolean,
        onEntry: suspend (EncodedBackupEntry) -> Unit,
    ): Backup {
        val metadata = Buffer()
        var profileIndex = 0
        while (!input.exhausted()) {
            val tag = input.readTag()
            if (tag == BackupLayout.PROFILE_TAG) {
                readProfile(profileIndex++, metadata, include, onEntry)
                continue
            }
            when (val format = BackupLayout.TOP_LEVEL_ENTRY_TAGS[tag]) {
                null -> input.copyField(tag, metadata)
                else -> readEntry(null, format, end = null, include, onEntry)
            }
        }
        return parser.decodeBackup(Backup.serializer(), metadata.readByteArray())
    }

    private suspend fun readProfile(
        profileIndex: Int,
        metadata: Buffer,
        include: (Int?) -> Boolean,
        onEntry: suspend (EncodedBackupEntry) -> Unit,
    ) {
        val end = input.readLength() + input.position
        val profileMetadata = Buffer()
        while (input.position < end) {
            val tag = input.readTag()
            when (val format = BackupLayout.PROFILE_ENTRY_TAGS[tag]) {
                null -> input.copyField(tag, profileMetadata)
                else -> readEntry(profileIndex, format, end, include, onEntry)
            }
            if (input.position > end) throw MalformedBackupException("A field runs past the end of its profile")
        }
        metadata.writeVarint(BackupLayout.PROFILE_TAG)
        metadata.writeVarint(profileMetadata.size)
        metadata.writeAll(profileMetadata)
    }

    /** @param end where the message holding the entry ends, or null at the top level. */
    private suspend fun readEntry(
        profileIndex: Int?,
        format: BackupEntryFormat,
        end: Long?,
        include: (Int?) -> Boolean,
        onEntry: suspend (EncodedBackupEntry) -> Unit,
    ) {
        val length = input.readLength()
        if (end != null && input.position + length > end) {
            throw MalformedBackupException("An entry runs past the end of its profile")
        }
        if (include(profileIndex)) {
            onEntry(EncodedBackupEntry(profileIndex, format, input.readByteArray(length), parser))
        } else {
            input.skip(length)
        }
    }
}

internal fun <T> ProtoBuf.decodeBackup(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
    return try {
        decodeFromByteArray(deserializer, bytes)
    } catch (e: SerializationException) {
        throw MalformedBackupException(e.message, e)
    }
}
