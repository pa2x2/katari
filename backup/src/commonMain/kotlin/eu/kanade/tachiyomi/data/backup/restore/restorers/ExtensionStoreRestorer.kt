package eu.kanade.tachiyomi.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.toExtensionStore
import tachiyomi.data.Database

@Inject
class ExtensionStoreRestorer(
    private val database: Database,
) {

    suspend operator fun invoke(
        backupStore: BackupExtensionStore,
    ) {
        val store = backupStore.toExtensionStore()
        database.extension_storeQueries.upsert(
            indexUrl = store.indexUrl,
            name = store.name,
            badgeLabel = store.badgeLabel,
            signingKey = store.signingKey,
            contactWebsite = store.contact.website,
            contactDiscord = store.contact.discord,
            format = store.format.key,
            extensionListUrl = store.extensionListUrl,
        )
    }
}
