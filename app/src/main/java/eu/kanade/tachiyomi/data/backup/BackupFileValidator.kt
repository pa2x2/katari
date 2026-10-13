package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.source.visualName
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BackupFileValidator(
    private val context: Context,

    private val sourceManager: SourceManager = Injekt.get(),
    private val trackingFeature: EntryTrackingFeature = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    suspend fun validate(uri: Uri): Results {
        val backup = try {
            BackupDecoder(context).decode(uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = backup.backupSources.associate { it.sourceId to it.name }
        // Opened right after a cold start, every source would otherwise be listed as missing
        sourceManager.awaitInitialized()
        val missingSources = sources
            .filterKeys { sourceManager.get(it) == null }
            .map { (id, name) ->
                // Some backups store the id as the name, and sources without a stub were backed up with no name
                if (name.isBlank() || name.toLongOrNull() != null) {
                    sourceManager.getDisplayInfo(id).visualName()
                } else {
                    name
                }
            }
            .distinct()
            .sorted()

        val missingTrackers = trackingFeature.missingLoginNames(
            backup.trackingServiceIds().mapTo(mutableSetOf(), ::EntryTrackingServiceId),
        )

        return Results(missingSources, missingTrackers)
    }

    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
    )
}

internal fun Backup.trackingServiceIds(): Set<Long> {
    return allEntries().flatMap { it.tracking }.mapTo(linkedSetOf()) { it.syncId.toLong() }
}
