package mihon.data.extension.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import mihon.data.extension.model.AniyomiNetworkExtensionStore
import mihon.data.extension.model.ArrayIndexFamily
import mihon.data.extension.model.NetworkExtensionList
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.model.NetworkLegacyExtension
import mihon.data.extension.model.NetworkLegacyExtensionRepo
import mihon.data.extension.model.NetworkV2ExtensionStore
import mihon.data.extension.model.arrayIndexFamily
import mihon.data.extension.model.lnreader.NetworkLnReaderPlugin
import mihon.data.extension.model.lnreader.lnReaderStore
import mihon.data.extension.model.readInFittingLayout
import mihon.data.extension.model.toAvailableExtensions
import mihon.domain.extension.model.ExtensionStore
import mihon.domain.extension.model.ExtensionStoreFormat
import okio.BufferedSource
import okio.buffer
import okio.gzip
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.cancellation.CancellationException

@Inject
@SingleIn(AppScope::class)
class ExtensionStoreService(
    private val network: NetworkHelper,
    private val json: Json,
    private val protoBuf: ProtoBuf,
) {
    // Callers are often on Main, and the bodies are read and decoded while blocking
    suspend fun fetch(indexUrl: String): Result<ExtensionStore> = withContext(Dispatchers.IO) { fetchStore(indexUrl) }

    private suspend fun fetchStore(indexUrl: String): Result<ExtensionStore> {
        var updatedIndexUrl: String = indexUrl
        return try {
            val response = network.client.newCall(GET(updatedIndexUrl)).awaitSuccess()
            val store = response.body.source().decompressIfGzipped().use { source ->
                val networkStore = when (source.peek().readByte()) {
                    // "[..."
                    0x5B.toByte() -> run {
                        val entries = json.decodeFromBufferedSource<JsonArray>(source)
                        if (json.arrayIndexFamily(entries) == ArrayIndexFamily.LNREADER) {
                            return Result.success(lnReaderStore(indexUrl))
                        }
                        if (!indexUrl.endsWith("/index.min.json")) {
                            throw IllegalArgumentException("Provided legacy store url is not valid")
                        }
                        updatedIndexUrl = indexUrl.replace("/index.min.json", "/repo.json")
                        network.client.newCall(GET(updatedIndexUrl)).awaitSuccess().body.source().use {
                            json.decodeFromBufferedSource<NetworkLegacyExtensionRepo>(it)
                        }
                    }
                    // "{..."
                    0x7B.toByte() -> try {
                        json.decodeFromBufferedSource<NetworkLegacyExtensionRepo>(source.peek())
                    } catch (_: IllegalArgumentException) {
                        readV2Store(source)
                    }
                    else -> readV2Store(source)
                }

                if (networkStore is NetworkLegacyExtensionRepo && networkStore.indexV2 != null) {
                    return fetchStore(networkStore.indexV2)
                }

                networkStore.toExtensionStore(updatedIndexUrl)
            }
            Result.success(store)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to add extension store '$updatedIndexUrl'"
            }
            Result.failure(e)
        }
    }

    suspend fun getExtensions(store: ExtensionStore): Result<List<Extension.Available>> = withContext(Dispatchers.IO) {
        try {
            val extensions = if (store.extensionListUrl != null) {
                val response = network.client.newCall(GET(store.extensionListUrl!!)).awaitSuccess()
                response.body.source().decompressIfGzipped().use { source ->
                    readV2ExtensionList(source).toAvailableExtensions(store)
                }
            } else if (store.format == ExtensionStoreFormat.V2) {
                val response = network.client.newCall(GET(store.indexUrl)).awaitSuccess()
                response.body.source().decompressIfGzipped().use { source ->
                    readV2Store(source).extensionList!!.toAvailableExtensions(store)
                }
            } else if (store.format == ExtensionStoreFormat.LNREADER) {
                val response = network.client.newCall(GET(store.indexUrl)).awaitSuccess()
                response.body.source().decompressIfGzipped().use { source ->
                    val entries = json.decodeFromBufferedSource<JsonArray>(source)
                    // A store keeps the format it was added with, so an index that changed family lists nothing
                    check(json.arrayIndexFamily(entries) == ArrayIndexFamily.LNREADER) {
                        "${store.name} no longer lists LNReader plugins"
                    }
                    entries.map { json.decodeFromJsonElement<NetworkLnReaderPlugin>(it).toAvailableExtension(store) }
                }
            } else if (store.format == ExtensionStoreFormat.LEGACY) {
                val storeBaseUrl = store.indexUrl.removeSuffix("/repo.json")
                val response = network.client.newCall(GET("$storeBaseUrl/index.min.json")).awaitSuccess()
                response.body.source().use { source ->
                    json.decodeFromBufferedSource<List<NetworkLegacyExtension>>(source)
                        .toAvailableExtensions(store, storeBaseUrl)
                }
            } else {
                throw IllegalStateException("Katari can't read the index of ${store.name} (${store.format.key})")
            }
            Result.success(extensions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readV2Store(source: BufferedSource): NetworkV2ExtensionStore {
        val bytes = source.readByteArray()
        return if (bytes.isJson()) {
            readInFittingLayout(
                katari = { json.decodeFromString<NetworkExtensionStore>(bytes.decodeToString()) },
                aniyomi = { json.decodeFromString<AniyomiNetworkExtensionStore>(bytes.decodeToString()) },
                extensionList = { it.extensionList },
            )
        } else {
            readInFittingLayout(
                katari = { protoBuf.decodeFromByteArray<NetworkExtensionStore>(bytes) },
                aniyomi = { protoBuf.decodeFromByteArray<AniyomiNetworkExtensionStore>(bytes) },
                extensionList = { it.extensionList },
            )
        }
    }

    private fun readV2ExtensionList(source: BufferedSource): NetworkExtensionList {
        val bytes = source.readByteArray()
        return if (bytes.isJson()) {
            readInFittingLayout<NetworkExtensionList>(
                katari = { json.decodeFromString<NetworkExtensionStore.ExtensionList>(bytes.decodeToString()) },
                aniyomi = { json.decodeFromString<AniyomiNetworkExtensionStore.ExtensionList>(bytes.decodeToString()) },
                extensionList = { it },
            )
        } else {
            readInFittingLayout<NetworkExtensionList>(
                katari = { protoBuf.decodeFromByteArray<NetworkExtensionStore.ExtensionList>(bytes) },
                aniyomi = { protoBuf.decodeFromByteArray<AniyomiNetworkExtensionStore.ExtensionList>(bytes) },
                extensionList = { it },
            )
        }
    }

    // "{..."
    private fun ByteArray.isJson(): Boolean = firstOrNull() == 0x7B.toByte()

    private fun BufferedSource.decompressIfGzipped(): BufferedSource {
        val isGzip = peek().use { peeked ->
            try {
                peeked.readShort().toInt() == 0x1f8b
            } catch (_: Exception) {
                false
            }
        }

        return if (isGzip) gzip().buffer() else this
    }
}
