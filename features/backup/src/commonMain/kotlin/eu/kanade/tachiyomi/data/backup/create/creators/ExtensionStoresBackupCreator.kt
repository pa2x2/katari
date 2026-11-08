package eu.kanade.tachiyomi.data.backup.create.creators

import app.katari.domain.extension.interactor.GetExtensionStores
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.backupExtensionStoreMapper

@Inject
class ExtensionStoresBackupCreator(
    private val getExtensionStores: GetExtensionStores,
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupExtensionStoreMapper)
    }
}
