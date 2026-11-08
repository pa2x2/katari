package app.katari.sync.client.restore

import app.cash.sqldelight.async.coroutines.awaitAsOne
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/**
 * Rolling the whole account back to a backup (D046a), as [RestoreStamping] describes: the person's data in the
 * profiles the backup holds is emptied here, then the backup is restored, both captured as edits made now. Every
 * device applies the emptying and then the restore like any other change, so each ends with the backup's data in
 * those profiles. Profiles the backup doesn't hold are left as they are, and reading statistics stay, being kept as
 * last synced everywhere.
 */
@Inject
class AccountRollback(private val handler: DatabaseHandler) {
    /** Empties [profileIds] and runs [restore]; without sync only runs it, as there is no account to roll back. */
    suspend fun <T> rollBack(profileIds: Set<Long>, restore: suspend () -> T): T {
        val state = handler.await { sync_stateQueries.get().awaitAsOne() }
        if (state.enabled && profileIds.isNotEmpty()) empty(profileIds)
        return restore()
    }

    private suspend fun empty(profileIds: Set<Long>) = handler.await(inTransaction = true) {
        with(sync_rollbackQueries) {
            deleteCategoryMemberships(profileIds)
            deleteCategoryUpdateRules(profileIds)
            deleteCategories(profileIds)
            deleteMerges(profileIds)
            deleteTrackerBindings(profileIds)
            deleteExcludedScanlators(profileIds)
            deleteUpdateModes(profileIds)
            deleteViewerSettings(profileIds)
            deleteDownloadPreferences(profileIds)
            deleteTranslationLanguages(profileIds)
            deletePlaybackPreferences(profileIds)
            deleteIntroTimesPicks(profileIds)
            deleteHistory(profileIds)
            deleteProgress(profileIds)
            clearChapterMarks(profileIds)
            leaveLibrary(profileIds)
        }
    }
}
