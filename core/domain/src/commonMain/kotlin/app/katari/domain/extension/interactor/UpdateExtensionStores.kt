package app.katari.domain.extension.interactor

import app.katari.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject

@Inject
class UpdateExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke() {
        repository.refreshAll()
    }
}
