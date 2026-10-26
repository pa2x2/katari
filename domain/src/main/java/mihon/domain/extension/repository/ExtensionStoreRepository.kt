package mihon.domain.extension.repository

import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.model.ExtensionStore

interface ExtensionStoreRepository {
    /** Reads the store at [indexUrl] without adding it. */
    suspend fun fetch(indexUrl: String): Result<ExtensionStore>

    suspend fun insert(store: ExtensionStore)

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun refreshAll()

    suspend fun fetchExtensions(): List<Extension.Available>

    suspend fun getAll(): List<ExtensionStore>

    fun getAllAsFlow(): Flow<List<ExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)
}
