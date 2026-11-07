package app.katari.sync.client.payload

import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A file a record names by its content, never by where a device keeps it: devices put it under their own local IDs,
 * and the server stores it once however many records name it. The field holding it says which kind of file it is.
 */
@Serializable
data class SyncedFile(
    /** SHA-256 of the file's bytes, as 64 lowercase hex digits. */
    @ProtoNumber(1) val sha256: String,
    /** In bytes. */
    @ProtoNumber(2) val size: Long,
) : Comparable<SyncedFile> {
    fun reference(category: FileCategory): FileReference = FileReference(category, sha256, size)

    // Two devices' files tie only at a first sync, where neither was edited while syncing: any fixed order does.
    override fun compareTo(other: SyncedFile): Int = compareValuesBy(this, other, SyncedFile::sha256, SyncedFile::size)
}
