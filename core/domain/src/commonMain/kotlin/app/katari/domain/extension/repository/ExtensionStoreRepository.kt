package app.katari.domain.extension.repository

import app.katari.domain.extension.model.ExtensionStore
import app.katari.domain.extension.model.StoreListings
import kotlinx.coroutines.flow.Flow

interface ExtensionStoreRepository {
    /** Reads the store at [indexUrl] without adding it. */
    suspend fun fetch(indexUrl: String): Result<ExtensionStore>

    suspend fun insert(store: ExtensionStore)

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun refreshAll()

    suspend fun fetchExtensions(): StoreListings

    /** What [stores] list, whether or not they're added. */
    suspend fun fetchExtensions(stores: List<ExtensionStore>): StoreListings

    suspend fun getAll(): List<ExtensionStore>

    fun getAllAsFlow(): Flow<List<ExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)
}
