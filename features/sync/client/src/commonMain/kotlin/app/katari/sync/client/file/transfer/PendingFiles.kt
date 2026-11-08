package app.katari.sync.client.file.transfer

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler

/** How many custom covers and fonts wait to move, as they change. */
@Inject
class PendingFiles(private val handler: DatabaseHandler) {
    /** Files this device holds that the server may lack. */
    fun uploads(): Flow<Long> = handler.subscribeToOne { sync_filesQueries.uploadCount() }

    /** Files records name that aren't here yet, not counting [missingOnServer]. */
    fun downloads(): Flow<Long> = handler.subscribeToOne { sync_filesQueries.downloadCount() }

    fun missingOnServer(): Flow<Long> = handler.subscribeToOne { sync_filesQueries.unavailableCount() }
}
