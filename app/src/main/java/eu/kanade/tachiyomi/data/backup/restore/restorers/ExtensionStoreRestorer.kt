package eu.kanade.tachiyomi.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import tachiyomi.data.Database

@Inject
class ExtensionStoreRestorer(
    private val database: Database,
) {

    suspend operator fun invoke(
        backupStore: BackupExtensionStore,
    ) {
        database.extension_storeQueries.upsert(
            indexUrl = backupStore.indexUrl,
            name = backupStore.name,
            badgeLabel = backupStore.badgeLabel ?: backupStore.name,
            signingKey = backupStore.signingKey,
            contactWebsite = backupStore.contactWebsite,
            contactDiscord = backupStore.contactDiscord,
            format = backupStore.format ?: when (backupStore.isLegacy) {
                false -> ExtensionStoreFormat.V2.key
                else -> ExtensionStoreFormat.LEGACY.key
            },
            extensionListUrl = backupStore.extensionListUrl,
        )
    }
}
