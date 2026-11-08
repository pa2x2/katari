package app.katari.sync.protocol.file

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A file named by its content, which is how records refer to files and how files are uploaded and downloaded. The
 * same file from two devices is the same reference, so the server stores it once.
 */
@Serializable
data class FileReference(
    @ProtoNumber(1) val category: FileCategory,
    /** SHA-256 of the file's bytes, as 64 lowercase hex digits; the server checks an upload against it. */
    @ProtoNumber(2) val sha256: String,
    /** In bytes. */
    @ProtoNumber(3) val size: Long,
)

/** Where the server files a reference, since each category has a folder of its own. */
@Serializable
enum class FileCategory {
    @ProtoNumber(1)
    COVER,

    @ProtoNumber(2)
    FONT,
}
