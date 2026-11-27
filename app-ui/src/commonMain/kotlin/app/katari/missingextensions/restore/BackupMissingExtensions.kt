package app.katari.missingextensions.restore

import app.katari.domain.extension.model.ExtensionStore
import app.katari.domain.extension.repository.ExtensionStoreRepository
import app.katari.missingextensions.model.CantInstall
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
            val listingOf = listingFinder(withBackupStores)
            val needed = sources.mapIndexed { index, source ->
                NeededSource(
                    key = index.toLong(),
                    name = source.name,
                    listing = listingOf(source.identity),
                    untrusted = null,
                    countsByProfile = mapOf(0L to source.libraryEntries.toLong()),
                )
            }
            return missingExtensions(needed) { compatibility.cantInstall(it) }
        }

        /** Why each source's extension can't be installed after restoring, null where it can, by the source. */
        fun cantInstall(withBackupStores: Boolean): Map<SourceIdentity, CantInstall?> {
            val listingOf = listingFinder(withBackupStores)
            return sources.associate { source ->
                val listing = listingOf(source.identity)
                source.identity to if (listing == null) CantInstall.NOT_LISTED else compatibility.cantInstall(listing)
            }
        }

        // The listing that would install a source: one that can be installed here if any store has one
        private fun listingFinder(withBackupStores: Boolean): (SourceIdentity) -> Extension.Available? {
            val byIdentity = listings
                .filter { withBackupStores || it.store.indexUrl in addedStores }
                .flatMap { listing -> listing.sources.map { SourceIdentity(listing.sourceIdScheme, it.id) to listing } }
                .groupBy({ it.first }, { it.second })
            return { identity ->
                val candidates = byIdentity[identity].orEmpty()
                candidates.firstOrNull { compatibility.cantInstall(it) == null } ?: candidates.firstOrNull()
            }
        }
    }
}
