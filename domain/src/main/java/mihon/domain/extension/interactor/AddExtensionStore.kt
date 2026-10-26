package mihon.domain.extension.interactor

import dev.zacsweers.metro.Inject
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository

/**
 * Adding a store trusts what it lists, so it's read first and added once the user agreed to what it is: a store that
 * doesn't sign its extensions asks for a confirmation of its own.
 */
@Inject
class AddExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    suspend fun fetch(indexUrl: String): Result<ExtensionStore> = repository.fetch(indexUrl)

    suspend fun add(store: ExtensionStore) {
        repository.insert(store)
    }
}
