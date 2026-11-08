package app.katari.sync.client.file.local

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.payload.SyncedFile
import tachiyomi.data.Database

/**
 * Sets the file [owner]'s record names, null for none, inside the transaction that writes the record's rows. The file
 * itself is put in place, fetched or removed after the transaction ([app.katari.sync.client.file.transfer.FilePlacement]),
 * since a file can't be rolled back with it.
 */
internal suspend fun Database.want(owner: FileOwner, file: SyncedFile?) {
    val category = owner.category.stored
    if (file == null) {
        sync_filesQueries.unwant(category, owner.ref.json)
    } else {
        sync_filesQueries.want(category, owner.ref.json, file.sha256, file.size)
    }
}

/**
 * The file [owner]'s record names as this device knows it: [Known.file] is null when the owner is known to have none,
 * and the result is null when sync knows nothing of the owner's file, such as for a row sync wrote no file for.
 */
internal suspend fun Database.wanted(owner: FileOwner): Known? {
    val row = sync_filesQueries.get(owner.category.stored, owner.ref.json).awaitAsOneOrNull() ?: return null
    return Known(row.wanted_sha256?.let { SyncedFile(it, row.wanted_size) })
}

internal data class Known(val file: SyncedFile?)
