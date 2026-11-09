package app.katari.sync.client.file.transfer

import app.katari.sync.client.file.local.dataGroup
import app.katari.sync.client.file.local.stored
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.file.FileCategory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.DatabaseHandler

/** How many custom covers and fonts wait to move, as they change. */
@Inject
class PendingFiles(private val handler: DatabaseHandler, private val preferences: SyncPreferences) {
    /** Files this device holds that the server may lack. */
    fun uploads(): Flow<Long> = handler.subscribeToOne { sync_filesQueries.uploadCount() }

    /**
     * Files records name that aren't here yet, not counting [missingOnServer], nor files of a kind this device doesn't
     * sync, which it never fetches.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun downloads(): Flow<Long> = preferences.dataOff.changes().flatMapLatest {
        val choices = preferences.choices()
        val categories = FileCategory.entries.filter { choices.syncs(it.dataGroup) }.map { it.stored }
        handler.subscribeToOne { sync_filesQueries.downloadCount(categories) }
    }

    fun missingOnServer(): Flow<Long> = handler.subscribeToOne { sync_filesQueries.unavailableCount() }
}
