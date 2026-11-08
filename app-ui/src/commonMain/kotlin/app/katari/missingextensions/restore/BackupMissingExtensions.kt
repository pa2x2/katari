package app.katari.missingextensions.restore

import app.katari.domain.extension.model.ExtensionStore
import app.katari.domain.extension.repository.ExtensionStoreRepository
import app.katari.missingextensions.model.MissingExtension
import app.katari.missingextensions.model.NeededSource
import app.katari.missingextensions.model.cantInstall
import app.katari.missingextensions.model.missingExtensions
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.extension.kind.ExtensionCompatibility
import eu.kanade.tachiyomi.extension.model.Extension
import tachiyomi.domain.source.identity.SourceIdentity

/**
 * The extensions a backup's library needs that aren't installed, before it's restored: which of them the Library can
 * offer to install afterwards, from the stores added here and, when the restore adds them, the backup's own.
 */
@Inject
class BackupMissingExtensions(
    private val storeRepository: ExtensionStoreRepository,
    private val compatibility: ExtensionCompatibility,
) {
    /** Reads the lists of the stores added here and the backup's; a store that can't be read lists nothing. */
    suspend fun check(sources: List<BackupFileValidator.MissingSource>, backupStores: List<ExtensionStore>): Checked {
        val added = storeRepository.getAll().mapTo(HashSet()) { it.indexUrl }
        val stores = storeRepository.getAll() + backupStores.filter { it.indexUrl !in added }
        return Checked(sources, storeRepository.fetchExtensions(stores), added)
    }

    inner class Checked internal constructor(
        private val sources: List<BackupFileValidator.MissingSource>,
        private val listings: List<Extension.Available>,
        private val addedStores: Set<String>,
    ) {
        /** Grouped by the extension that provides each; [withBackupStores] when the restore adds the backup's stores. */
        fun extensions(withBackupStores: Boolean): List<MissingExtension> {
            val byIdentity = listings
                .filter { withBackupStores || it.store.indexUrl in addedStores }
                .flatMap { listing -> listing.sources.map { SourceIdentity(listing.sourceIdScheme, it.id) to listing } }
                .groupBy({ it.first }, { it.second })
            val needed = sources.mapIndexed { index, source ->
                val candidates = byIdentity[source.identity].orEmpty()
                NeededSource(
                    key = index.toLong(),
                    name = source.name,
                    listing =
                    candidates.firstOrNull { compatibility.cantInstall(it) == null } ?: candidates.firstOrNull(),
                    untrusted = null,
                    countsByProfile = mapOf(0L to source.libraryEntries.toLong()),
                )
            }
            return missingExtensions(needed) { compatibility.cantInstall(it) }
        }
    }
}
