package app.katari.sync.server.file

import app.katari.sync.protocol.file.FileCategory

/** Which files each account has in the data directory, and when each was last uploaded or claimed. */
interface StoredFileIndex {
    /** Adds the file, or moves its claim forward if it is already there. */
    fun record(file: StoredFile, size: Long, claimedAtMillis: Long)

    /** Moves the file's claim forward and returns whether the index has it. */
    fun claim(file: StoredFile, claimedAtMillis: Long): Boolean

    /** Files no live record lists that were last claimed before [beforeMillis]. */
    fun unreferencedClaimedBefore(beforeMillis: Long): List<StoredFile>

    /** Removes the file if it is still unreferenced and unclaimed since [beforeMillis], and returns whether it did. */
    fun removeIfUnreferencedClaimedBefore(file: StoredFile, beforeMillis: Long): Boolean

    /** The files the account's live records list that the index has. */
    fun referencedTotals(accountId: Long): FileTotals

    fun removeAccount(accountId: Long)
}

data class StoredFile(
    val accountId: Long,
    val category: FileCategory,
    val sha256: String,
)

data class FileTotals(
    val files: Long,
    val bytes: Long,
)
