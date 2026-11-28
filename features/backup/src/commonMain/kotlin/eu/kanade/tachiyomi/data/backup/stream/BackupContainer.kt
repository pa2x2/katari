package eu.kanade.tachiyomi.data.backup.stream

import okio.BufferedSource
import okio.buffer
import okio.gzip

/** How a backup file wraps what it holds, told from its first two bytes. */
internal enum class BackupContainer {
    /** Gzipped protobuf, as Katari and the Mihon and Aniyomi families write backups. */
    GZIP,

    /** A zip archive, as LNReader writes backups. */
    ZIP,

    /** JSON, as Tachiyomi wrote backups before protobuf. */
    JSON,

    /** Anything else, which the Mihon and Aniyomi families' readers take as protobuf that isn't compressed. */
    RAW,
    ;

    companion object {
        /** Throws an [java.io.EOFException] for a file shorter than two bytes. */
        fun of(source: BufferedSource): BackupContainer {
            val peeked = source.peek().apply { require(2) }
            return when (peeked.readShort().toInt()) {
                0x1f8b -> GZIP
                0x504b -> ZIP // `PK`
                0x7b7d, 0x7b22, 0x7b0a -> JSON // `{}`, `{"`, `{\n`
                else -> RAW
            }
        }
    }
}

/** The protobuf a gzipped or raw backup holds, read from [source]'s start. */
internal fun BackupContainer.protobuf(source: BufferedSource): BufferedSource = when (this) {
    BackupContainer.GZIP -> source.gzip().buffer()
    else -> source
}
