package app.katari.sync.server.storage.sqlite

import app.katari.sync.server.file.FileTotals
import app.katari.sync.server.file.StoredFile
import app.katari.sync.server.file.StoredFileIndex

internal class SqliteStoredFileIndex(database: SyncServerDatabase) : StoredFileIndex {
    private val queries = database.stored_filesQueries

    override fun record(file: StoredFile, size: Long, claimedAtMillis: Long) {
        queries.upsert(
            accountId = file.accountId,
            category = file.category,
            sha256 = file.sha256,
            size = size,
            claimedAt = claimedAtMillis,
        )
    }

    override fun claim(file: StoredFile, claimedAtMillis: Long): Boolean = queries.claim(
        claimedAt = claimedAtMillis,
        accountId = file.accountId,
        category = file.category,
        sha256 = file.sha256,
    ).value > 0

    override fun unreferencedClaimedBefore(beforeMillis: Long): List<StoredFile> =
        queries.unreferencedClaimedBefore(beforeMillis, ::StoredFile).executeAsList()

    override fun removeIfUnreferencedClaimedBefore(file: StoredFile, beforeMillis: Long): Boolean =
        queries.deleteIfUnreferencedClaimedBefore(
            accountId = file.accountId,
            category = file.category,
            sha256 = file.sha256,
            before = beforeMillis,
        ).value > 0

    override fun referencedTotals(accountId: Long): FileTotals = queries.referencedTotals(accountId) { count, bytes ->
        FileTotals(files = count, bytes = bytes ?: 0)
    }.executeAsOne()
}
