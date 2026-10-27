package eu.kanade.tachiyomi.data.backup

import android.net.Uri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import mihon.feature.profiles.core.ProfileBackup
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

@Inject
class BackupFileValidator(
    private val backupDecoder: BackupDecoder,
    private val sourceManager: SourceManager,
    private val sourceIdentities: SourceIdentityRepository,
    private val trackingFeature: EntryTrackingFeature,
) {

    /**
     * Checks for critical backup file data.
     *
     * @return List of missing sources or missing trackers.
     */
    suspend fun validate(uri: Uri): Results {
        val summary = try {
            backupDecoder.summarize(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
        val backup = summary.metadata

        // The backup also lists sources only its settings refer to, which restoring doesn't need
        val entrySourceKeys = summary.entrySourceKeys
        val localKeys = sourceIdentities.findAll().associate { it.identity to it.localKey }
        // Opened right after a cold start, every source would otherwise be listed as missing
        sourceManager.awaitInitialized()
        val missingSources = backup.backupSources
            .filter { it.sourceId in entrySourceKeys }
            .mapNotNull { source ->
                val identity = source.identity
                // A source never seen here yet would get its own ID as the key in the schemes that keep it
                val localKey =
                    localKeys[identity] ?: identity.externalId.takeIf { identity.scheme.externalIdIsLocalKey }
                when {
                    localKey != null && sourceManager.get(localKey) != null -> null
                    // Some backups store the id as the name, and sources without a stub were backed up with no name
                    source.name.isNotBlank() && source.name.toLongOrNull() == null -> source.name
                    localKey != null -> sourceManager.getDisplayInfo(localKey).visualName()
                    else -> identity.externalId.toString()
                }
            }
            .distinct()
            .sorted()

        val missingTrackers = trackingFeature.missingLoginNames(
            summary.trackingServiceIds.mapTo(mutableSetOf(), ::EntryTrackingServiceId),
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
