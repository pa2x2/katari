package eu.kanade.tachiyomi.data.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.toExtensionStore
import eu.kanade.tachiyomi.source.visualName
import kotlinx.coroutines.CancellationException
import mihon.domain.extension.model.ExtensionStore
import mihon.feature.profiles.core.ProfileBackup
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import tachiyomi.domain.storage.DocumentUri

@Inject
class BackupFileValidator(
    private val backupDecoder: BackupDecoder,
    private val sourceManager: SourceManager,
    private val sourceIdentities: SourceIdentityRepository,
) {

    /** Checks for critical backup file data, such as the sources its entries need that aren't installed. */
    suspend fun validate(uri: DocumentUri): Results {
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
                val name = when {
                    localKey != null && sourceManager.get(localKey) != null -> return@mapNotNull null
                    StubSource.isKnownName(source.name) -> source.name
                    localKey != null -> sourceManager.getDisplayInfo(localKey).visualName()
                    else -> identity.externalId.toString()
                }
                MissingSource(name, identity, summary.librarySourceCounts[source.sourceId] ?: 0)
            }
            .distinctBy { it.identity }
            .sortedBy { it.name }

        return Results(
            missingSources = missingSources,
            extensionStores = backup.backupExtensionStores.map { it.toExtensionStore() },
            profiles = backup.backupProfiles.map { it.profile },
            createdAt = summary.createdAt,
        )
    }

    /** A source the backup's entries come from that isn't installed, and how many of them are in a library. */
    data class MissingSource(val name: String, val identity: SourceIdentity, val libraryEntries: Int)

    /** [profiles] is empty for a backup made before profiles, which restores into the active profile. */
    data class Results(
        val missingSources: List<MissingSource>,
        /** The extension stores the backup restores, which may list what the missing sources need. */
        val extensionStores: List<ExtensionStore>,
        val profiles: List<ProfileBackup>,
        /** When the backup was made, in epoch milliseconds, as restoring it on a synced device counts it. */
        val createdAt: Long,
    )
}
