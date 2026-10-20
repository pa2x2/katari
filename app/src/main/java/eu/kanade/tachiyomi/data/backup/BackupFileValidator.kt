package eu.kanade.tachiyomi.data.backup

import android.net.Uri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.source.visualName
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import mihon.feature.profiles.core.ProfileBackup
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

@Inject
class BackupFileValidator(
    private val backupDecoder: BackupDecoder,
    private val sourceManager: SourceManager,
    private val trackingFeature: EntryTrackingFeature,
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    suspend fun validate(uri: Uri): Results {
        val backup = try {
            backupDecoder.decode(uri)
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

        return Results(missingSources, missingTrackers, backup.backupProfiles.map { it.profile })
    }

    /** [profiles] is empty for a backup made before profiles, which restores into the active profile. */
    data class Results(
        val missingSources: List<String>,
        val missingTrackers: List<String>,
        val profiles: List<ProfileBackup>,
    )
}

internal fun Backup.trackingServiceIds(): Set<Long> {
    return allEntries().flatMap { it.tracking }.mapTo(linkedSetOf()) { it.syncId.toLong() }
}
