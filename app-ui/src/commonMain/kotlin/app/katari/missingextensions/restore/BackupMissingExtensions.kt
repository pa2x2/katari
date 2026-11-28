package app.katari.missingextensions.restore

import app.katari.core.platform.PlatformContext
import app.katari.domain.extension.model.ExtensionStore
import app.katari.domain.extension.model.StoreListings
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
import eu.kanade.tachiyomi.source.sourceVisualName
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/**
 * The extensions a backup's library needs that aren't installed, before it's restored: which of them the Library can
 * offer to install afterwards, from the stores added here and, when the restore adds them, the backup's own.
 */
@Inject
class BackupMissingExtensions(
    private val context: PlatformContext,
    private val storeRepository: ExtensionStoreRepository,
    private val compatibility: ExtensionCompatibility,
) {
    /** Reads the lists of the stores added here and the backup's [backupStores]. */
    suspend fun check(sources: List<BackupFileValidator.MissingSource>, backupStores: List<ExtensionStore>): Checked {
        val added = storeRepository.getAll().mapTo(HashSet()) { it.indexUrl }
        val stores = storeRepository.getAll() + backupStores.filter { it.indexUrl !in added }
        return Checked(sources, backupStores, storeRepository.fetchExtensions(stores), added)
    }

    inner class Checked internal constructor(
        private val sources: List<BackupFileValidator.MissingSource>,
        private val backupStores: List<ExtensionStore>,
        private val listings: StoreListings,
        private val addedStores: Set<String>,
    ) {
        /** Checks again, as after a store couldn't be read (D113). */
        suspend fun again(): Checked = check(sources, backupStores)

        /**
         * Grouped by the extension that provides each; [withBackupStores] when the restore adds the backup's stores.
         */
        fun extensions(withBackupStores: Boolean): List<MissingExtension> {
            val listingOf = listingFinder(withBackupStores)
            val nameOf = names(withBackupStores)
            val needed = sources.mapIndexed { index, source ->
                NeededSource(
                    key = index.toLong(),
                    name = nameOf[source.identity] ?: context.stringResource(
                        MR.strings.backup_source_unnamed_numbered,
                        source.identity.externalId.toString(),
                    ),
                    listing = listingOf(source.identity),
                    untrusted = null,
                    countsByProfile = mapOf(0L to source.libraryEntries.toLong()),
                )
            }
            return missingExtensions(needed, storesUnreadable(withBackupStores)) { compatibility.cantInstall(it) }
        }

        /** Why each source's extension can't be installed after restoring, null where it can, by the source. */
        fun cantInstall(withBackupStores: Boolean): Map<SourceIdentity, CantInstall?> {
            val listingOf = listingFinder(withBackupStores)
            val unlisted =
                if (storesUnreadable(withBackupStores)) CantInstall.STORES_UNREADABLE else CantInstall.NOT_LISTED
            return sources.associate { source ->
                val listing = listingOf(source.identity)
                source.identity to if (listing == null) unlisted else compatibility.cantInstall(listing)
            }
        }

        /**
         * What each source is called (D114): as the store listing it names it, with its language, or as the backup's
         * file names it ([BackupFileValidator.MissingSource.name]); null where nothing names it.
         */
        fun names(withBackupStores: Boolean): Map<SourceIdentity, String?> {
            val listingOf = listingFinder(withBackupStores)
            return sources.associate { source ->
                val listed = listingOf(source.identity)?.sources?.find { it.id == source.identity.externalId }
                source.identity to (listed?.let { sourceVisualName(it.name, it.lang) } ?: source.name)
            }
        }

        private fun storesUnreadable(withBackupStores: Boolean): Boolean =
            listings.unreadable.any { withBackupStores || it.indexUrl in addedStores }

        // The listing that would install a source: one that can be installed here if any store has one
        private fun listingFinder(withBackupStores: Boolean): (SourceIdentity) -> Extension.Available? {
            val byIdentity = listings.available
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
